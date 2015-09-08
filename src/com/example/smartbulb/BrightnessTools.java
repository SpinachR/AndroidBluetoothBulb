package com.example.smartbulb;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.WindowManager;

public class BrightnessTools {

	public static final String TAG = "setBrightness";

	/** * 判断是否开启了自动亮度调节 */

	public static int isAutoBrightness(Activity activity) {

		int screenMode = 0;

		try {
			screenMode = Settings.System.getInt(activity.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE);
		} catch (SettingNotFoundException e) {
			Log.e(TAG, e.toString());
		}
		return screenMode;
	}

	/** * 获取屏幕的亮度 */

	public static int getScreenBrightness(Activity activity) {

		int nowBrightnessValue = 50;
		ContentResolver resolver = activity.getContentResolver();
		try {
			nowBrightnessValue = android.provider.Settings.System.getInt(
					resolver, Settings.System.SCREEN_BRIGHTNESS);
		} catch (Exception e) {
			Log.e(TAG, e.toString());
		}
		return nowBrightnessValue;
	}

	/** * 设置亮度 */
	public static void setBrightness(Activity activity, int brightness) {
		WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		lp.screenBrightness = brightness / 100.0F;
		activity.getWindow().setAttributes(lp);
	}

	/** * 停止自动亮度调节 */
	public static void stopAutoBrightness(Activity activity) {

		Settings.System.putInt(activity.getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	}

}
