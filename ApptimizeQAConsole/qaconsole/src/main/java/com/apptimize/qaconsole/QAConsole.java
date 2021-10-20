package com.apptimize.qaconsole;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.apptimize.Apptimize;
import com.apptimize.ApptimizeTestInfo;

public class QAConsole implements SensorEventListener {
    public boolean isShakeGestureEnabled;
    private Context appContext;

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private long mShakeTimestamp;
    private int mShakeCount;
    private SensorManager mSensorManager;
    public static boolean qaActivityLaunched;

    public QAConsole(Context aContext) {

        appContext = aContext;
        mSensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        qaActivityLaunched = false;
        isShakeGestureEnabled = true;
    }

    public void launchQAConsole() {
        if (!isShakeGestureEnabled && !qaActivityLaunched) {
            launchApptimizeQAActivity();
        }
    }

    private void launchApptimizeQAActivity() {
        Intent intent = new Intent(appContext, ApptimizeQaActivity.class);
        appContext.startActivity(intent);
        qaActivityLaunched = true;
        Apptimize.track("QA Console Opened");
    }

    private void onShake() {
        if(isShakeGestureEnabled && !qaActivityLaunched) {
            Log.i("QAConsole","Shake detected, launching console");
            launchApptimizeQAActivity();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ignore
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement.
            float gForce = (float)Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                final long now = System.currentTimeMillis();
                // ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                // reset the shake count after 3 seconds of no shakes
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0;
                }

                mShakeTimestamp = now;
                mShakeCount++;

               onShake();
        }
    }
}
