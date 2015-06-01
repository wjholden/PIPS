package com.wjholden.nmap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {

	DatabaseHelper(final Context context) {
		super(context, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
	}

	@Override
	public
	void onCreate(final SQLiteDatabase database) {
		PipsError.log(Constants.DATABASE_CREATE);
		database.execSQL(Constants.DATABASE_CREATE);
	}

	@Override
	public void onUpgrade(final SQLiteDatabase database,
			final int oldVersion, final int newVersion) {
		PipsError.log("Erasing old database completely.");
		database.execSQL("DROP TABLE IF EXISTS " + PipsDatabase.TABLE);
		onCreate(database);
	}
}