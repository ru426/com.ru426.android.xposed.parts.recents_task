package com.ru426.android.xposed.parts.recents_task;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ru426.android.xposed.library.ModuleBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class RecentsActivityModule extends ModuleBase {
	public static final String PACKAGE_NAME = RecentsActivityModule.class.getPackage().getName();
	private static final String TAG = RecentsActivityModule.class.getSimpleName();
	public static final String STATE_CHANGE = RecentsActivityModule.class.getName() + ".intent.action.STATE_CHANGE";
	public static final String STATE_EXTRA_IS_MOVE_OR_ADD = RecentsActivityModule.class.getName() + ".intent.extra.STATE_EXTRA_IS_MOVE_OR_ADD";
	
	private static LinearLayout mTaskButtonContainer;
	private static FrameLayout mRecentsPanel;
	private static int recents_pluginview_container_id = -1;
	private static boolean isMoveOrAdd;
	
	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		isMoveOrAdd = (Boolean) xGetValue(prefs, xGetString(R.string.move_or_add_kill_all_apps_button_key), false);
		Class<?> xRecentsActivity = null;
		try{
			xRecentsActivity = XposedHelpers.findClass("com.android.systemui.recent.RecentsActivity", classLoader);
		}catch(ClassNotFoundError e){
			xLog(TAG + " : " + e.getMessage());
		}
		if(xRecentsActivity == null) return;
		
		Object callback[] = new Object[1];
		callback[0] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod onResume");
					if(recents_pluginview_container_id > 0) moveKillAllAppsButton(param);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}	
		};
		xHookMethod(xRecentsActivity, "onResume", callback, true);
		
		Object callback2[] = new Object[2];
		callback2[0] = Bundle.class;
		callback2[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod onCreate");
					mContext = (Context) param.thisObject;
					if(mContext != null){
						recents_pluginview_container_id = mContext.getResources().getIdentifier("recents_pluginview_container", "id", mContext.getPackageName());
						if(recents_pluginview_container_id > 0){
							IntentFilter intentFilter = new IntentFilter();
							intentFilter.addAction(STATE_CHANGE);
							xRegisterReceiver(mContext, intentFilter);
						}				
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}	
		};
		xHookMethod(xRecentsActivity, "onCreate", callback2, true);
	}
	
	private static void moveKillAllAppsButton(MethodHookParam param){
		if(makeTaskButtonContainer(param, mContext)){
			moveKillAllAppsButtonCore();
		}
	}
	
	private static boolean makeTaskButtonContainer(MethodHookParam param, Context context){
		if(!isMoveOrAdd) return false;
		try{
			mRecentsPanel = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mRecentsPanel");
		}catch(Exception e){
			XposedBridge.log(e);
		}
		
		if(mTaskButtonContainer == null){
			mTaskButtonContainer = (LinearLayout) makeRecentsActivityTaskController(context);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			mTaskButtonContainer.setPadding(0, 16, 0, 160);
			mTaskButtonContainer.setLayoutParams(params);
			Button taskKillerButton = (Button) mTaskButtonContainer.findViewById(xModuleResources.getIdentifier("task_killer_button", "id", PACKAGE_NAME));
			taskKillerButton.setOnClickListener(new OnClickListener() {				
				@Override
				public void onClick(View v) {
					killTasks();
					try{
						XposedHelpers.callMethod(mRecentsPanel, "refreshViews");
						XposedHelpers.callMethod(mRecentsPanel, "dismissAndGoBack");
					}catch(Exception e){
						XposedBridge.log(e);
					}
				}
			});
		}

		if(mTaskButtonContainer.getParent() != null){
			ViewGroup parent = (ViewGroup) mTaskButtonContainer.getParent();
			parent.removeView(mTaskButtonContainer);
		}
		int index = 0;
		if(mRecentsPanel.getChildCount() > 0){
			index = mRecentsPanel.getChildCount() - 1;
		}
		mRecentsPanel.removeView(mTaskButtonContainer);
		mRecentsPanel.addView(mTaskButtonContainer, index);
		return true;
	}
	
	private static View makeRecentsActivityTaskController(Context context){
		Context mContext = null;
		View view = null;
		try {
			mContext = context.createPackageContext(PACKAGE_NAME, 3);
			view = View.inflate(mContext, R.layout.task_killer_button_layout, null);
		} catch (NameNotFoundException e) {
			XposedBridge.log(e);
		}
		return view;
	}
	
	private static void moveKillAllAppsButtonCore(){
		if(mTaskButtonContainer != null){
			mTaskButtonContainer.findViewById(xModuleResources.getIdentifier("button_paddingtop", "id", PACKAGE_NAME)).setVisibility(isMoveOrAdd ? View.VISIBLE : View.GONE);
		}
	}
	
	private static boolean killTasks() {
		boolean flag = false;
		if (mContext != null) {
			ActivityManager activitymanager = (ActivityManager) mContext.getSystemService("activity");
			List<RecentTaskInfo> list = activitymanager.getRecentTasks(1000, 2);

			if (list != null) {
				android.content.pm.PackageManager packagemanager = mContext.getPackageManager();
				ActivityInfo activityinfo = (new Intent("android.intent.action.MAIN")).addCategory("android.intent.category.HOME").resolveActivityInfo(packagemanager, 0);
				int i = 1;
				while (i < list.size()) {
					String s = ((android.app.ActivityManager.RecentTaskInfo) list.get(i)).baseIntent.getComponent().getPackageName();
					if (!s.equals(activityinfo.packageName)) {
						try {
							//activitymanager.removeTask(((android.app.ActivityManager.RecentTaskInfo) list.get(i)).persistentId, 1);
							XposedHelpers.callMethod(activitymanager, "removeTask", new Object[]{ ((android.app.ActivityManager.RecentTaskInfo) list.get(i)).persistentId, 1} );
							activitymanager.killBackgroundProcesses(s);
						} catch (Exception e) {
							XposedBridge.log(e);
						}
						flag = true;
					}
					i++;
				}
			}
		}
		return flag;
	}

	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + intent.getAction());
		if (intent.getAction().equals(STATE_CHANGE)) {
			isMoveOrAdd = intent.getBooleanExtra(STATE_EXTRA_IS_MOVE_OR_ADD, false);
			moveKillAllAppsButtonCore();
		}
	}
}
