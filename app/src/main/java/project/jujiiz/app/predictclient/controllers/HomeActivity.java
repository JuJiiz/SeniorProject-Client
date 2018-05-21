package project.jujiiz.app.predictclient.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import project.jujiiz.app.predictclient.R;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener{
    Button btnCamera,btnVideo;
    String strServerIP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        strServerIP = getIntent().getExtras().getString("serverIP");

        init();
    }

    private void init(){
        btnCamera = findViewById(R.id.btnCamera);
        btnVideo = findViewById(R.id.btnVideo);

        btnCamera.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btnCamera){
            Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
            intent.putExtra("serverIP", strServerIP);
            getApplicationContext().startActivity(intent);
        }

        if (v == btnVideo){
            Intent intent = new Intent(getApplicationContext(), VideoActivity.class);
            intent.putExtra("serverIP", strServerIP);
            getApplicationContext().startActivity(intent);
        }
    }
}
