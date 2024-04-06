package com.example.heartrate;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.components.YAxis;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

import android.widget.Button;
import android.view.View;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    private LineChart heartRateChart;
    private float lastDataPoint = Float.MIN_VALUE;
    private boolean isAboveThreshold = false;
    private final float R_PEAK_THRESHOLD = 650;
    private ArrayList<Float> recentECGData = new ArrayList<>();
    private long lastRPeakTimeMillis = System.currentTimeMillis();
    private volatile float mostRecentHeartRate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button goToHomePageButton = findViewById(R.id.goToHomePageButton);
        goToHomePageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HomePageActivity.class));
            }

        });

        Button btnHeartRateAnalysis = findViewById(R.id.btnHeartRateAnalysis);
        btnHeartRateAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HeartAnalysis.class);
            intent.putExtra("HeartRate", mostRecentHeartRate);
            startActivity(intent);
        });

        heartRateChart = findViewById(R.id.heartRateChart);
        setupChart();
        //loadHeartRateDataFromCSV(); // Load and plot the data
        // Start adding data dynamically
        //addDataDynamically();
        connectToServerAndReceiveData();
    }

    private float calculateCurrentHeartRate(long rrIntervalMillis) {
        if(rrIntervalMillis == 0){
            return 0;
        }
        return 60000f/rrIntervalMillis;
    }

    /*private boolean isRPeak(float ecgvalue, ArrayList<Float> recentECGData){
        final float r_peak_threshold = 650;

        if(recentECGData.size() < 30) return false;

        boolean isMaximum = true;
        for (float dataPoint : recentECGData){
            if (ecgvalue <=dataPoint){
                isMaximum = false;
                break;
            }
        }
        return isMaximum && ecgvalue > r_peak_threshold;
    }*/

    private boolean crossesThreshold(float ecgValue) {
        return ecgValue > R_PEAK_THRESHOLD;
    }

    private void setupChart() {
        Description description = new Description();
        description.setText("Heart Rate Data");
        heartRateChart.setDescription(description);
        heartRateChart.setTouchEnabled(true);
        heartRateChart.setDragEnabled(true);
        heartRateChart.setScaleEnabled(true);

        LineDataSet dataSet = new LineDataSet(new ArrayList<Entry>(), "Heart Rate");
        dataSet.setColor(android.graphics.Color.BLUE);
        dataSet.setValueTextColor(android.graphics.Color.BLACK);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        heartRateChart.setData(lineData);

        XAxis xAxis = heartRateChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 10f);
        xAxis.setDrawGridLines(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setLabelCount(5);
        xAxis.setGranularity(1f);

    }

    //private volatile float mostRecentHeartRate = 0;

    private void connectToServerAndReceiveData() {
        new Thread(() -> {
            try {
                // Use 10.0.2.2 to access your host machine's localhost from the Android emulator
                Socket socket = new Socket("10.219.21.230", 65432);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                ArrayList<Float> recentECGData = new ArrayList<>();
                long lastRPeakTimeMillis = System.currentTimeMillis();

                String line;

                while ((line = in.readLine()) != null) {
                    final String[] tokens = line.split(",");
                    if (tokens.length >= 2) {
                        try {
                            float timeValue = Float.parseFloat(tokens[1].trim());
                            float heartRateValue = Float.parseFloat(tokens[0].trim());
                            // Update the chart on the UI thread
                            runOnUiThread(() -> addEntryToChart(timeValue, heartRateValue));

                            //find r peaks
                            if (crossesThreshold(heartRateValue)){
                                if(!isAboveThreshold){
                                    isAboveThreshold = true;
                                }
                                else if (heartRateValue <lastDataPoint && isAboveThreshold){
                                    long currentTimeMillis = System.currentTimeMillis();
                                    long rrIntervalMillis = currentTimeMillis - lastRPeakTimeMillis;
                                    mostRecentHeartRate = calculateCurrentHeartRate(rrIntervalMillis);
                                    lastRPeakTimeMillis = currentTimeMillis;
                                    isAboveThreshold = false;
                                }
                                //float heartRate = calculateCurrentHeartRate(rrIntervalMillis);
                            }
                            else {
                                isAboveThreshold = false;
                            }

                            lastDataPoint = heartRateValue;
                            // add ecg value to recent ecg data
                            recentECGData.add(heartRateValue);
                            if (recentECGData.size() > 30) {
                                recentECGData.remove(0); // Keep recentECGData size manageable
                            }
                        } catch (NumberFormatException e) {
                            Log.e("TCP", "Parsing error", e);
                        }
                    }
                }
                socket.close();
            } catch (IOException e) {
                Log.e("TCP", "Client error", e);
            }
        }).start();
    }



    private void addEntryToChart(float time, float heartRate) {
        LineData data = heartRateChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            else{
                set.setDrawValues(false);
            }
            data.addEntry(new Entry(time, heartRate), 0);
            data.notifyDataChanged();

            heartRateChart.notifyDataSetChanged();
            //heartRateChart.moveViewToX(data.getEntryCount());
            //heartRateChart.invalidate(); // Refresh the chart
            // After adding data to the chart
            heartRateChart.setVisibleXRangeMaximum(1000); // Show only 10 entries at a time
            heartRateChart.moveViewToX(data.getXMax() - 10); // Move to the latest entry
            heartRateChart.invalidate(); // Refresh the chart

        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Heart Rate");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(android.graphics.Color.BLUE);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setFillAlpha(65);
        set.setFillColor(android.graphics.Color.BLUE);
        set.setHighLightColor(android.graphics.Color.rgb(244, 117, 117));
        set.setValueTextColor(android.graphics.Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }
}