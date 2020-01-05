package com.tianjj.glanimtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class SecondActivity extends AppCompatActivity {

    private ComboPreferences mPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        mPreference = new ComboPreferences(this);
        mPreference.setLocalId(1);
        mPreference.edit().putString(CameraSettings.KEY_CAMERA_ID, "KEY_CAMERA_ID_1").apply();
    }
}
