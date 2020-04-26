package org.screen.recorder.example.preference;
import android.content.Context;
import android.content.SharedPreferences;


public class PreferenceManagerEx {

	private static class Name {
		public static final String FLOATING_ICON_POS_X="x";
		public static final String FLOATING_ICON_POS_Y="y";
		public static final String MUST_RECORD_SCREEN_AUDIO="rsa";
	}

	private static PreferenceManagerEx mInstance;
	private static SharedPreferences mPrefs;

	private static final String PREFERENCES = "setting";

	public static PreferenceManagerEx getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new PreferenceManagerEx(context);
		}
		return mInstance;
	}

	private PreferenceManagerEx(Context context) {
		mPrefs = context.getSharedPreferences(PREFERENCES, 0);
	}

	public void setrecordscreenwithaudio(boolean value) {
		putBoolean(Name.MUST_RECORD_SCREEN_AUDIO, value);
	}
	public boolean getrecordscreenwithaudio(){
		return mPrefs.getBoolean(Name.MUST_RECORD_SCREEN_AUDIO, false);
	}
	public void setFloatingIconPosX(int value) {
		putString(Name.FLOATING_ICON_POS_X, Integer.toString(value));
	}
	public int getFloatingIconPosX(){
		int result = Integer.parseInt(mPrefs.getString(Name.FLOATING_ICON_POS_X, "0"));
		return result;
	}
	public void setFloatingIconPosY(int value) {
		putString(Name.FLOATING_ICON_POS_Y, Integer.toString(value));
	}
	public int getFloatingIconPosY(){
		int result = Integer.parseInt(mPrefs.getString(Name.FLOATING_ICON_POS_Y, "100"));
		return result;
	}
	private void putBoolean(String name, boolean value) {
		mPrefs.edit().putBoolean(name, value).apply();
	}

	private void putInt(String name, int value) {
		mPrefs.edit().putInt(name, value).apply();
	}

	private void putString(String name, String value) {
		mPrefs.edit().putString(name, value).apply();
	}


}
