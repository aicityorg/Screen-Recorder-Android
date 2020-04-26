/*
 * Copyright (c) 2016. Vijai Chandra Prasad R.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package org.screen.recorder.example.recordscreen;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import org.screen.recorder.example.R;
import org.screen.recorder.example.preference.PreferenceManagerEx;
import org.screen.recorder.example.utils.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by vijai on 12-10-2016.
 */
//TODO: Update icons for notifcation
public class RecorderService extends Service {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static int FPS;
    private static int BITRATE;
    private static boolean mustRecAudio = false;
    private static String SAVEPATH;
    private boolean isRecording;
    private boolean isBound = false;
    private PreferenceManagerEx mPreferences;

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

    //Service connection to manage the connection state between this service and the bounded service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Get the service instance
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private long startTime, elapsedTime = 0;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        //Find the action to perform from intent
        switch (intent.getAction()) {
            case Const.SCREEN_RECORDING_START:
                initMessenger();
                if (!isRecording) {
                    //Get values from Default SharedPreferences
                    getValues();
                    Intent data = intent.getParcelableExtra(Const.RECORDER_INTENT_DATA);
                    int result = intent.getIntExtra(Const.RECORDER_INTENT_RESULT, Activity.RESULT_OK);

                    //Initialize MediaRecorder class and initialize it with preferred configuration
                    DisplayMetrics metrics = new DisplayMetrics();
                    WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
                    wm.getDefaultDisplay().getRealMetrics(metrics);

                    int mScreenDensity = metrics.densityDpi;
                    int displayWidth = metrics.widthPixels;
                    int displayHeight = metrics.heightPixels;

                    try {
                        mMediaRecorder = new MediaRecorder();
                        if (mustRecAudio){
                            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        }
                        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        if (mustRecAudio){
                            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        }
                        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

                        mMediaRecorder.setVideoSize(displayWidth, displayHeight);
                        mMediaRecorder.setVideoFrameRate(FPS);
                        mMediaRecorder.setOutputFile(SAVEPATH);
                        mMediaRecorder.setVideoEncodingBitRate(BITRATE);
                        int rotation = wm.getDefaultDisplay().getRotation();
                        int orientation = ORIENTATIONS.get(rotation + 90);
                        mMediaRecorder.setOrientationHint(orientation);
                        mMediaRecorder.prepare();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //Set Callback for MediaProjection
                    mMediaProjectionCallback = new MediaProjectionCallback();


                    //Initialize MediaProjection using data received from Intent

                    MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    mMediaProjection = mProjectionManager.getMediaProjection(result, data);
                    mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                    /* Create a new virtual display with the actual default display
                 * and pass it on to MediaRecorder to start recording */
                    try {
                        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenRecorderActivity",
                                displayWidth, displayHeight, mScreenDensity,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                                /*Handler*/);

                        mMediaRecorder.start();
                        isRecording = true;

                        Toast.makeText(this, R.string.screen_recording_started_toast, Toast.LENGTH_SHORT).show();
                    } catch (Exception e){
                        Toast.makeText(this, R.string.recording_failed_toast, Toast.LENGTH_SHORT).show();
                        isRecording = false;
                    }

                /* Add Pause action to Notification to pause screen recording if the user's android version
                 * is >= Nougat(API 24) since pause() isnt available previous to API24 else build
                 * Notification with only default stop() action */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //startTime is to calculate elapsed recording time to update notification during pause/resume
                        startTime = System.currentTimeMillis();
                        Intent recordPauseIntent = new Intent(this, RecorderService.class);
                        recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
                        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
                        showNotification(precordPauseIntent, Const.SCREEN_RECORDING_PAUSE, true, false, false);
                    } else
                        showNotification(null, Const.SCREEN_RECORDING_STOP, true, false, false);
                } else {
                    Toast.makeText(this, R.string.screenrecording_already_active_toast, Toast.LENGTH_SHORT).show();
                }
                break;
            case Const.SCREEN_RECORDING_PAUSE:
                pauseScreenRecording();
                break;
            case Const.SCREEN_RECORDING_RESUME:
                resumeScreenRecording();
                break;
            case Const.SCREEN_RECORDING_STOP:
                //Unbind the floating control service if its bound (naturally unbound if floating controls is disabled)
                if (isBound)
                    unbindService(serviceConnection);
                stopScreenSharing();

