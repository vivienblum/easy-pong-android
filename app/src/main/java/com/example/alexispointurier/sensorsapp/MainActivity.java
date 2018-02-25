package com.example.alexispointurier.sensorsapp;

import android.content.Context;
import android.content.Intent;
import android.content.SyncStatusObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import oscP5.OscEventListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    SensorManager mSensorManager;

    private static final int accelero_seuil = 20;
    private static final int OSC = 0;
    private static final int UDP = 1;

    Sensor accelerometer;
    Sensor gyroscope;
    Sensor magnetometer;
    UDPClient udpClient;
    int packetNumber = 0;
    int currentProtocole = 0;
    String lastMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        System.out.println("Start the service....");
        Intent intent = new Intent(this, ServiceTest.class);
        startService(intent);

        initNetwork();
        initComponentSensors();
    }


    @Override
    public void onBackPressed(){}


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        return false;
    }



    private void initComponentSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer =    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void initNetwork() {
        udpClient = UDPClient.getInstance();
        udpClient.run();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,magnetometer,SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        packetNumber ++;
        String message = "";

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            message = "orientation :" + Math.round(event.values[0]) + ":" + Math.round(event.values[2]);
        } else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (Math.abs(event.values[1]) + Math.abs(event.values[2]) >= accelero_seuil) {
                message = "vitesse :" + (Math.abs(event.values[1]) + Math.abs(event.values[2]));
            }
        }

        if(!message.equals("") && !message.equals(lastMessage)){
            lastMessage = message;
            System.out.println(message);
            try {
                sendMessage(message);
            } catch (SocketException e) {
                System.out.println(e);
            } catch (UnknownHostException e) {
                System.out.println(e);
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    private String currentProtocoleToString() {
        return currentProtocole==1?"UDP":"OSC";
    }

    private void sendMessage(String message) throws IOException {
        if(currentProtocole == OSC){
            udpClient.sendMessageOSC(message);
        } else if(currentProtocole == UDP){
            udpClient.sendMessageUDP(message);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
