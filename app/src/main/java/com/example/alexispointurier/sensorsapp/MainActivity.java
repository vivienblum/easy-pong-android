package com.example.alexispointurier.sensorsapp;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    SensorManager mSensorManager;


    private static final int accelero_seuil = 35;
    private static final int OSC = 0;
    private static final int UDP = 1;

    float[] mGravity = new float[11];
    float[] mGeomagnetic = new float[11];

    Sensor gyroscopeDefault;
    Sensor mAccelerometer;
    Sensor orientation;
    UDPClient udpClient;
    int packetNumber = 0;

    private int currentProtocole;

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
    public void onBackPressed(){
        // Toast.MakeText(getApplicationContext(),"You Are Not Allowed to Exit the App", Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        return false;
    }



    private void initComponentSensors() {


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Pour trouver un capteur spécifique
        gyroscopeDefault = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelerometer =    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    private void initNetwork() {
        udpClient = UDPClient.getInstance();
        udpClient.run();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, gyroscopeDefault, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,orientation,SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        packetNumber ++;
        String message = "";
        String gyro = "";

        if ( event.sensor.getType() == Sensor.TYPE_GYROSCOPE ){
            message = "gyro :" + event.values[2];
        } else if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if(Math.abs(event.values[1]) + Math.abs(event.values[2]) >= accelero_seuil) {
                message = "accelero :" + (Math.abs(event.values[1]) + Math.abs(event.values[2]));
                mGravity = event.values;
            }
        }

        //traitement special sur le magnetometre, selon les recommandation google
        if (mAccelerometer != null && gyroscopeDefault != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
            }
        }
        sendMessage(message);
        //sendMessage(messageCompas);
    }

    private String currentProtocoleToString() {
        return currentProtocole==1?"UDP":"OSC";
    }

    private void sendMessage(String message) {
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
