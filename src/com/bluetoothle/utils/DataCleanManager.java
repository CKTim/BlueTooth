package com.bluetoothle.utils;

import java.io.File;
import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("SdCardPath")
public class DataCleanManager {
	//删除数据库
	public static void cleanDatabases(Context context) {
		deleteFilesByDirectory(new File("/data/data/" + context.getPackageName() + "/databases"));
	}
	//删除数据库
	public static void cleanDatabasesByName(Context context,String dbName) {
		context.deleteDatabase(dbName);
	}
   //删除sharedpreference
	public static void cleanSharedPreference(Context context) {
		deleteFilesByDirectory(new File("/data/data/" + context.getPackageName() + "/shared_prefs"));
	}
   //删除方法,传入一个文件夹，遍历删除
	private static void deleteFilesByDirectory(File directory) {
      if(directory!=null&&directory.exists()&&directory.isDirectory()){
    	  for(File item:directory.listFiles()){
    		  item.delete();
    	  }
      }
	}
}
