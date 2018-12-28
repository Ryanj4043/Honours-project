package com.example.ryan.honours_project;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.example.ryan.honours_project.LocationHandler;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Hello world");
        LocationHandler loc = new LocationHandler(this);
    }

}
