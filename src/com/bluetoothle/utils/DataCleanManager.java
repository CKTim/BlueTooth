package com.bluetoothle.utils;

import java.io.File;
import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("SdCardPath")
public class DataCleanManager {
	//ɾ�����ݿ�
	public static void cleanDatabases(Context context) {
		deleteFilesByDirectory(new File("/data/data/" + context.getPackageName() + "/databases"));
	}
	//ɾ�����ݿ�
	public static void cleanDatabasesByName(Context context,String dbName) {
		context.deleteDatabase(dbName);
	}
   //ɾ��sharedpreference
	public static void cleanSharedPreference(Context context) {
		deleteFilesByDirectory(new File("/data/data/" + context.getPackageName() + "/shared_prefs"));
	}
   //ɾ������,����һ���ļ��У�����ɾ��
	private static void deleteFilesByDirectory(File directory) {
      if(directory!=null&&directory.exists()&&directory.isDirectory()){
    	  for(File item:directory.listFiles()){
    		  item.delete();
    	  }
      }
	}
}
