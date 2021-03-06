package com.ru426.android.xposed.parts.recents_task;

import java.util.HashMap;
import java.util.List;

import com.ru426.android.xposed.parts.recents_task.util.XUtil;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

public class Settings extends PreferenceActivity {
	private static Context mContext;
	private static SharedPreferences prefs;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_fragment_base);
	    init();
	    initOption();
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}
	
	private void init(){
		String key = mContext.getString(R.string.move_or_add_kill_all_apps_button_key);
		@SuppressWarnings("deprecation")
		CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
		if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT) {
			HashMap<String, Object> sysValue = XUtil.getSystemUIValue(mContext, "recents_inject_closeall_button", "id");
			boolean sysValueIsExists = sysValue != null && (Boolean) sysValue.get("isExists");
			boolean taskKillerAppIsInstalled = getIsInstalled(mContext, "com.sonymobile.taskkiller", ".TaskKillerViewTestActivity");
			
			if(!sysValueIsExists | !taskKillerAppIsInstalled){
				pref.setTitle(R.string.move_or_add_kill_all_apps_button_z_title);
				pref.setSummary(R.string.move_or_add_kill_all_apps_button_z_summary);
			}
		} else if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT
				&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
		}
	}
	
	@SuppressWarnings("deprecation")
	private void initOption(){
		showHomeButton();
		setPreferenceChangeListener(getPreferenceScreen());
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private static void setPreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory) preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory) preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(onPreferenceChangeListener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(onPreferenceChangeListener);				
			}
		}
	}
	
	private static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			Intent intent = new Intent();
			switch(preference.getTitleRes()){
			case R.string.move_or_add_kill_all_apps_button_title:
			case R.string.move_or_add_kill_all_apps_button_z_title:
				intent.setAction(TaskswitcherModule.STATE_CHANGE);
				intent.putExtra(TaskswitcherModule.STATE_EXTRA_IS_MOVE_OR_ADD, (Boolean) newValue);
				mContext.sendBroadcast(intent);
				break;
			default:
				return false;
			}
			return true;
		}		
	};
	
	public static boolean getIsInstalled(Context context, String packageName, String activityName){
		try{
			boolean isExists = false;
	        Intent intent = new Intent();
	        intent.setClassName(packageName, packageName + activityName);
	        intent.setAction(Intent.ACTION_MAIN);
	        intent.addCategory(Intent.CATEGORY_MONKEY);
	        PackageManager packageManager = context.getPackageManager();
	        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY);
	        if(resolveInfos.size() > 0)//when uninstalled or frozen : 0; when installed : 1; 
	            isExists = true;
	        return isExists;
		}catch(Exception e){
			e.printStackTrace();
		}
        return false;
    }
}
