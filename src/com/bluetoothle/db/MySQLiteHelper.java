package com.bluetoothle.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "Mydata.db";
	private static final int DATABASE_VERSION = 1;


	public MySQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS sample" +"(_id INTEGER PRIMARY KEY AUTOINCREMENT, sampleNumber VARCHAR,sampleIndex VARCHAR,sampleName VARCHAR,sampleNameByte VARCHAR)");
		db.execSQL("CREATE TABLE IF NOT EXISTS record" +"(_id INTEGER PRIMARY KEY AUTOINCREMENT, recordNumber VARCHAR,vegetableName VARCHAR,year VARCHAR,month VARCHAR,data VARCHAR,hour VARCHAR,min VARCHAR,second VARCHAR,InhibitionRate VARCHAR,result VARCHAR,printByte VARCHAR,synchronization VARCHAR)");
		Log.e("AAAAAA","数据库创建成功");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
