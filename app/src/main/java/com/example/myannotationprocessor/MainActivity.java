package com.example.myannotationprocessor;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.annotationlib.DoctorInterface;
import com.example.annotationlib.Test;

@Test
@DoctorInterface
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
