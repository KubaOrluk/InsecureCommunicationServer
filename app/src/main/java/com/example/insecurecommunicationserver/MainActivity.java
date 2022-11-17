package com.example.insecurecommunicationserver;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.net.ssl.SSLServerSocket;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    ServerSocket serverSocket;
    Thread Thread1 = null;
    TextView tvIP, tvPort, tvConnectionStatus, encryptionPass;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8600;

    private static final String TLS_VERSION = "TLSv1.2";
    private static final int SERVER_COUNT = 1;
    private static final String SERVER_HOST_NAME = "127.0.0.1";
    private static final String TRUST_STORE_NAME = "servercert.p12";
    private static final char[] TRUST_STORE_PWD = new char[] {'a', 'b', 'c', '1', '2', '3'};
    private static final String KEY_STORE_NAME = "servercert.p12";
    private static final char[] KEY_STORE_PWD = new char[] {'a', 'b', 'c', '1', '2', '3'};
    String message;
    String user;
    EncryptData encryptData;

    private static Context context;

    public static Context getAppContext() {
        return MainActivity.context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplication().getApplicationContext();
        encryptData = new EncryptData();
        setContentView(R.layout.activity_main);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        encryptionPass = findViewById(R.id.encryptionPass);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        etMessage.setVisibility(View.GONE);
        btnSend.setVisibility(View.GONE);

        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Thread1 = new Thread(new Thread1());
        Thread1.start();
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    new Thread(new Thread3(message)).start();
                }
            }
        });
    }
    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }
    private PrintWriter output;
    private BufferedReader input;
    class Thread1 implements Runnable {
        @Override
        public void run() {
            TLSServer server = new TLSServer();
            System.setProperty("javax.net.debug", "ssl"); //TODO: remove

            SSLServerSocket sslServerSocket;
            Socket socket;
            try {
                sslServerSocket = server.serve(SERVER_PORT, TLS_VERSION, TRUST_STORE_NAME,
                        TRUST_STORE_PWD, KEY_STORE_NAME, KEY_STORE_PWD);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(sslServerSocket==null)
                            tvConnectionStatus.setText("Server socket is NULL");
                        else
                            tvConnectionStatus.setText("Not connected");

                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });
                Socket sslSocket = null;
                encryptData.setSecretKeyFromString(encryptionPass.getText().toString().trim());
                while (true) {
                    try {
                        sslSocket = sslServerSocket.accept();
                        output = new PrintWriter(sslSocket.getOutputStream(), true);
                        input = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String inp = input.readLine();
                    final String user = encryptData.decryptFromRec(inp);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvConnectionStatus.setText("Connected");
                            encryptionPass.setVisibility(View.GONE);
                            etMessage.setVisibility(View.VISIBLE);
                            btnSend.setVisibility(View.VISIBLE);
                        }
                    });
                    new Thread(new Thread2(user)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private class Thread2 implements Runnable {
        private String user;
        Thread2(String user) {
            this.user = user;
        }
        @Override
        public void run() {
            while (true) {
                try {
                    String inp = input.readLine();
                    final String message = encryptData.decryptFromRec(inp);
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("\n" + user + ": " + message + " ");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class Thread3 implements Runnable {
        private String message;
        Thread3(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            //output.write(message+"\n");
            output.write(encryptData.encryptToSend(message)+"\n");
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("\nServer: " + message + " ");
                            etMessage.setText("");
                }
            });
        }
    }
}