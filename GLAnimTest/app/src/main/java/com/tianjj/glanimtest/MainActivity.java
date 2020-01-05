package com.tianjj.glanimtest;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    TextView tv;
    GestureDetector mDetector;
    private ComboPreferences mPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        mDetector = new GestureDetector(this, new TestDetectorListener());
        mDetector.setIsLongpressEnabled(true);

        mPreference = new ComboPreferences(this);
        mPreference.setLocalId(0);
        mPreference.setLocalId(1);
        mPreference.registerOnSharedPreferenceChangeListener(this);
    }

    private void springAnim() {
        SpringConfig springConfig = SpringConfig.fromBouncinessAndSpeed(10, 1);
        Spring spring = SpringSystem.create()
                .createSpring()
                .setSpringConfig(springConfig)
                .addListener(new SimpleSpringListener() {
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        float cValue = (float)spring.getCurrentValue();
                        //Log.d(TAG, "onSpringUpdate: cValue= " + cValue);
                        tv.setX(cValue);
                        tv.setY(cValue);
                        super.onSpringUpdate(spring);
                    }
                });

        spring.setCurrentValue(100).setEndValue(400);


    }

    private void animTest() {
        TypeEvaluator evaluator = new TypeEvaluator<PointF>() {
            @Override
            public PointF evaluate(float fraction, PointF start, PointF end) {
                float x = start.x;
                float y = start.y + (fraction * (end.y - start.y));
                return new PointF(x, y);
            }
        };

        ValueAnimator animator = ValueAnimator.ofObject(evaluator, new PointF(500, 800), new PointF(800, 1500));

        animator.setInterpolator(new PathInterpolator(0.14f, 1.0f, 0.9f, 0f));
        animator.setDuration(5000);
        animator.setRepeatMode(ValueAnimator.RESTART);
        //animator.setRepeatCount(5);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                PointF pointF = (PointF) valueAnimator.getAnimatedValue();
                Log.d("TJJ", "onAnimationUpdate: pointF= (" + pointF.x + ", " + pointF.y + ") fraction= " + valueAnimator.getAnimatedFraction());
                tv.setX(pointF.x);
                tv.setY(pointF.y);
                tv.setText(String.format(Locale.CHINA, "%.1f", valueAnimator.getAnimatedFraction()));
            }
        });
        animator.start();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("TJJ_SHA", "onSharedPreferenceChanged >>> " +
                "key= " + key + ", value= " + sharedPreferences.getString(key, "Default_value"));
    }

    private class TestDetectorListener extends GestureDetector.SimpleOnGestureListener {
        TestDetectorListener() {
            super();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.d(TAG, "onSingleTapUp: e: " + e);
//            animTest();
            //springAnim();
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: e: " + e);
            super.onLongPress(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG, "onScroll: e1: " + e1 + ", e2: " + e2 + ", distanceX= " + distanceX + ", distanceY= " + distanceY);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling: e1: " + e1 + "，e2: " + e2 + ", velocityX= " + velocityX + ", velocityY= " + velocityY);
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.d(TAG, "onShowPress: e: " + e);
            super.onShowPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: e: " + e);
            return super.onDown(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG, "onDoubleTap: e: " + e);
            pushPreference(CameraSettings.KEY_CAMERA_ID, "KEY_CAMERA_ID_0");

            startActivity(new Intent(MainActivity.this, SecondActivity.class));
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.d(TAG, "onDoubleTapEvent: e: " + e);
            return super.onDoubleTapEvent(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "onSingleTapConfirmed: e: " + e);
            return super.onSingleTapConfirmed(e);
        }
    }

    SharedPreferences.Editor mEditor;
    private void pushPreference(String key, String value) {
        if (null == mEditor) {
            mEditor = mPreference.edit();
        }

        mEditor.clear();
        mEditor.putString(key, value).apply();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mDetector.onTouchEvent(event);
    }
}
