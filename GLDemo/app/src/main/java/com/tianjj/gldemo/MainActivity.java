package com.tianjj.gldemo;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private AnimGlSurfaceView mGLView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGLView = (AnimGlSurfaceView)findViewById(R.id.gl_view);
        mGLView.onCreate();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLView.onDestroy();
    }
}
