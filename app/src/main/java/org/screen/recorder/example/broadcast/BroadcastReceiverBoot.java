package org.screen.recorder.example.broadcast;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.screen.recorder.example.bootstarttime.RecorderScreenExampleService;


public class BroadcastReceiverBoot extends android.content.BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == "android.intent.action.ACTION_SHUTDOWN") {
            // Your tasks for shut down
        } else {
            RecorderScreenExampleService mSensorService = new RecorderScreenExampleService();
            Intent mServiceIntent = new Intent(context, mSensorService.getClass());
            if (!isMyServiceRunning(mSensorService.getClass(), context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(mServiceIntent);
                } else {
                    context.startService(mServiceIntent);
                }
            }
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

