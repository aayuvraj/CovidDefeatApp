package com.example.coviddefeat;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.FocusFinder;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AccelerometerService extends Service implements SensorEventListener {
    private SensorManager accelerometermanage;
    private Sensor sense_accelerometer;
    double accelerometervalueX[]= new double[1280];
    double accelerometervalueY[]= new double[1280];
    double accelerometervalueZ[]= new double[1280];
    int index = 0;
    int k=0;
    Bundle b;
    HelperFunctions algos = new HelperFunctions();
    public static String TAG = "Debug_Acc_Service";


    String data_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Covidefeat/";
    String file_name = "breathe.csv";
    //ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

    public AccelerometerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    // Start the service
    @Override
    public void onCreate()
    {
       // Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        accelerometermanage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sense_accelerometer = accelerometermanage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometermanage.registerListener(this, sense_accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/raw/notification");
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
        r.play();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Toast.makeText(this, "registered listener", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    // Log the axis values from the accelerometer
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor my_sensor = sensorEvent.sensor;
        if(my_sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            index++;
            accelerometervalueX[index] = sensorEvent.values[0];
            accelerometervalueY[index] = sensorEvent.values[1];
            accelerometervalueZ[index] = sensorEvent.values[2];
            //Toast.makeText(this,"X-axis"+Double.toString(sensorEvent.values[0]),Toast.LENGTH_SHORT).show();
            //Toast.makeText(this,"Y-axis"+Double.toString(sensorEvent.values[1]),Toast.LENGTH_SHORT).show();
            //Toast.makeText(this,"Z-axis"+Double.toString(sensorEvent.values[2]),Toast.LENGTH_SHORT).show();

            if (index>=1279){
                index = 0;
                Toast.makeText(this, "Started to File", Toast.LENGTH_LONG).show();
                accelerometermanage.unregisterListener(this);
                Uri alarmSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getApplicationContext().getPackageName() + "/raw/notification");
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alarmSound);
                r.play();
                List<Double> values_x = new ArrayList<Double>();
                List<Double> values_y = new ArrayList<Double>();
                List<Double> values_z = new ArrayList<Double>();
                //int it =0;
                for(int it=0;it<accelerometervalueX.length;it++){
                    Double val = accelerometervalueX[it];
                    values_x.add(val);
                }
                for(int it=0;it<accelerometervalueY.length;it++){
                    Double val = accelerometervalueY[it];
                    values_y.add(val);
                }
                for(int it=0;it<accelerometervalueZ.length;it++){
                    Double val = accelerometervalueZ[it];
                    values_z.add(val);
                }
                int mov_period = 50;

                // Calculating the moving average and peak detection
                //List<Double> values_x = hashMap.get("x");
                List<Double> avg_data_x = algos.calc_mov_avg(mov_period, values_x);
                int peak_counts_X = algos.count_peak_points_thresh(avg_data_x);

                //List<Double> values_y = hashMap.get("y");
                List<Double> avg_data_y = algos.calc_mov_avg(mov_period, values_y);
                int peak_counts_Y = algos.count_peak_points_thresh(avg_data_y);

                //List<Double> values_z = hashMap.get("z");
                List<Double> avg_data_z = algos.calc_mov_avg(mov_period, values_z);
                int peak_counts_Z = algos.count_peak_points_thresh(avg_data_z);


                //String s = " " + peak_counts_X/2 + " " + peak_counts_Y/2 + " " + peak_counts_Z/2;
                String resp_rate_val = ""+peak_counts_Y/2;
                // Sending back the received value to the main activity
                sendBroadcast(resp_rate_val);
                //Log.d(TAG, resp_rate_val);
                Toast.makeText(this,"Respiratory Rate:" + resp_rate_val,Toast.LENGTH_LONG).show();
            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // Function to send the broadcast message to the main activity
    private void sendBroadcast (String success){
        Intent intent = new Intent ("message"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("success", success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
