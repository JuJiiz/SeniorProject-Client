package project.jujiiz.app.predictclient.controllers;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.text.format.Formatter;

import project.jujiiz.app.predictclient.R;
import project.jujiiz.app.predictclient.models.ModelNetwork;

public class ConnectActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "MYLOG";
    EditText etIP, etPort;
    Button btnConnect;
    TextView tvIP;
    Socket client_socket = new Socket();
    DataOutputStream DOS;
    InetSocketAddress inetSocketAddress;
    String strDeviceIP = "";
    Socket server_Socket;
    ServerSocket serverSocket;

    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        }

        init();

        //etIP.setText("10.199.2.11");
    }

    @Override
    protected void onResume() {
        super.onResume();

        strDeviceIP = ModelNetwork.getDeviceIP(getApplicationContext());
        tvIP.setText(strDeviceIP);

        thread = new Thread(new MyServerThread());
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void init() {
        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        tvIP = findViewById(R.id.tvIP);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
    }

    class AsynTaskSend extends AsyncTask<Void, Void, Void> {
        private final ProgressDialog dialog = new ProgressDialog(ConnectActivity.this);
        boolean threadBool = false;

        // can use UI thread here
        protected void onPreExecute() {
            super.onPreExecute();
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }
            this.dialog.setMessage("กรุณารอสักครู่...");
            this.dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                inetSocketAddress = new InetSocketAddress(etIP.getText().toString(), 8010);
                client_socket = new Socket();
                client_socket.connect(inetSocketAddress);
                DOS = new DataOutputStream(client_socket.getOutputStream());
                DOS.writeBytes(strDeviceIP + ",connectRequest");
                DOS.close();
                client_socket.close();
                threadBool = true;

            } catch (IOException e) {
                e.printStackTrace();
                Log.d("MYLOG", "IOException: " + e);
                threadBool = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }

            if (threadBool == true) {
                Toast.makeText(getApplicationContext(), "Successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Fail", Toast.LENGTH_SHORT).show();
            }
        }
    }

    class MyServerThread implements Runnable {
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        String strMessege = "";
        Handler handler = new Handler();

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(8010));
                while (true) {
                    server_Socket = serverSocket.accept();
                    inputStreamReader = new InputStreamReader(server_Socket.getInputStream());
                    bufferedReader = new BufferedReader(inputStreamReader);
                    strMessege = bufferedReader.readLine();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("MYLOG", "strMessege: " + strMessege);
                            if (strMessege.equals("OK")) {
                                Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                                intent.putExtra("serverIP", etIP.getText().toString());
                                getApplicationContext().startActivity(intent);
                            }
                        }
                    });
                }
            } catch (IOException ex) {

            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnConnect) {
            new AsynTaskSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
