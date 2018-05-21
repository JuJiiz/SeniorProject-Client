package project.jujiiz.app.predictclient.controllers;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

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

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    String TAG = "MYLOG";
    Mat mRgba;
    Bitmap bitmap;
    String encoded = "";
    byte[] byteArray;

    ImageButton btnCapture;
    TextView tvResult, tvStatus;

    Socket client_socket;
    DataOutputStream DOS;
    InetSocketAddress inetSocketAddress;
    String strDeviceIP = "";
    String strServerIP = "";
    Socket server_Socket;
    ServerSocket serverSocket;

    Thread thread;
    boolean processready = false, sendready;

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

        //strDeviceIP = ModelNetwork.getWifiIP(getApplicationContext());
        strDeviceIP = ModelNetwork.getMobileIP();
    }

    private void init() {
        cameraBridgeViewBase = findViewById(R.id.jvcPreview);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        tvResult = findViewById(R.id.tvResult);
        tvStatus = findViewById(R.id.tvStatus);
        btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(this);
        btnCapture.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_BUTTON_PRESS) {
                    btnCapture.setImageResource(R.drawable.btnpush);
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_BUTTON_RELEASE) {
                    btnCapture.setImageResource(R.drawable.btnrelease);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCv not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCv load success");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        sendready = false;

        thread = new Thread(new CameraActivity.MyServerThread());
        thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        sendready = false;

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
        return mRgba;
    }

    class AsynTaskSend extends AsyncTask<Void, Void, Void> {

        // can use UI thread here
        protected void onPreExecute() {
            super.onPreExecute();
            //btnCapture.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (sendready == true) {
                if (processready == true) {
                    processready = false;
                    bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mRgba, bitmap);
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
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
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    @Override
    public void onClick(View v) {

        if (v == btnCapture) {
            if (sendready == false) {
                sendready = true;
                processready = true;
                tvStatus.setText(R.string.status_text_working);
                tvStatus.setTextColor(getResources().getColor(R.color.colorCameraStatusWork));
                new CameraActivity.AsynTaskSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                sendready = false;
                tvStatus.setText(R.string.status_text_standby);
                tvStatus.setTextColor(getResources().getColor(R.color.colorCameraStatusStandby));
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
                            //Log.d(TAG, "strMessege camera: " + strMessege);

                            processready = true;
                            //btnCapture.setEnabled(true);
                        }
                    });

                }
            } catch (IOException ex) {
                Log.d("MYLOG", "IOException Thread: " + ex);
            }
        }
    }
}
