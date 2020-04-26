package org.screen.recorder.example.screenshot;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.OrientationEventListener;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.screen.recorder.example.R;
import org.screen.recorder.example.utils.Utils;

public class ScreenShotActivity extends Activity {

    private static final String TAG = ScreenShotActivity.class.getName();
    private static final int REQUEST_CODE = 100;
    private static final String SCREENCAP_NAME = "sh_screenshot";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private int mCount = 0;
    private String mFileImageName;
    private OrientationChangeCallback mOrientationChangeCallback;

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

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(mCount > 1)
                return;

            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                image = mImageReader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // create bitmap
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // write bitmap to a file
                    fos = new FileOutputStream(mFileImageName);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    galleryAddPic(new File(mFileImageName));
                    if(mCount == 0)
                        createNotification(mFileImageName);
                    mCount++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null) {
                    bitmap.recycle();
                }

                if (image != null) {
                    image.close();
                }
            }
        }
    }
    public void galleryAddPic(File currentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(currentPhotoPath);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_shot);

        initMessenger();
        // call for the projection manager
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mCount = 0;
        // start capture handling thread
        mHandler = new Handler();
        mFileImageName = getImageFolderSave() + File.separator + getImageFilename();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            startProjection();
     //       stopProjection();
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
                    startProjection();
             //       stopProjection();
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
            startProjection();
 //           stopProjection();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");
                this.finish();
                return;
            }
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            if (sMediaProjection != null) {

                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), null);
            }
            stopProjection();
            sendMessage(Utils.DISPLAY_FLOATING_ICON_MSG, "");
            this.finish();
        }
    }
    public void createNotification(String filePath){

        Uri imageUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imageUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName()+ ".provider", new File(filePath));
        } else {
            imageUri = Uri.fromFile(new File(filePath));
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
     //   File file = new File(filePath); // set your audio path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(imageUri, "image/*");

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

         NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

         int notificationId = 10;
         String channelId = "8282";
         String channelName = "Screenshot";
         int importance = NotificationManager.IMPORTANCE_HIGH;

         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
             NotificationChannel mChannel = new NotificationChannel(
                     channelId, channelName, importance);
             notificationManager.createNotificationChannel(mChannel);
         }

         NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                 .setSmallIcon(R.mipmap.shot_noti)
                 .setContentTitle(getResources().getString(R.string.screen_shot_notify_title))
                 .setContentText(getResources().getString(R.string.screen_shot_notify_content))
                 .setLargeIcon(
                         Bitmap.createScaledBitmap(bitmap, 128, 128, false))
               //  .setUsesChronometer(true)
              //   .setOngoing(true)
                 ;

/*         TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
         stackBuilder.addNextIntent(intent);
         PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                 0,
                 PendingIntent.FLAG_UPDATE_CURRENT
         );*/
        mBuilder.setContentIntent(pIntent);
        Notification noti = mBuilder.build();
        noti.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(notificationId, noti);

    }
    private String getImageFolderSave() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,"DCIM");

        if(!file.exists()){
            file.mkdirs();
        }
        File file_2 = new File(file.getPath(),"Screenshots");

        if(!file_2.exists()){
            file_2.mkdirs();
        }
        return file_2.getAbsolutePath();
    }
    private String getImageFilename(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Random rand = new Random();
        int value = rand.nextInt(1000);
        timeStamp += Integer.toString(value);
        String fileName = "Screenshot_" + timeStamp + ".png";

        return  fileName;
    }
    private void startProjection() {
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    private void stopProjection() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
    }

    private void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

    }
}
