package com.example.student.pad2;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    TextView textView, textView2;
    Server server;
    Client client;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);

        //server start
        try {
            server = new Server();
            server.start();
            client = new Client();
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }//Server start complete
    }//oncreate End

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button){
            server.sendMsg("패드서버에서 왔습니다."+textView.getText().toString());
        }
    }


    public void setTextView(final String msg){
        //받은 메시지 web으로 전송
        SendTask task = new SendTask("id01",msg);
        task.execute();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(msg);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    public void setTextView2(final String msg){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView2.setText(msg);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }



    //Http Async Start
    public class SendTask extends AsyncTask<Void,Void,Void>{
        String id;
        String speed;
        HttpURLConnection urlcon;
        String url = "http://70.12.50.148/a/test";

        public SendTask(String id, String speed){
            this.id = id;
            this.speed = speed;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                url += "?id="+id+"&value="+speed;
                URL serverUrl = new URL(url);
                urlcon = (HttpURLConnection) serverUrl.openConnection();
                urlcon.setRequestMethod("GET");
                urlcon.setReadTimeout(3000);
                urlcon.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }//Http Async End

    //Server Class start
    public class Server extends Thread{
        ServerSocket serversocket;
        Socket socket;
        int port = 8888;

        boolean flag = true;

        ArrayList<DataOutputStream> list; //클라이언트 접속시마다 소켓생성 > 해당 소켓의 스트림을 계속 열어두기 위해서
        String client;

        public Server() throws IOException {
            list = new ArrayList<>();
            serversocket = new ServerSocket(port);
        }

        public void run()  {
            while(flag) {
                 //클라이언트마다 소켓이 생성되어야 하기 때문에 전역변수로 이용하면 안됨
                try {
                    socket = serversocket.accept();
//                    client = socket.getInetAddress().getHostAddress();
                    new Receiver(socket, client).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMsg(String msg) {
            // BroadCast msg
            Sender sender = new Sender();
            sender.setMsg(msg);
            sender.start();
        }

        class Receiver extends Thread{
            InputStream is;
            DataInputStream dis;
            String client;
            // For Sender
            OutputStream os;
            DataOutputStream dos;

            public Receiver(Socket socket, String client) throws IOException {
                this.client = client;
                is = socket.getInputStream();
                dis = new DataInputStream(is);

                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
                list.add(dos);
            }

            @Override
            public void run() {
                while(dis != null) {
                    try {
                        String msg = dis.readUTF();
                        setTextView(msg);
                    } catch (IOException e) {
//					e.printStackTrace();
                        break;
                    }
                }// end of while
                try {
                    list.remove(dos);
                    Thread.sleep(1000);
                    if(dis != null) {
                        dis.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } //Receiver End

        class Sender extends Thread{
            String msg;

            public void setMsg(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if(list.size() == 0) {
                    return;
                }
                for(DataOutputStream out : list) {
                    if(out != null) {
                        try {
                            out.writeUTF(msg);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } //Sender End

        public void end(){
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }//Server Class End

    //Client Class Start
    public class Client extends Thread{
        String host = "70.12.50.148";
        int port = 9999;

        Socket socket;

        boolean flag = true; //서버에 소켓 생성 요청
        Sender sender;

        public void run()  {
            while(flag) {
                try {
                    socket = new Socket(host, port);
                    if(socket.isConnected()) {
                        break; // 서버와 소켓 연결되면 루프 빠짐
                    }
                } catch (Exception e) {
                    System.out.println("retry..");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            } // end of while

            //Ready to receive
            try {
                new Receiver(socket).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        class Sender extends Thread{
            OutputStream os;
            DataOutputStream dos;

            String msg;

            public Sender() {

            }
            public Sender(Socket socket) throws IOException {
                os = socket.getOutputStream();
                dos = new DataOutputStream(os);
            }

            public void setMsg(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if(dos != null) {
                    try {
                        dos.writeUTF(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        class Receiver extends Thread{
            InputStream is;
            DataInputStream dis;

            public Receiver() {
            }

            public Receiver(Socket socket) throws IOException {
                is = socket.getInputStream();
                dis = new DataInputStream(is);

            }
            @Override
            public void run() {
                while(dis != null) {
                    String msg;
                    try {
                        msg = dis.readUTF();
                        setTextView2(msg);
                        server.sendMsg(msg);
                    } catch (IOException e) {
                        break;
                    }
                }// end of while
                try {
                    Thread.sleep(1000);// 오류난 상태에서 바로 종료 날리면 오류가능성
                    if(dis != null) {
                        dis.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } //Receiver End

        public void end(){
            if(socket != null){
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    } //Client Class End




    @Override
    protected void onDestroy() {
        super.onDestroy();
        server.end();

        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
