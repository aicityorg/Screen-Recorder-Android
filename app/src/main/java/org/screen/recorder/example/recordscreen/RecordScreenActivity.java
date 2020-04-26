package org.screen.recorder.example.recordscreen;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.screen.recorder.example.R;
import org.screen.recorder.example.utils.Utils;

public class RecordScreenActivity extends Activity {

    private static final String TAG = RecordScreenActivity.class.getName();
    private MediaProjectionManager mProjectionManager;
    public static Handler mHandlerMsg;
    private Messenger messageHandler = null;

    public void initMessenger(){
        if(messageHandler == null)
            messageHandler = new Messenger(mHandlerMsg);
    }
    public void sendMessage(int iwhat, String data) {
        Message message = Message.obtain();
        message.what = iwhat;
        message.obj = data;

        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_recorder);
        initMessenger();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        Intent iin= getIntent();
        Bundle b = iin.getExtras();

        int iCheck = -1;
        if(b!=null)
        {
            iCheck = b.getInt("screen");
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            if (!isServiceRunning(RecorderService.class)) {
                //Request Screen recording permission
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
            } else if (isServiceRunning(RecorderService.class)) {
                //stop recording if the service is already active and recording
                if(iCheck == 100){
                    stopService(new Intent(this, RecorderService.class));
                    startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
                }
                else{
                    Toast.makeText(RecordScreenActivity.this, getResources().getString(R.string.screen_recording_recording_toast), Toast.LENGTH_SHORT).show();
                }
            }
        }else {
            requestContactPermission();
        }


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode){
            case Utils.WRITE_STOREAGE_REQUEST_CODE:

                // Check if the only required permission has been granted
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!isServiceRunning(RecorderService.class)) {
                        //Request Screen recording permission
                        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
                    } else if (isServiceRunning(RecorderService.class)) {
                        //stop recording if the service is already active and recording
                        Toast.makeText(RecordScreenActivity.this, getResources().getString(R.string.screen_recording_recording_toast), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");
                    this.finish();
                }

                break;
        }
    }
    private void requestContactPermission() {

        int hasContactPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(hasContactPermission != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Utils.WRITE_STOREAGE_REQUEST_CODE);
        }else {
            if (!isServiceRunning(RecorderService.class)) {
                //Request Screen recording permission
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), Const.SCREEN_RECORD_REQUEST_CODE);
            } else if (isServiceRunning(RecorderService.class)) {
                //stop recording if the service is already active and recording
                Toast.makeText(RecordScreenActivity.this, getResources().getString(R.string.screen_recording_recording_toast), Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Method to check if the service is running
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //The user has denied permission for screen mirroring. Let's notify the user
        if (resultCode == RESULT_CANCELED && requestCode == Const.SCREEN_RECORD_REQUEST_CODE) {
            sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");
            this.finish();
            return;
        }

        Intent recorderService = new Intent(this, RecorderService.class);
        recorderService.setAction(Const.SCREEN_RECORDING_START);
        recorderService.putExtra(Const.RECORDER_INTENT_DATA, data);
        recorderService.putExtra(Const.RECORDER_INTENT_RESULT, resultCode);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(recorderService);
        } else {
            startService(recorderService);
        }

        this.finish();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
