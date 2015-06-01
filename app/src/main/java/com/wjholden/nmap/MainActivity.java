/**
 * TODO: 
 * (done) email debugging info
 * Subnet viewer
 * Easy "Scan My Subnet" button
 * Show IP address, if connected to wifi
 * (done) Licensing
 * Show notification when completed in notification area.
 * Disable scan button while scanning (synchronization).
 * (done) Long-press on PipsViewResultActivity should OPEN, SHARE, DELETE.
 * Add OnOptionsMenu to each activity.
 *  
 * (gif): save scan results to db in Scan class, then notification thread is easier (in case of dead handler).
 */

package com.wjholden.nmap;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**

 * @author john
 *
 */
public class MainActivity extends Activity {

	private transient Button btnStart, btnHelp, btnHistory, btnShare;
	private transient EditText etxtArguments;

	/**
	 * Changed from an EditText to an 
	 * <a href="http://developer.android.com/resources/tutorials/views/hello-autocomplete.html">AutoCompleteTextView</a>
	 * so that I can provide the user a nice list of historical targets, 
	 * pulled directly from the database with PipsDatabase.fetchTargets().
	 */
	private transient AutoCompleteTextView autoCompleteTarget;
	private transient TextView txtResults;

	public static ProgressDialog progressDialog;

	private static Context context;

	private transient int currentVersion = 0;
	private transient boolean hasRoot;
	private transient boolean forceRoot;
	private transient boolean saveHistory;
	private transient boolean showLogcat;

	private Handler handler;

	private transient Thread scanThread;

	private transient PipsDatabase db;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		btnStart = (Button) findViewById(R.id.Start);
		btnHelp = (Button) findViewById(R.id.Help);
		btnHistory = (Button) findViewById(R.id.History);
		btnShare = (Button) findViewById(R.id.Share);

		autoCompleteTarget = (AutoCompleteTextView) findViewById(R.id.Target);
		etxtArguments = (EditText) findViewById(R.id.Arguments);

		txtResults = (TextView) findViewById(R.id.Results);

		context = this.getApplicationContext();

