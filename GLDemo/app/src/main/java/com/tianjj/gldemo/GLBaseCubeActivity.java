package com.tianjj.gldemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.tianjj.gldemo._2d.AnimGlSurfaceView;
import com.tianjj.gldemo._3d.GlSurfaceCubeView;

public class GLBaseCubeActivity extends AppCompatActivity {

    private GlSurfaceCubeView mGLCubeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glbase_cube);

        mGLCubeView = (GlSurfaceCubeView)findViewById(R.id.gl_cube_view);
        mGLCubeView.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLCubeView.onDestroy();
    }
}
