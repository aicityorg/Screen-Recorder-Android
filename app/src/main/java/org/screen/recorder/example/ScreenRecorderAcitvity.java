package org.screen.recorder.example;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;


import org.screen.recorder.example.bootstarttime.RecorderScreenExampleService;

public class ScreenRecorderAcitvity extends Activity {
    private Context mContext;

    private void setSystembarColor_Normal(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.getDecorView().setSystemUiVisibility(/*View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |*/ View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            window.setStatusBarColor(getResources().getColor(R.color.main_top_page_color));
            window.setNavigationBarColor(getResources().getColor(R.color.page_background));

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        super.onCreate(savedInstanceState);
        setSystembarColor_Normal();
        if (getActionBar() != null){
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);
        this.mContext = this;
        checkOverlay();
    }

    private void checkOverlay(){
        if(Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1234);
            }
            else {
                startScreenRecorderService();
            }
        }
        else
        {
            startScreenRecorderService();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1234) {
            if(Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(this)) {
                }
                else {
                    startScreenRecorderService();
                }
            }
            else
            {
                startScreenRecorderService();
            }
        }
    }

    private void startScreenRecorderService(){
        RecorderScreenExampleService mSensorService = new RecorderScreenExampleService();
        Intent mServiceIntent = new Intent(mContext, mSensorService.getClass());
        if (!isMyServiceRunning(mSensorService.getClass())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(mServiceIntent);
            } else {
                mContext.startService(mServiceIntent);
            }
        }
        finish();
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }


}
