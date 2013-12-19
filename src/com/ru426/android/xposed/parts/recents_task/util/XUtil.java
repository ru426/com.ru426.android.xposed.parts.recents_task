package com.ru426.android.xposed.parts.recents_task.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

public class XUtil {
	public static HashMap<String, Object> getSystemUIValue(Context context, String value, String type){
		if(context == null) return null;
		HashMap<String, Object> result = new HashMap<String, Object>();
		Context mContext = null;
		int id = 0;
		try {
			mContext = context.createPackageContext("com.android.systemui", Context.CONTEXT_RESTRICTED);
			id = mContext.getResources().getIdentifier(value, type, mContext.getPackageName());
			result.put("id", id);
			result.put("isExists", id > 0);
			return result;
		} catch (NameNotFoundException e) {
			return null;
		}
 	}
	
	public static void copyPreferences(SharedPreferences source, SharedPreferences target, String sourceKey){
		Map<String, ?> pluginPrefAll = source.getAll();
		if(sourceKey == null || sourceKey.length() == 0){
			Object obj = pluginPrefAll.get(sourceKey);
			copyPreferenceCore(target, sourceKey, obj);
		}else{
			for(String key : pluginPrefAll.keySet()){
				Object obj = pluginPrefAll.get(key);
				copyPreferenceCore(target, key, obj);
			}
		}
	}
	
	private static void copyPreferenceCore(SharedPreferences target, String key, Object obj){
		try{
			boolean value = (Boolean) obj;
			target.edit().putBoolean(key, value).commit();
		}catch(ClassCastException e){
			try{
				int value = (Integer) obj;
				target.edit().putInt(key, value).commit();
			}catch(ClassCastException e1){
				try{
					long value = (Long) obj;
					target.edit().putLong(key, value).commit();
				}catch(ClassCastException e2){
					try{
						float value = (Float) obj;
						target.edit().putFloat(key, value).commit();
					}catch(ClassCastException e3){											
						try{
							String value = (String) obj;
							target.edit().putString(key, value).commit();
						}catch(ClassCastException e4){
							try{
								@SuppressWarnings("unchecked")
								Set<String> value = (Set<String>) obj;
								target.edit().putStringSet(key, value).commit();
							}catch(ClassCastException e5){
							}
						}
					}
				}
			}
		}
	}
}
