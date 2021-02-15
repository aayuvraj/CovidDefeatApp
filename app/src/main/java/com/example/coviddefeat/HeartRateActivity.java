package com.example.coviddefeat;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
public class HeartRateActivity {
    static String TAG =  "Debug Heart Rate Activity";
    HelperFunctions algos = new HelperFunctions();
    public String measure_heart_rate(String videoPath, String videoName) throws IOException {
        VideoCapture videoCapture = new VideoCapture();

        if(new File(videoPath + videoName).exists()){
            Log.d(TAG, "AVI file exists!");
            videoCapture.open(videoPath + videoName);
            if(videoCapture.isOpened()){
                Log.d(TAG, "isOpened() works!");

                Mat current_frame = new Mat();
                Mat next_frame = new Mat();
                Mat diff_frame = new Mat();

                List<Double> extremes = new ArrayList<Double> ();


                int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
                Log.d(TAG, "Video Length: " + video_length);
                int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
                Log.d(TAG, "Frames per second: " + frames_per_second);

                List<Double> list = new ArrayList<Double>();

                videoCapture.read(current_frame);
                for(int k = 0; k < video_length - 1; k++){
                    videoCapture.read(next_frame);
                    Core.subtract(next_frame, current_frame, diff_frame);
                    next_frame.copyTo(current_frame);
                    list.add(Core.mean(diff_frame).val[0] + Core.mean(diff_frame).val[1] + Core.mean(diff_frame).val[2]);
                }

                List<Double> new_list = new ArrayList<Double>();
                for(int i = 0; i < (Integer)(list.size()/5) - 1; i++){
                    List<Double> sublist = list.subList(i*5, (i+1)*5);
                    double sum = 0.0;
                    for(int j = 0; j < sublist.size(); j++){
                        sum += sublist.get(j);
                    }

                    new_list.add(sum/5);
                }

                int mov_period = 50;

                List<Double> avg_data = algos.calc_mov_avg(mov_period, new_list);
                int peak_counts = algos.count_peak_points(avg_data);

                double fps_to_sec = (video_length/frames_per_second);
                double count_heart_rate = (peak_counts/2)*(60)/fps_to_sec;

                return ""+ count_heart_rate;

            }
            else{
                Log.d(TAG, ":(");
                return "";
            }
        }
        else{
            Log.d(TAG, "AVI file does not exist!");
            return "";
        }

    }
}
