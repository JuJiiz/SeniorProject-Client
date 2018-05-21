package project.jujiiz.app.predictclient.controllers;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import project.jujiiz.app.predictclient.R;
import project.jujiiz.app.predictclient.models.ModelNetwork;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView ivExam;
    Button btnBrowse, btnExecute;
    TextView tvResult, tvStatus;

    Socket client_socket;
    DataOutputStream DOS;
    InetSocketAddress inetSocketAddress;
    String strDeviceIP = "";
    String strServerIP = "";
    Socket server_Socket;
    ServerSocket serverSocket;
    Thread thread;

    int REQUEST_TAKE_GALLERY_VIDEO = 0;
    String selectedImagePath;
    String TAG = "MYLOG";
    Bitmap imgBitmap;
    String encoded = "";
    byte[] byteArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        strServerIP = getIntent().getExtras().getString("serverIP");

        init();

        strDeviceIP = ModelNetwork.getMobileIP();
    }

    private void init() {
        ivExam = findViewById(R.id.ivExam);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnExecute = findViewById(R.id.btnExecute);
        tvResult = findViewById(R.id.tvResult);
        tvStatus = findViewById(R.id.tvStatus);

        btnBrowse.setOnClickListener(this);
        btnExecute.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        thread = new Thread(new VideoActivity.MyServerThread());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedImageUri = data.getData();

                // MEDIA GALLERY
                selectedImagePath = getPath(selectedImageUri);
                if (selectedImagePath != null) {
                    try {
                        Uri imageUri = data.getData();
                        InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        imgBitmap = BitmapFactory.decodeStream(imageStream);
                        ivExam.setImageBitmap(imgBitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        //Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    class AsynTaskSend extends AsyncTask<Void, Void, Void> {

        // can use UI thread here
        protected void onPreExecute() {
            super.onPreExecute();

            btnBrowse.setEnabled(false);
            btnExecute.setEnabled(false);
            tvStatus.setText("Sending...");
            tvResult.setText("");
        }

        @Override
        protected Void doInBackground(Void... voids) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            imgBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byteArray = byteArrayOutputStream.toByteArray();
            encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);

            try {
                inetSocketAddress = new InetSocketAddress(strServerIP, 8010);
                client_socket = new Socket();
                client_socket.connect(inetSocketAddress);
                DOS = new DataOutputStream(client_socket.getOutputStream());
                DOS.writeBytes(strDeviceIP + "," + encoded);
                DOS.close();
                client_socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "IOException: " + e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            tvStatus.setText("Receiving...");
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnBrowse) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_TAKE_GALLERY_VIDEO);
        }

        if (v == btnExecute) {
            if (ivExam.getDrawable() != null) {
                new VideoActivity.AsynTaskSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                Toast.makeText(getApplicationContext(), "No Image", Toast.LENGTH_SHORT).show();
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
                            if (strMessege.equals("1")) {
                                tvResult.setText("On lane (Middle)");
                            } else if (strMessege.equals("2")) {
                                tvResult.setText("On lane (Left)");
                            } else if (strMessege.equals("3")) {
                                tvResult.setText("On lane (Right)");
                            } else if (strMessege.equals("4")) {
                                tvResult.setText("Head out");
                            } else {
                                tvResult.setText("Unknow");
                            }

                            btnBrowse.setEnabled(true);
                            btnExecute.setEnabled(true);
                            tvStatus.setText("Idle");
                        }
                    });

                }
            } catch (IOException ex) {
                Log.d("MYLOG", "IOException Thread: " + ex);
            }
        }
    }
}
