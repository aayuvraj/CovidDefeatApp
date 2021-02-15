package com.example.coviddefeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int VIDEO_CAPTURE = 101;
    ProgressDialog progressDialog;
    public static String TAG = "Debug_Main_Activity";
    boolean check_if_hr_measured = false;
    boolean check_if_rr_measured = false;
    //TextView disp_txt;
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CovidDefeat/";
    String vid_name = "heart_rate.mp4";
    String mpjeg_name = "heart_rate_conv_mp.mjpeg";
    String avi_name = "final_heart_rate.avi";
    double rr_val = 0.0;
    double hr_val = 0.0;
    HelperFunctions algos = new HelperFunctions();
    HeartRateActivity act = new HeartRateActivity();
    DatabaseActivity data_act = new DatabaseActivity();

    // Checks if OpenCV Library is loaded correctly
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV done");
        } else {
            Log.d(TAG, "OpenCV error");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configure_permissions();
        progressDialog = new ProgressDialog(this);
        // TextView to represent the instructions and print heart and respiratory rate
        TextView ins = (TextView)findViewById(R.id.instructions);
        String start_display = "User Guide: \n 1. Press Heart Rate Button \n 2. Turn on Flash Light \n" + "3. Keep Fingertip on camera";
        ins.setText(start_display);

        //Button Handler to measure heart rate
        Button measure_hr = (Button)findViewById(R.id.mhr);
        measure_hr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check_if_hr_measured = true;
                start_recording_intent();
            }
        });
        //Button Handler to measure respiratory rate
        Button measure_rr = (Button)findViewById(R.id.mrr);
        measure_rr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check_if_rr_measured = true;
                showProgressDialogWithTitle("Put your phone on chest, calculating your respiratory rate .. ..");
                Intent intent = new Intent(MainActivity.this, AccelerometerService.class);
                startService(intent);
            }
        });

        // Button Handler to go to the symptoms logging page
        Button sym_btn = (Button)findViewById(R.id.gotopage2);
        sym_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent int1 = new Intent(MainActivity.this, SymptomActivity.class);
                startActivity(int1);
            }
        });

        Button upld_signs = (Button)findViewById(R.id.upload);
        upld_signs.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(true || (check_if_hr_measured && check_if_rr_measured)){
                    data_act.create_database();  // Create the database
                    data_act.create_table();   // Create the table
                    int up_check = data_act.upload_hr_resp_rate(rr_val, hr_val); // Insert the values in the database
                    if (up_check == 1) {
                        Toast.makeText(MainActivity.this, "Signs Uploaded Successfully", Toast.LENGTH_LONG).show();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this,"awesome", Toast.LENGTH_LONG).show();
                    //Toast.makeText(this,"Heart rate and Respiratory rate not measured yet",Toast.LENGTH_LONG).show();
                }
            }
        }));



    }


    public void start_recording_intent()
    {

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,5);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, VIDEO_CAPTURE);

    }

    // Function to ask the permissions required by the app
    void configure_permissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , 10);
            }
            return;
        }
    }

    //Function to show the Processing Dialog box
    private void showProgressDialogWithTitle(String substring) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(substring);
        progressDialog.show();
    }


    // Function to hide the processing dialog box
    private void hideProgressDialogWithTitle() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }

    // Receives the final respiratory rate sent by the Accelerometer service class
    private BroadcastReceiver bReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {

            // Display the respiratory rate
            String output = intent.getStringExtra("success");
            //SpannableString redSpannable= new SpannableString(output);
            //redSpannable.setSpan(new ForegroundColorSpan(Color.RED), 0, output.length(), 0);
            Log.d("Output", output);
            TextView ins = (TextView)findViewById(R.id.instructions);
            //String start_display = "User Guide: \n 1. Press Heart Rate Button \n 2. Turn on Flash Light \n" + "3. Keep Fingertip on camera";
            ins.append("\n Respiratory Rate: "+ output);
            rr_val = Double.parseDouble(output);
            hideProgressDialogWithTitle();

        }
    };
    // Function to perform action after the camera intent is finished
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK )
            {
                // The rest of the code takes the video into the input stream and writes it to the location given in the internal storage
                Log.d("uy","ok res");
                File newfile;
                //data.
                AssetFileDescriptor videoAsset = null;
                FileInputStream in_stream = null;
                OutputStream out_stream = null;
                try {

                    videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
                    Log.d("uy","vid ead");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    in_stream = videoAsset.createInputStream();
                    Log.d("uy","in stream");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("uy","dir");
                Log.d("uy",Environment.getExternalStorageDirectory().getAbsolutePath());
                File dir = new File(folder_path);
                if (!dir.exists())
                {
                    dir.mkdirs();
                    Log.d("uy","mkdir");
                }

                newfile = new File(dir, vid_name);
                Log.d("uy","hr");

                if (newfile.exists()) {
                    newfile.delete();
                }


                try {
                    out_stream = new FileOutputStream(newfile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                byte[] buf = new byte[1024];
                int len;

                while (true) {
                    try
                    {
                        Log.d("uy","try");
                        if (((len = in_stream.read(buf)) > 0))
                        {
                            Log.d("uy","File write");
                            out_stream.write(buf, 0, len);
                        }
                        else
                        {
                            Log.d("uy","else");
                            in_stream.close();
                            out_stream.close();
                            break;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                // Function to convert video to avi for processing the heart rate
                convert_video_commands();

                Toast.makeText(this, "Video has been saved to:\n" +
                        data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to record video",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    // Function to convert video to avi for processing the heart rate
    public void convert_video_commands()
    {
        //Loads the ffmpeg library
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

        // If the .mpjep files exist it deletes the older file
        File newfile = new File(folder_path + mpjeg_name);

        if (newfile.exists()) {
            newfile.delete();
        }

        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + vid_name, "-vcodec", "mjpeg", folder_path + mpjeg_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {
                    showProgressDialogWithTitle("Converting to AVI and Measuring Heart Rate");
                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {

                }

                @Override
                public void onFinish()
                {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

        // If the .avi file exist it deletes the older file
        File avi_newfile = new File(folder_path + avi_name);

        if (avi_newfile.exists()) {
            avi_newfile.delete();
        }
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + mpjeg_name, "-vcodec", "mjpeg", folder_path + avi_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {

                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {


                }

                @Override
                public void onFinish()
                {

                    while(true)
                    {

                        // Calculate the heart rate
                        VideoCapture videoCapture = new VideoCapture();

                        if(new File(folder_path + avi_name).exists()) {
                            Log.d(TAG, "AVI file exists!");
                            videoCapture.open(folder_path + avi_name);
                            if (videoCapture.isOpened()) {
                                Log.d(TAG, "isOpened() works!");

                                Mat current_frame = new Mat();
                                Mat next_frame = new Mat();
                                Mat diff_frame = new Mat();

                                List<Double> extremes = new ArrayList<Double>();


                                int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
                                Log.d(TAG, "Video Length: " + video_length);
                                int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
                                Log.d(TAG, "Frames per second: " + frames_per_second);

                                List<Double> list = new ArrayList<Double>();

                                videoCapture.read(current_frame);
                                for (int k = 0; k < video_length - 1; k++) {
                                    videoCapture.read(next_frame);
                                    Core.subtract(next_frame, current_frame, diff_frame);
                                    next_frame.copyTo(current_frame);
                                    list.add(Core.mean(diff_frame).val[0] + Core.mean(diff_frame).val[1] + Core.mean(diff_frame).val[2]);
                                }

                                List<Double> new_list = new ArrayList<Double>();
                                for (int i = 0; i < (Integer) (list.size() / 5) - 1; i++) {
                                    List<Double> sublist = list.subList(i * 5, (i + 1) * 5);
                                    double sum = 0.0;
                                    for (int j = 0; j < sublist.size(); j++) {
                                        sum += sublist.get(j);
                                    }

                                    new_list.add(sum / 5);
                                }

                                int mov_period = 50;

                                List<Double> avg_data = algos.calc_mov_avg(mov_period, new_list);
                                int peak_counts = algos.count_peak_points(avg_data);

                                double fps_to_sec = (video_length / frames_per_second);
                                double count_heart_rate = (peak_counts / 2) * (60) / fps_to_sec;
                                String heart_rate = "" + count_heart_rate;
                                if (heart_rate != "") {
                                    // Display the heart rate
                                    hr_val = Double.parseDouble(heart_rate);
                                    TextView ins = (TextView) findViewById(R.id.instructions);
                                    //String start_display = "User Guide: \n 1. Press Heart Rate Button \n 2. Turn on Flash Light \n" + "3. Keep Fingertip on camera";
                                    ins.append("\n Heart Rate: " + hr_val);
                                    hideProgressDialogWithTitle();
                                    break;
                                }
                            }
                        }
                    }


                }
            });

        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

    }
    protected void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
    }

    protected void onPause (){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }

}