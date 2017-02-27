package com.sukesan1984.stepsensorlibsample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.sukesan1984.stepsensorlib.Database;
import com.sukesan1984.stepsensorlib.model.ChunkStepCount;
import com.sukesan1984.stepsensorlib.util.DateUtils;
import com.sukesan1984.stepsensorlib.util.Logger;
import com.sukesan1984.stepsensorlib.util.SensorListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private int currentChunkSteps;
    private int stepsSinceBoot = 0;

    TextView stepView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stepView = (TextView) findViewById(R.id.steps);
        startService(new Intent(this, SensorListener.class));
        setContentView(R.layout.activity_main);
        final Database db = Database.getInstance(this);
        //findViewById(R.id.insert).setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        db.updateOrInsert(DateUtils.getCurrentDateAndHour(), stepsSinceBoot);
        //        Logger.log(String.valueOf(db.getSteps(DateUtils.getCurrentDateAndHour())));
        //    }
        //});

        //findViewById(R.id.shutdown).setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {
        //        db.resetLastUpdatedSteps(stepsSinceBoot);
        //        db.logState();
        //    }
        //});

        findViewById(R.id.show_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.logState();
            }
        });

        findViewById(R.id.get_since_boot).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.log(String.valueOf(stepsSinceBoot));
            }
        });


        findViewById(R.id.update_chunked_lists).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<ChunkStepCount> lists = db.getNotRecordedChunkedStepCounts();
                int length = lists.size();
                long[] dateAndHours = new long[length];
                int i = 0;
                for (ChunkStepCount c : lists) {
                    dateAndHours[i] = c.unixTimeMillis;
                    i++;
                }
                db.updateToRecorded(dateAndHours);
            }
        });

        findViewById(R.id.get_chunked_lists).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<ChunkStepCount> lists = db.getNotRecordedChunkedStepCounts();
                for (ChunkStepCount c : lists) {
                    Logger.log("UnixTime: " + c.unixTimeMillis + " , steps: " + c.steps);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BuildConfig.DEBUG) Logger.log("onResume");
        Database db = Database.getInstance(this);
        if (BuildConfig.DEBUG) db.logState();
        currentChunkSteps = db.getSteps(DateUtils.getCurrentDateAndHour());
        stepsSinceBoot = db.getLastUpdatedSteps();
        SharedPreferences prefs = getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        int pauseDifference = stepsSinceBoot - prefs.getInt("stepsSinceBoot", stepsSinceBoot);
        stepsSinceBoot -= pauseDifference;
        SensorManager sm =
                (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (sensor != null) {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
        }
        db.close();
        updateStep();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (BuildConfig.DEBUG) {
            Logger.log("UI - sensorChanged | currentChunkSteps: " + currentChunkSteps + " since boot: " +
                    event.values[0]);
        }
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }

        stepsSinceBoot = (int) event.values[0];
        updateStep();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Database db = Database.getInstance(this);
        db.updateOrInsert(DateUtils.getCurrentDateAndHour(), stepsSinceBoot);
    }

    private void updateStep() {
        Database db = Database.getInstance(this);
        int stepToday = Math.max(db.getTodayStep() + stepsSinceBoot - db.getLastUpdatedSteps(), 0);
        if (stepView == null) {
            stepView = (TextView) findViewById(R.id.steps);
        }
        stepView.setText(String.valueOf(stepToday));
    }
}
