package com.wjholden.nmap;

class Constants {
	public final static int INSTALL_COMPLETE = 1;
	public final static int INSTALL_ERROR = -1;

	public final static int PROGRESS_DIALOG_CHANGE_TEXT = 100;
	public final static int PROGRESS_DIALOG_START = 101;
	public final static int PROGRESS_DIALOG_DISMISS = 102;

	public final static int SCAN_ERROR_NULL_PROCESS = -200;
	public final static int SCAN_ERROR_STANDARD_ERROR = -201;
	public final static int SCAN_ERROR_IOEXCEPTION = -202;
	public final static int SCAN_COMPLETE = 203;

	public final static int DB_SHOW_LAST_RESULT = 300;
	public final static int DB_OPEN = 301;
	public final static int DB_CLOSE = 302;
	
	public final static int UPDATE_IN_PROGRESS = 401;
	public final static int UPDATE_IN_PROGRESS_STRING = 402;
	public final static int UPDATE_COMPLETE_NO_ERRORS = 400;
	public final static int UPDATE_COMPLETE_WITH_ERRORS = 404;

	public final static String TAG = "PIPS";

	public static final int DATABASE_VERSION = 3;
	public static final String DATABASE_NAME = "pips.db";
	public static final String DATABASE_CREATE = "create table "
			+ PipsDatabase.TABLE + "( " + PipsDatabase.KEY_ROWID
			+ " integer primary key autoincrement, " + PipsDatabase.KEY_RESULT
			+ " text not null, " + PipsDatabase.KEY_TARGET + " text not null, "
			+ PipsDatabase.KEY_DATE + " date" + ");";
}
