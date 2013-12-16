package com.ru426.android.xposed.parts.recents_task;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.ru426.android.xposed.library.ModuleBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class TaskswitcherModule extends ModuleBase {
	private static final String TAG = TaskswitcherModule.class.getSimpleName();
	public static final String STATE_CHANGE = TaskswitcherModule.class.getName() + ".intent.action.STATE_CHANGE";
	public static final String STATE_EXTRA_IS_MOVE_OR_ADD = TaskswitcherModule.class.getName() + ".intent.extra.STATE_EXTRA_IS_MOVE_OR_ADD";
	
	private static LinearLayout mTaskManagerButtonContainer;
	private static RelativeLayout mRecentAppsPanelView;
	private static boolean isMoveOrAdd;
	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);		
		if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT) {
			return;
		} else if (Build.VERSION_CODES.ICE_CREAM_SANDWICH <= Build.VERSION.SDK_INT
				&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
		}
		isMoveOrAdd = (Boolean) xGetValue(prefs, xGetString(R.string.move_or_add_kill_all_apps_button_key), false);
		
		Class<?> xTaskswitcher = XposedHelpers.findClass("com.sonymobile.taskswitcher.TaskSwitcher", classLoader);
		XposedBridge.hookAllConstructors(xTaskswitcher, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				xLog(TAG + " : " + "afterHookedMethod hookAllConstructors");
				try{
					FrameLayout mTaskSwitcher = (FrameLayout) param.thisObject;
					mContext = mTaskSwitcher.getContext();
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(TaskswitcherModule.STATE_CHANGE);
					xRegisterReceiver(mContext, intentFilter);
					moveKillAllAppsButton(param);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}			
		});
		
		Object callback[] = new Object[2];
		callback[0] = Context.class;
		callback[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				xLog(TAG + " : " + "afterHookedMethod setupView");
				try{
					moveKillAllAppsButton(param);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}	
		};
		xHookMethod(xTaskswitcher, "setupView", callback, true);
		
		Object callback2[] = new Object[2];
		callback2[0] = int.class;
		callback2[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				xLog(TAG + " : " + "afterHookedMethod setVisibility");
				try{
					moveKillAllAppsButton(param);
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}				
			}	
		};
		xHookMethod(xTaskswitcher, "setVisibility", callback2, true);
	}
	
	private static void moveKillAllAppsButton(MethodHookParam param){
		FrameLayout mTaskSwitcher = (FrameLayout) param.thisObject;		
		if(makeTaskManagerButtonContainer(param, mTaskSwitcher.getContext())){
			moveKillAllAppsButtonCore();
		}else{
			// CubeMod SystemUI
			int id = mRecentAppsPanelView.getContext().getResources().getIdentifier("button_paddingtop", "id", mRecentAppsPanelView.getContext().getPackageName());
			if(id > 0){
				mTaskSwitcher.findViewById(id).setVisibility(isMoveOrAdd ? View.VISIBLE : View.GONE);
			}
		}
	}	
	
	private static boolean makeTaskManagerButtonContainer(MethodHookParam param, Context context){
		if(!isMoveOrAdd) return false;
		try{
			mRecentAppsPanelView = (RelativeLayout) XposedHelpers.getObjectField(param.thisObject, "mRecentAppsPanelView");
		}catch(Exception e){
			XposedBridge.log(e);
		}
		
		int id = mRecentAppsPanelView.getContext().getResources().getIdentifier("button_paddingtop", "id", mRecentAppsPanelView.getContext().getPackageName());
		if(id > 0){
			XposedBridge.log("This TaskSwitcher is CubeMod TaskSwitcher");
			return false;
		}

		Button mTaskManagerButton = null;
		try{
			mTaskManagerButton = (Button) XposedHelpers.getObjectField(param.thisObject, "mTaskManagerButton");
		}catch(Exception e){
			XposedBridge.log(e);
		}
		
		if(mTaskManagerButton != null && mTaskManagerButton.getParent() != null){
			if(mTaskManagerButtonContainer == null){
				mTaskManagerButtonContainer = new LinearLayout(context);
				mTaskManagerButtonContainer.setOrientation(LinearLayout.VERTICAL);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				mTaskManagerButtonContainer.setPadding(20, 70, 0, 160);
				mTaskManagerButtonContainer.setLayoutParams(params);
				View padView = new View(context);
				params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
				padView.setLayoutParams(params);
				mTaskManagerButtonContainer.addView(padView);
			}
			if(mTaskManagerButton.getParent() != null){
				ViewGroup parent = (ViewGroup) mTaskManagerButton.getParent();
				parent.removeView(mTaskManagerButton);
			}
			if(mTaskManagerButtonContainer.getChildCount() > 1 && mTaskManagerButtonContainer.getChildAt(1) != null) mTaskManagerButtonContainer.removeViewAt(1);
			mTaskManagerButtonContainer.addView(mTaskManagerButton);
			if(mTaskManagerButtonContainer.getParent() != null){
				ViewGroup parent = (ViewGroup) mTaskManagerButtonContainer.getParent();
				parent.removeView(mTaskManagerButtonContainer);
			}
			mRecentAppsPanelView.addView(mTaskManagerButtonContainer);
			return true;
		}
		return false;
	}
	
	private static void moveKillAllAppsButtonCore(){
		if(mTaskManagerButtonContainer != null){
			if(mTaskManagerButtonContainer.getChildCount() > 0) mTaskManagerButtonContainer.getChildAt(0).setVisibility(isMoveOrAdd ? View.VISIBLE : View.GONE);
		}
	}
	
	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + intent.getAction());
		if (intent.getAction().equals(TaskswitcherModule.STATE_CHANGE)) {
			isMoveOrAdd = intent.getBooleanExtra(TaskswitcherModule.STATE_EXTRA_IS_MOVE_OR_ADD, false);
			moveKillAllAppsButtonCore();
		}
	}
}