		btnStart.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (scanThread == null || !scanThread.isAlive()) {
					hasRoot = forceRoot ? forceRoot : Utilities.canGetRoot();
					Scan scan = new Scan(Utilities.getApplicationFolder(context,
							"bin"), hasRoot, autoCompleteTarget.getText().toString(),
							etxtArguments.getText().toString(), 0, context, saveHistory, handler);
					scanThread = new Thread(scan);
					scanThread.start();
					scanThread.setName("Scan thread: " + autoCompleteTarget.getText().toString());
				} else {
					scanThread.interrupt(); // TODO this does nothing
				}
			}
		});

		btnHelp.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				if (scanThread == null || !scanThread.isAlive()) {
					Scan scan = new Scan(Utilities.getApplicationFolder(context,
							"bin"), hasRoot, autoCompleteTarget.getText().toString(),
							"", 1, context, false, handler);
					scanThread = new Thread(scan);
					scanThread.setName("Help thread");
					scanThread.start();
				} else {
					scanThread.interrupt(); // TODO this does nothing
				}
			}
		});
		
		btnHistory.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				PipsError.log("History button pressed.");
				Intent intent = new Intent(MainActivity.this, PipsListActivity.class);
				startActivity(intent);
			}
		});
		
		btnShare.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				final String body = (String) txtResults.getText();
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Nmap Scan Result");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
				MainActivity.this.startActivity(emailIntent);
			}
		});
	}

	public static Context getContext() {
		return (context);
	}

	protected void onPause() {
		final SharedPreferences.Editor preferencesEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();

		// wipe everything before adding anything
		preferencesEditor.clear();

		if (saveHistory) {
			preferencesEditor.putString("target", autoCompleteTarget.getText().toString());
			preferencesEditor.putString("args", etxtArguments.getText().toString());
		} else {
			preferencesEditor.putString("target", "");
			preferencesEditor.putString("args", "");
		}
		preferencesEditor.putInt("versionLastRun", currentVersion);
		preferencesEditor.putBoolean("forceRoot", forceRoot);
		preferencesEditor.putBoolean("saveHistory", saveHistory);
		preferencesEditor.putBoolean("showLogcat", showLogcat);

		preferencesEditor.commit(); // TODO once you get up to sdk 9 switch to apply().
		
		if (scanThread != null && scanThread.isAlive()) {
			Thread closeDb = new Thread(new Runnable() {

				public void run() {
					PipsError.log("Spinlocking while waiting for scan to complete.");
					while (scanThread.isAlive())
						; // spinlock
					db.close();
				}
				
			});
			closeDb.start();
		} else {
			db.close();
		}

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				// TODO what happens if a thread is running when the handler is resumed?
				PipsError.log("Handler received: " + msg.what + " Object: "
						+ msg.obj);

				if (msg.obj != null && !((msg.obj instanceof String) || (msg.obj instanceof List<?>)))
				{
					PipsError.log("Warning: handler received unexpected non-null Message.obj not instanceof String or List.");
				}

				switch (msg.what) {
				case Constants.INSTALL_COMPLETE:
					// TODO do nothing, no output to show user.
					break;
				case Constants.INSTALL_ERROR:
					// TODO show error to user.
					break;
				case Constants.PROGRESS_DIALOG_START:
					progressDialog = ProgressDialog.show(MainActivity.this, "",
							(String) msg.obj, true);
					break;
				case Constants.PROGRESS_DIALOG_DISMISS:
					progressDialog.dismiss();
					progressDialog = null; // TODO this might not be safe
					break;
				case Constants.PROGRESS_DIALOG_CHANGE_TEXT:
					if (progressDialog.isShowing())
						progressDialog.setMessage((String) msg.obj);
					else
						PipsError.log("Progress dialog is not showing but text changed.");
					break;
				case Constants.SCAN_ERROR_NULL_PROCESS:
					txtResults
							.setText("Unable to start compiled Nmap program.");
					btnStart.setEnabled(true);
					break;
				case Constants.SCAN_ERROR_IOEXCEPTION:
					StringBuilder sb = new StringBuilder("An I/O error occured.\n");
					sb.append(msg.obj);
					sb.append('\n');
					if (forceRoot) {
						sb.append("Force Root is turned on - are you sure the \"su\" command is available?");
					}
					AlertDialog.Builder alertIOException = new AlertDialog.Builder(
							MainActivity.this).setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					alertIOException.setMessage(sb);
					alertIOException.show();
					txtResults.setText((String) msg.obj);
					btnStart.setEnabled(true);
					break;
				case Constants.SCAN_ERROR_STANDARD_ERROR:
					AlertDialog.Builder alert = new AlertDialog.Builder(
							MainActivity.this).setPositiveButton("OK",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					alert.setMessage((String) msg.obj);
					alert.show();
					btnStart.setEnabled(true);
					break;
				case Constants.SCAN_COMPLETE:
					String result = (String) msg.obj;
					txtResults.setText(result);

					bindAutoCompleteAdapter(); // rebind now that data is changed.
					break;
				case Constants.DB_SHOW_LAST_RESULT:
					try
					{
						String lastResult = db.fetchLastAsString();
						if (lastResult != null) {
							txtResults.setText(lastResult);
						}
					}
					catch (IllegalStateException e)
					{
						PipsError.log(e);

						AlertDialog.Builder illegalStateBuilder = new AlertDialog.Builder(MainActivity.this);
						illegalStateBuilder.setMessage(R.string.illegalStateExceptionError);
						illegalStateBuilder.setPositiveButton("Yes", sendDebugListener).setNegativeButton("No", sendDebugListener);
						illegalStateBuilder.show();
					}
					break;
				case Constants.DB_OPEN:
					bindAutoCompleteAdapter();
					break;
				case Constants.UPDATE_COMPLETE_NO_ERRORS:
					progressDialog.dismiss();
					progressDialog = null;
					break;
				case Constants.UPDATE_COMPLETE_WITH_ERRORS:
					progressDialog.dismiss();
					progressDialog = null; // try to destroy the old object

					AlertDialog.Builder updateErrorAlert = new AlertDialog.Builder(MainActivity.this);
					updateErrorAlert.setTitle("Update Failed");

					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("Could not download these files: \n");
					if (msg.obj instanceof List<?>)
					{
						@SuppressWarnings("unchecked")
						List<String> errors = (List<String>) msg.obj;
						for (String f : errors) {
							stringBuilder.append(f).append("\n");
						}
					} else {
						stringBuilder.append("(A serious error has occurred while updating support files. Please contact wjholden@gmail.com");
					}
					updateErrorAlert.setMessage(stringBuilder.toString());
					updateErrorAlert.show();
					break;
				case Constants.UPDATE_IN_PROGRESS:
					progressDialog.setProgress(msg.arg1);
					break;
				case Constants.UPDATE_IN_PROGRESS_STRING:
					progressDialog.setMessage((String) msg.obj);
					break;
				default:
					PipsError.log("Handler received unexpected message that the switch statement does not allow.");
					break;
				}
			}
		};

		db = new PipsDatabase(this, this.handler);
		Thread dbThread = new Thread(db);
		dbThread.start(); // TODO: holy shit...
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		autoCompleteTarget.setText(settings.getString("target", ""));
		etxtArguments.setText(settings.getString("args", ""));
		int lastVersionRun = settings.getInt("versionLastRun", -1);
		forceRoot = settings.getBoolean("forceRoot", false);
		saveHistory = settings.getBoolean("saveHistory", true);
		showLogcat = settings.getBoolean("showLogcat", false);
		PipsError.setLogcatVisible(showLogcat);
		
		try {
			currentVersion = getPackageManager().getPackageInfo(
					"com.wjholden.nmap", 0).versionCode;
		} catch (NameNotFoundException e) {
			PipsError.log(e.toString());
		} finally {
			if (currentVersion > lastVersionRun) {
				hasRoot = forceRoot ? forceRoot : Utilities.canGetRoot();
				
				Install install = new Install(MainActivity.context,
						Utilities.getApplicationFolder(MainActivity.context,
								"bin"), hasRoot, this.handler);
				Thread installThread = new Thread(install);
				installThread.setName("Install Thread");
				installThread.start();
			}
		}
		
		if (forceRoot) {
			PipsError.log("Found true forceRoot key in Shared Preferences.");
			hasRoot = true;
		}
		
		Thread showLastResult = new Thread(new Runnable() {
			public void run() {
				long startTime = System.nanoTime();
				while (!db.isOpen())
					; // spinlock until DB open.
				long endTime = System.nanoTime();
				double duration = (endTime - startTime) / 1000000000.0d;
				PipsError.log("Spinlocked for " + (duration) 
						+ " seconds waiting for database to open.");
				handler.sendEmptyMessage(Constants.DB_SHOW_LAST_RESULT);
			}
		});
		showLastResult.start();
	}
	
	/**
	 * This callback provides a means for the user to send debugging information
	 * to the developer (me).
	 * It is intended to be used in catch statements for known bugs to gather
	 * information specific to an ongoing issue.
	 */
	private DialogInterface.OnClickListener sendDebugListener = new DialogInterface.OnClickListener() {
		
		public void onClick(DialogInterface dialog, int which) {
			PipsError.log("Presented choice to send debugging information to developer.");
			switch (which)
			{
			case DialogInterface.BUTTON_POSITIVE:
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				final StringBuilder fullErrorLog = new StringBuilder();
				
				for (String errorLine : PipsError.getLog())
				{
					fullErrorLog.append(errorLine).append('\n');
				}
				
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "PIPS Debugging Output");
				emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "wjholden@gmail.com" });
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, fullErrorLog.toString());
				MainActivity.this.startActivity(emailIntent);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				
				break;
			}
		}
	};
	
	/**
	 * Provides a nice list of historical targets for ease of use.
	 * I decided to create a method to do this since it needs to be called more than once -
	 * could not count on notifyDataSetChanged because the ArrayAdapter itself does not
	 * change with the database until you tell it to fetch updated information from said db.
	 */
	private void bindAutoCompleteAdapter()
	{
		if (db.isOpen()) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, db.fetchTargets());
			autoCompleteTarget.setAdapter(adapter);
		} else {
			PipsError.log("Not binding autocomplete because database is closed.");
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.showOptionsActivity:
			Intent intent = new Intent(MainActivity.this, PipsOptions.class);
			startActivity(intent);
			break;
		case R.id.clearItem:
			PipsError.log("Clear button pressed.");
			txtResults.setText(R.string.gpl);
			etxtArguments.setText("");
			autoCompleteTarget.setText("");
			break;
		case R.id.pingItem:
			Intent pingIntent = new Intent(MainActivity.this, PingActivity.class);
			startActivity(pingIntent);
			break;
		case R.id.updateItem:
			PipsError.log("Update button pressed.");
			String folder = Utilities.getApplicationFolder(this.getApplicationContext(), "bin");
			UpdateSupportFiles updateRunnable = new UpdateSupportFiles(folder, handler);
			Thread updateThread = new Thread(updateRunnable);
			updateThread.start();
			progressDialog = new ProgressDialog(MainActivity.this);
			progressDialog.setTitle("Updating Support Files");
			progressDialog.setMessage("Starting...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setProgress(0);
			progressDialog.setMax(updateRunnable.getNumberOfFiles());
			progressDialog.show();
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_options, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

}
