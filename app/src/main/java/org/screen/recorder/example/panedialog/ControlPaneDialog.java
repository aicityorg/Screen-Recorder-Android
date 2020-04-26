package org.screen.recorder.example.panedialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;


import android.os.Bundle;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import android.widget.ToggleButton;

import org.screen.recorder.example.R;
import org.screen.recorder.example.bootstarttime.RecorderScreenExampleService;
import org.screen.recorder.example.preference.PreferenceManagerEx;
import org.screen.recorder.example.recordscreen.RecordScreenActivity;
import org.screen.recorder.example.screenshot.ScreenShotActivity;

public class ControlPaneDialog extends Dialog {
    private View mDialogView;
    private AnimationSet mModalInAnim;
    private AnimationSet mModalOutAnim;

    private ImageButton mBtnRecScreen;
    private ImageButton mBtnScreenShot;
    private ImageButton mBtnSetting;
    private ImageButton mMenuBack;

    private LinearLayout mLayoutQuickHome;
    private RelativeLayout mLayoutSettings;

    private ToggleButton    mRecordAudioToggle;
    public static final int NORMAL_TYPE = 0;
    public static Context mContext;
    private PreferenceManagerEx mPreferences;

    public ControlPaneDialog(Context context) {
        this(context, NORMAL_TYPE);
        mContext = context;
    }

    public ControlPaneDialog(Context context, int alertType) {
        super(context, R.style.alert_dialog);
        mContext = context;
        loadAnimation();
    }
    public void resetDialogViewPosition(){
        mDialogView.setVisibility(View.VISIBLE);
        onStart();
    }
    public void loadAnimation(){
        mModalInAnim = (AnimationSet) OptAnimationLoader.loadAnimation(getContext(), R.anim.modal_in);
        mModalOutAnim = (AnimationSet) OptAnimationLoader.loadAnimation(getContext(), R.anim.modal_out);
        mModalOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mDialogView.setVisibility(View.GONE);
                mDialogView.post(new Runnable() {
                    @Override
                    public void run() {
                        hidePane();
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void hidePane(){
        ControlPaneDialog.super.cancel();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pane_dialog);
        mPreferences = PreferenceManagerEx.getInstance(mContext);

        mDialogView = getWindow().getDecorView().findViewById(android.R.id.content);
        mLayoutQuickHome =  findViewById(R.id.quickassistant);
        mLayoutSettings = findViewById(R.id.layoutsettings);

        setupSettingPane();

        mBtnRecScreen = findViewById(R.id.btnRecScreen);
        mBtnRecScreen.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startScreenRecorder();
            }
        });


        mBtnScreenShot = findViewById(R.id.btnScreenShot);
        mBtnScreenShot.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startScreenShot();
            }
        });


        mBtnSetting =  findViewById(R.id.btnSetting);
        mBtnSetting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showSettingPane();
            }
        });

        mMenuBack = findViewById(R.id.btnMenuBack);
        mMenuBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showMenuBack();
            }
        });


        showHomePane();
    }

    private void startScreenRecorder(){
        RecorderScreenExampleService.mDisplayFloatingIcon = false;
        hidePane();
        Intent myIntent = new Intent(mContext, RecordScreenActivity.class);
        myIntent.putExtra("screen", 100);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(myIntent);
    }
    private void startScreenShot(){
        RecorderScreenExampleService.mDisplayFloatingIcon = false;
        hidePane();
        Intent myIntent = new Intent(mContext, ScreenShotActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(myIntent);
    }

    private void setupSettingPane(){
        mRecordAudioToggle =  findViewById(R.id.cbScreenRecordAudio);
        mRecordAudioToggle.setChecked(mPreferences.getrecordscreenwithaudio());
        mRecordAudioToggle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                mPreferences.setrecordscreenwithaudio(mRecordAudioToggle.isChecked());
            }
        });
    }

    private void showMenuBack(){
        showHomePane();
    }

    public void showHomePane(){
        mLayoutQuickHome.setVisibility(View.VISIBLE);
        mLayoutSettings.setVisibility(View.GONE);
        mMenuBack.setVisibility(View.GONE);

    }

    private void showSettingPane(){
        int heightdialog = mLayoutQuickHome.getHeight();
        mLayoutSettings.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightdialog));
        mLayoutQuickHome.setVisibility(View.GONE);
        mLayoutSettings.setVisibility(View.VISIBLE);
        mMenuBack.setVisibility(View.VISIBLE);

    }
    protected void onStart() {
        mDialogView.startAnimation(mModalInAnim);
    }
    @Override
    public void cancel() {
        dismissWithAnimation();
    }

    private void dismissWithAnimation() {
        mDialogView.startAnimation(mModalOutAnim);
    }

}