                //The service is started as foreground service and hence has to be stopped
                stopForeground(true);
                break;
        }
        return START_STICKY;
    }

    @TargetApi(24)
    private void pauseScreenRecording() {
        mMediaRecorder.pause();
        //calculate total elapsed time until pause
        elapsedTime += (System.currentTimeMillis() - startTime);

        //Set Resume action to Notification and update the current notification
        Intent recordResumeIntent = new Intent(this, RecorderService.class);
        recordResumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
        PendingIntent precordResumeIntent = PendingIntent.getService(this, 0, recordResumeIntent, 0);
        showNotification(precordResumeIntent, Const.SCREEN_RECORDING_RESUME, false, false, true);
        Toast.makeText(this, R.string.screen_recording_paused_toast, Toast.LENGTH_SHORT).show();

    }

    @TargetApi(24)
    private void resumeScreenRecording() {
        mMediaRecorder.resume();

        //Reset startTime to current time again
        startTime = System.currentTimeMillis();

        //set Pause action to Notification and update current Notification
        Intent recordPauseIntent = new Intent(this, RecorderService.class);
        recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
        showNotification(precordPauseIntent, Const.SCREEN_RECORDING_PAUSE, true, true, true);
        Toast.makeText(this, R.string.screen_recording_resumed_toast, Toast.LENGTH_SHORT).show();

    }

    private void showNotification(PendingIntent action, String strID, boolean bChromeUser, boolean hasWhen, boolean bUpdateNotification) {

      //  RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_notification);
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.custom_notification);
        if(strID.compareTo(Const.SCREEN_RECORDING_RESUME) == 0){
            remoteViews.setViewVisibility(R.id.pausevideo, GONE);
            remoteViews.setViewVisibility(R.id.startvideo, VISIBLE);
            remoteViews.setOnClickPendingIntent(R.id.startvideo, action);
        }else if (strID.compareTo(Const.SCREEN_RECORDING_PAUSE) == 0){
            remoteViews.setViewVisibility(R.id.startvideo, GONE);
            remoteViews.setViewVisibility(R.id.pausevideo, VISIBLE);
            remoteViews.setOnClickPendingIntent(R.id.pausevideo, action);
        }else {
            remoteViews.setViewVisibility(R.id.startvideo, GONE);
            remoteViews.setViewVisibility(R.id.pausevideo, GONE);
        }

        Intent recordStopIntent = new Intent(this, RecorderService.class);
        recordStopIntent.setAction(Const.SCREEN_RECORDING_STOP);
        PendingIntent precordStopIntent = PendingIntent.getService(this, 0, recordStopIntent, 0);
        remoteViews.setOnClickPendingIntent(R.id.stopvideo, precordStopIntent);

        // Open NotificationView.java Activity
        Intent UIIntent = new Intent(this, RecordScreenActivity.class);
        PendingIntent notificationContentIntent = PendingIntent.getActivity(this, 0, UIIntent, 0);

        int notificationId = Const.SCREEN_RECORDER_NOTIFICATION_ID;
        String channelId = "8383";
        String channelName = "ScreenRecorderAcitvity";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.recordervideo_noti_small)
                .setTicker(getString(R.string.screen_recording_notification_title))
                .setContent(remoteViews).
                        setUsesChronometer(bChromeUser);

        if(hasWhen){
            mBuilder.setWhen((System.currentTimeMillis() - elapsedTime));
        }
        mBuilder.setContentIntent(notificationContentIntent);
        Notification noti = mBuilder.build();
        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        if(bUpdateNotification == false){
            startForeground(notificationId, noti);
        }
        else {
            notificationManager.notify(notificationId, noti);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Get user's choices for user choosable settings
    public void getValues() {
        mPreferences = PreferenceManagerEx.getInstance(this);

        FPS = 30;
        BITRATE = 7130317;
        mustRecAudio = mPreferences.getrecordscreenwithaudio();
        String saveLocation = Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR;
        File saveDir = new File(saveLocation);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !saveDir.isDirectory()) {
            saveDir.mkdirs();
        }
        SAVEPATH = getVideoFolderSave() + File.separator + getVideoFilename();
    }


    private String getVideoFolderSave() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,"Movies");

        if(!file.exists()){
            file.mkdirs();
        }

        return file.getAbsolutePath();
    }
    private String getVideoFilename(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Random rand = new Random();
        int value = rand.nextInt(1000);
        timeStamp += Integer.toString(value);
        String fileName = "ScreenRecorder_" + timeStamp + ".mp4";

        return  fileName;
    }

    //Stop and destroy all the objects used for screen recording
    private void destroyMediaProjection() {
        try {
            mMediaRecorder.stop();
            indexFile();
            createNotificationAfterFinish(SAVEPATH);
        } catch (RuntimeException e) {
            if (new File(SAVEPATH).delete())
        //    Toast.makeText(this, getString(R.string.fatal_exception_message), Toast.LENGTH_SHORT).show();
            sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");
            stopService(new Intent(this, RecorderService.class));
        } finally {
            mMediaRecorder.reset();
            mVirtualDisplay.release();
            mMediaRecorder.release();
            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mMediaProjectionCallback);
                mMediaProjection.stop();
                mMediaProjection = null;
            }
        }
        isRecording = false;
    }

    /* Its weird that android does not index the files immediately once its created and that causes
     * trouble for user in finding the video in gallery. Let's explicitly announce the file creation
     * to android and index it */
    private void indexFile() {
        //Create a new ArrayList and add the newly created video file path to it
        ArrayList<String> toBeScanned = new ArrayList<>();
        toBeScanned.add(SAVEPATH);
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

        //Request MediaScannerConnection to scan the new file and index it
        MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                stopSelf();
            }
        });
    }
    public void createNotificationAfterFinish(String filePath){

        Uri imageUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName()+ ".provider", new File(filePath));
        } else {
            imageUri = Uri.fromFile(new File(filePath));
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(filePath));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(imageUri, "video/mp4");

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        int notificationId = 111;
        String channelId = "8585";
        String channelName = "ScreenRecorderAcitvity";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.recordervideo_noti_small)
                .setContentTitle(getResources().getString(R.string.screen_record_notify_title))
                .setContentText(getResources().getString(R.string.screen_record_notify_content));

        mBuilder.setContentIntent(pIntent);
        Notification noti = mBuilder.build();
        noti.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(notificationId, noti);

    }
    private void stopScreenSharing() {
        sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");

        if (mVirtualDisplay == null) {
            return;
        }
        destroyMediaProjection();
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            stopScreenSharing();
        }
    }
}
