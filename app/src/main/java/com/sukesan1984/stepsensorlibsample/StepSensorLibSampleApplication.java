package com.sukesan1984.stepsensorlibsample;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by kosuketakami on 2016/11/08.
 */

public class StepSensorLibSampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
