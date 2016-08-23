package com.bluetoothle.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;

public class ActivityManager {
	private static List<Activity> activityList = new ArrayList<Activity>();
	private static ActivityManager instance;

	public static ActivityManager getActivityManager() {
		if (instance == null) {
			instance = new ActivityManager();
		}
		return instance;
	}
	
	//添加Activity
	public void addActivity(Activity activity){
		if(activityList==null){
			activityList=new ArrayList<Activity>();
		}
		activityList.add(activity);
	}
	
	//销毁所有activity
	public void finishAllActivity(){
		for(int i=0;i<activityList.size();i++){
			if (null != activityList.get(i)){  
				activityList.get(i).finish();  
            } 
		}
	}

}
