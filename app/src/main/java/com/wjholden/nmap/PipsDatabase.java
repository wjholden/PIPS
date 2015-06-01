package com.wjholden.nmap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;

/**
 * This code might be a little spaghetti-ish for the first major version. It's
 * based on the famous Notepad example from Google and a Lars Vogel article.
 * Since this is my first foray into Android/SQLite it might be a little clumsy.
 * 
 * @author John
 * 
 */
public class PipsDatabase implements Runnable {

	public static final String TABLE = "pipsTable";
	public static final String KEY_ROWID = "_id";
	public static final String KEY_RESULT = "result";
	public static final String KEY_TARGET = "target";
	public static final String KEY_DATE = "dtg";
	
	/**
	 * It is what it looks like - an attempt to add additional
	 * safeguards and error checking throughout the code to
	 * detect strange errors for unique situations.
	 */
	private transient boolean isOpen = false;

	private transient final Context context;
	private transient DatabaseHelper helper;
	private transient SQLiteDatabase database;
    private transient Handler handler;

	public PipsDatabase(final Context context, final Handler handler) {
		this.context = context;
		this.handler = handler;
	}

	/**
	 * Opens a reference to the database. TODO No longer public - must access
	 * instances of the database through a thread instead.
	 * 
	 * @return
	 * @throws SQLException
	 */
	private PipsDatabase open() throws SQLException {
		PipsError.log("Opening " + Constants.DATABASE_NAME);
		helper = new DatabaseHelper(context);
		database = helper.getWritableDatabase();
		this.isOpen = true;
        if (handler != null)
		    handler.sendEmptyMessage(Constants.DB_OPEN);
		return this;
	}

	/**
	 * Opens the database in a separate thread.
	 */
	public void run() {
		this.open();
	}

	public void close() {
		PipsError.log("Closing database.");
		helper.close();
		this.isOpen = false;
	}
	
	/**
	 * This getter variable reflects expected database helper
	 * state. I see no reason not to trust this. 
	 * @return True if database is open and safe to read/write.
	 */
	public boolean isOpen() {
		return this.isOpen;
	}

	/**
	 * Date is inferred as NOW().
	 * 
	 * @param result
	 * @param target
	 * @return
	 */
	public long insert(final String result, final String target) {
		PipsError.log("Inserting new record into database.");
		final ContentValues contentValues = new ContentValues();
		contentValues.put(KEY_RESULT, result);
		contentValues.put(KEY_TARGET, target);
		contentValues.put(KEY_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
				Locale.US).format(new Date()));
		return database.insert(TABLE, null, contentValues);
	}

	public long deleteResult(final long rowId) {
		PipsError.log("Deleting record " + rowId + " from database.");
		return database.delete(TABLE, KEY_ROWID + "=" + rowId, null);
	}

	public long deleteAll() {
		PipsError.log("Deleting all records from database.");
		return database.delete(TABLE, "1", null);
	}

	public List<ScanResult> fetchAllAsList() {
		final List<ScanResult> results = new ArrayList<ScanResult>();

		if (database.isOpen()) {
			PipsError.log("Database is open to fetch results.");
			final Cursor cursor = database.query(TABLE, new String[] {
					KEY_ROWID, KEY_RESULT, KEY_TARGET, KEY_DATE }, null, null,
					null, null, KEY_ROWID + " DESC");
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				results.add(new ScanResult(cursor.getLong(0), cursor
						.getString(1), cursor.getString(2), cursor.getString(3)));
				cursor.moveToNext();
			}
			cursor.close();
		} else {
			PipsError.log("Database is not open, unable to retrieve results.");
		}

		PipsError.log("Retrieved List<> of scan results (" + results.size()
				+ " total).");

		return results;
	}

	public String fetchLastAsString() {
		final List<ScanResult> results = fetchAllAsList();
		String result = null;
		if (!results.isEmpty()) {
			result = results.get(0).getResult();
		}
		// throw(new java.lang.IllegalStateException());
		return result;
	}

	/**
	 * Gets a list of all distinct targets that have been scanned, newest first.
	 * This is intended for use with an <a href=
	 * "http://developer.android.com/resources/tutorials/views/hello-autocomplete.html"
	 * > AutoCompleteTextView</a>.
	 * 
	 * @return String list of historical targets.
	 */
	public List<String> fetchTargets() {
		final List<String> targets = new ArrayList<String>();

		final Cursor cursor = database.query(true, TABLE,
				new String[] { KEY_TARGET }, null, null, null, null, KEY_ROWID
						+ " DESC", null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			targets.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();

		PipsError.log("Retrieved List<String> of " + targets.size()
				+ " old targets to auto-suggest.");

		return targets;
	}
}
