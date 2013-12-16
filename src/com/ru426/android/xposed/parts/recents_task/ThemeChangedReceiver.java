package com.ru426.android.xposed.parts.recents_task;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ThemeChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(context.getString(R.string.ru_action_plugin_theme_settings_change))){
			boolean isUse = intent.getBooleanExtra(context.getString(R.string.ru_extra_plugin_theme_settings), false);
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			pref.edit().putBoolean(context.getString(R.string.ru_use_light_theme_key), isUse).commit();
		}
	}
}
