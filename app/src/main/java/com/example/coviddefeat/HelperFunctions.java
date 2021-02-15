package com.example.coviddefeat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HelperFunctions {
    public List<Double> calc_mov_avg(int period, List<Double> data)
    {
        SimpleMovingAverage sm = new SimpleMovingAverage(period);
        List<Double> out_avg_data = sm.getMA(data);

        return out_avg_data;

    }
    //Function to count normal peak points from the data points
    public int count_peak_points(List<Double> data_points){
        int peak_cnt = 0;
        List<Double> extremes = new ArrayList<Double>();
        double previous = data_points.get(0);
        double previousSlope = 0;
        double p;
        //int peak_count = 0;
        for (int i = 1; i < data_points.size(); i++) {
            p = data_points.get(i);
            double slope = p - previous;
            if (slope * previousSlope < 0) {
                extremes.add(previous);
                peak_cnt += 1;
            }
            previousSlope = slope;
            previous = p;
        }
        //System.out.println(peak_count);
        return peak_cnt;
    }

    //Function to count normal peak points greater than the calculated threshold
    public int count_peak_points_thresh(List<Double> data_points){
        int peak_cnt = 0;
        List<Double> extremes = new ArrayList<Double>();
        double previous = data_points.get(0);
        double previousSlope = 0;
        double p;
        //int peak_count = 0;
        for (int i = 1; i < data_points.size(); i++) {
            p = data_points.get(i);
            double slope = p - previous;
            if (slope * previousSlope < 0) {
                extremes.add(previous);
                peak_cnt += 1;
            }
            previousSlope = slope;
            previous = p;
        }
        List<Double> widths = new ArrayList<Double>();
        for (int i=1; i < extremes.size(); i++ )
        {

            widths.add(Math.abs(extremes.get(i) - extremes.get(i-1)));
        }
        double sum_width = 0.0;
        for (int i=0; i<widths.size(); i++)
        {
            sum_width += widths.get(i);
        }
        double avg_wid = sum_width/widths.size();
        System.out.println("Avg width: " + avg_wid);

        int new_peak_cnt = 0;
        for (int i=1; i < extremes.size(); i++ )
        {
            if ( Math.abs(extremes.get(i) - extremes.get(i-1)) >= avg_wid  )
            {
                new_peak_cnt += 1;
            }

        }
        return new_peak_cnt;
    }

}

class SimpleMovingAverage {

    private final Queue<Double> window = new LinkedList<>();
    private final int period;
    private double sum;

    public SimpleMovingAverage(int period) {
        //if (BuildConfig.DEBUG && period <= 0) {
        //     throw new AssertionError("Period must be a positive integer");
        //}
        this.period = period;
    }

    public void newNum(double num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public double getAvg() {
        if (window.isEmpty()) //Case when Average is undefined
            return 0;
        return sum / window.size();
    }


    public List<Double> getMA(List<Double> data){
        List<Double> ma_data = new ArrayList<Double>(data.size());
        for (double x : data) {
            newNum(x);
            ma_data.add(getAvg());
        }
        return ma_data;
    }
}