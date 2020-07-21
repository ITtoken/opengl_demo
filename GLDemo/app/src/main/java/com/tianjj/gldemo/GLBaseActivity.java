package com.tianjj.gldemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.tianjj.gldemo._2d.AnimGlSurfaceView;

public class GLBaseActivity extends AppCompatActivity {

    private AnimGlSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glbase);


        mGLView = (AnimGlSurfaceView)findViewById(R.id.gl_view);
        mGLView.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLView.onDestroy();
    }
}
