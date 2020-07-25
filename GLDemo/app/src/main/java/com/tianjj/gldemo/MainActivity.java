package com.tianjj.gldemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.tianjj.gldemo._2d.AnimGlSurfaceView;
import com.tianjj.gldemo._3d.GlSurfaceCubeView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mGlBaseBtn;
    private View mGlBaseCubeBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGlBaseBtn = findViewById(R.id.gl_base);
        mGlBaseBtn.setOnClickListener(this);

        mGlBaseCubeBtn = findViewById(R.id.gl_base_cube);
        mGlBaseCubeBtn.setOnClickListener(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.gl_base:
                startActivity(new Intent(this, GLBaseActivity.class));
                break;
            case R.id.gl_base_cube:
                startActivity(new Intent(this, GLBaseCubeActivity.class));
                break;
        }
    }
}
