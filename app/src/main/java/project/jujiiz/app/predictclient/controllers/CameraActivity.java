package project.jujiiz.app.predictclient.controllers;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import project.jujiiz.app.predictclient.R;
import project.jujiiz.app.predictclient.models.ModelNetwork;

import static org.opencv.android.Utils.matToBitmap;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    String TAG = "MYLOG";
    Mat mRgba;
    Bitmap bitmap;
    String encoded = "";
    byte[] byteArray;

    Button btnStart,btnCapture;
    TextView tvResult;

    Socket client_socket;
    DataOutputStream DOS;
    InetSocketAddress inetSocketAddress;
    String strDeviceIP = "";
    String strServerIP = "";
    Socket server_Socket;
    ServerSocket serverSocket;

    Thread thread;

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        strServerIP = getIntent().getExtras().getString("serverIP");

        init();

        strDeviceIP = ModelNetwork.getDeviceIP(getApplicationContext());
    }

    private void init() {
        cameraBridgeViewBase = findViewById(R.id.jvcPreview);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        tvResult = findViewById(R.id.tvResult);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);
        btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        thread = new Thread(new CameraActivity.MyServerThread());
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
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byteArray = byteArrayOutputStream.toByteArray();
        encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        return mRgba;
    }

    class AsynTaskSend extends AsyncTask<Void, Void, Void> {

        // can use UI thread here
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnStart) {
            btnStart.setVisibility(View.GONE);
            btnCapture.setVisibility(View.VISIBLE);
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "OpenCv not loaded");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
            } else {
                Log.d(TAG, "OpenCv load success");
                baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }

        if (v == btnCapture){
            new CameraActivity.AsynTaskSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                            tvResult.setText(strMessege);
                            Log.d("MYLOG", "strMessege camera: " + strMessege);
                        }
                    });

                }
            } catch (IOException ex) {
                Log.d("MYLOG", "IOException Thread: " + ex);
            }
        }
    }
}
