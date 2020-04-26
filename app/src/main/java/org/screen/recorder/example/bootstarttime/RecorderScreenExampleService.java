package org.screen.recorder.example.bootstarttime;

import android.Manifest;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import org.screen.recorder.example.R;
import org.screen.recorder.example.broadcast.BroadcastReceiverBoot;
import org.screen.recorder.example.panedialog.ControlPaneDialog;
import org.screen.recorder.example.preference.PreferenceManagerEx;
import org.screen.recorder.example.recordscreen.RecordScreenActivity;
import org.screen.recorder.example.recordscreen.RecorderService;
import org.screen.recorder.example.screenshot.ScreenShotActivity;
import org.screen.recorder.example.utils.Utils;


public class RecorderScreenExampleService extends Service {

    public static boolean mDisplayFloatingIcon = true;

    private WindowManager windowManager;
    private ImageView chatHead;

    private boolean _enable = true;
    private PreferenceManagerEx mPreferences;
    public static Handler mHandlerMsg = null;
    private Messenger mRecognitionHandler = null;

    private Point szWindow = new Point();

    private Context mServiceContext;
    private ControlPaneDialog mDialogPane = null;

    private WindowManager.LayoutParams mLocationParam = null;
    Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Utils.DISPLAY_FLOATING_ICON_MSG:
                    mDisplayFloatingIcon = true;
                    chatHead.setVisibility(View.VISIBLE);
                    break;
            }

        }
    }; // UPstream handler end


    public void initMessenger() {
        if (mRecognitionHandler == null && mHandlerMsg != null)
            mRecognitionHandler = new Messenger(mHandlerMsg);
    }

    public void sendMessage(int iwhat, String data) {
        Message message = Message.obtain();
        message.what = iwhat;
        message.obj = data;

        try {
            mRecognitionHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "8181";
            String CHANNEL_NAME = "Recorder is running";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MIN);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_laucher)
                    .build();
            startForeground(2, notification);
        }
        initMessenger();

        mServiceContext = this;
        handleStart();
    }
    private void handleStart() {

        mPreferences = PreferenceManagerEx.getInstance(this);
        ScreenShotActivity.mHandlerMsg = messageHandler;
        RecorderService.mHandlerMsg = messageHandler;
        RecordScreenActivity.mHandlerMsg = messageHandler;


        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        chatHead = new ImageView(this);

        chatHead.setImageResource(R.mipmap.floatingicon_fading);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = mPreferences.getFloatingIconPosX();
        params.y = mPreferences.getFloatingIconPosY();

        try {
            windowManager.addView(chatHead, params);
            chatHead.setOnTouchListener(new View.OnTouchListener() {
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                // long time_start = 0, time_end = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int x_cord = (int) event.getRawX();
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            chatHead.setImageResource(R.mipmap.floatingicon);
                            _enable = true;
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();

                            break;

                        case MotionEvent.ACTION_UP:
                            chatHead.setImageResource(R.mipmap.floatingicon_fading);
                            resetPosition(x_cord);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            //_enable = false;
                            int xDiff = (int) (event.getRawX() - initialTouchX);
                            int yDiff = (int) (event.getRawY() - initialTouchY);

                            if (Math.abs(xDiff) > 10 || Math.abs(yDiff) > 10)
                                _enable = false;
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(chatHead, paramsF);
                            break;
                    }
                    return false;
                }

            });
        } catch (Exception e) {

        }

        chatHead.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (_enable) {
                    initiatePopupWindow(chatHead);
                    _enable = false;
                }
            }
        });
    }

    private void initiatePopupWindow(View anchor) {
        try {

            chatHead.setVisibility(View.GONE);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            final int width = (int) (displayMetrics.widthPixels * 0.85);
            Handler refresh = new Handler(Looper.getMainLooper());
            refresh.post(new Runnable() {
                public void run() {
                    if (mDialogPane == null) {
                        mDialogPane = new ControlPaneDialog(RecorderScreenExampleService.this);
                        int LAYOUT_FLAG;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                        } else {
                            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
                        }
                        mDialogPane.getWindow().setType(LAYOUT_FLAG);
                        mDialogPane.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        mDialogPane.setCancelable(true);
                        mDialogPane.setOnKeyListener(new Dialog.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface arg0, int keyCode, KeyEvent event) {
                                // TODO Auto-generated method stub
                                if (keyCode == KeyEvent.KEYCODE_BACK) {
                                    chatHead.setVisibility(View.VISIBLE);
                                    mDisplayFloatingIcon = true;
                                    mDialogPane.hidePane();
                                }
                                return false;
                            }
                        });
                        mDialogPane.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mDisplayFloatingIcon)
                                    chatHead.setVisibility(View.VISIBLE);
                            }
                        });
                        mDialogPane.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                if (mDisplayFloatingIcon)
                                    chatHead.setVisibility(View.VISIBLE);
                            }
                        });


                    } else {
                    }
                    mDialogPane.show();
                    if (mLocationParam == null) {
                        mLocationParam = new WindowManager.LayoutParams();
                        mLocationParam.copyFrom(mDialogPane.getWindow().getAttributes());
                        mLocationParam.width = width;
                        mDialogPane.getWindow().setAttributes(mLocationParam);
                    } else {
                        mDialogPane.resetDialogViewPosition();
                    }
                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);

        if (windowManager == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatHead.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (layoutParams.y + (chatHead.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (chatHead.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(chatHead, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x);
            }

        }

    }

    private void resetPosition(int x_cord_now) {
        if (x_cord_now <= szWindow.x / 2) {
            moveToLeft(x_cord_now);

        } else {
            moveToRight(x_cord_now);
        }

    }

    private void moveToLeft(final int x_cord_now) {
        WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatHead.getLayoutParams();
        mParams.x = 0;
        windowManager.updateViewLayout(chatHead, mParams);
        mPreferences.setFloatingIconPosX(mParams.x);
        mPreferences.setFloatingIconPosY(mParams.y);
    }

    private void moveToRight(final int x_cord_now) {
        WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatHead.getLayoutParams();
        mParams.x = szWindow.x - chatHead.getWidth();
        windowManager.updateViewLayout(chatHead, mParams);
        mPreferences.setFloatingIconPosX(mParams.x);
        mPreferences.setFloatingIconPosY(mParams.y);
    }

    private int getStatusBarHeight() {
        int statusBarHeight = (int) Math.ceil(25 * getApplicationContext().getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatHead != null) windowManager.removeView(chatHead);
        Intent broadcastIntent = new Intent(this, BroadcastReceiverBoot.class);
        sendBroadcast(broadcastIntent);
    }
}