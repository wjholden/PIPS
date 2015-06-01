package com.wjholden.nmap;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;


public class PipsListActivity extends ListActivity {
	
	private transient PipsDatabase database;
	private transient ArrayAdapter<ScanResult> adapter;

	@Override
	protected void onCreate(final Bundle bundle) {
		super.onCreate(bundle);

		registerForContextMenu(this.getListView());
		
		showHelpMessage();
	}
	
	private void showHelpMessage() {
		Context context = getApplicationContext();
		CharSequence text = context.getResources().getString(R.string.longPress);
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	

	@Override
	protected void onPause() {
		database.close();
		super.onPause();
	}

	@Override
	protected void onResume() {
		database = new PipsDatabase(this, null);
		Thread dbThread = new Thread(database);
		dbThread.run();
		bindListAdapter();
		super.onResume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		PipsError.log("Click list item at position " + position);

		showScanResult(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_activity_context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId())
		{
		case R.id.open:
			showScanResult(info.position);
			break;
		case R.id.share:
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			final ScanResult scanResult = adapter.getItem(info.position);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Nmap Scan Results");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, scanResult.getResult());
			this.startActivity(emailIntent);
			break;
		case R.id.delete:
			database.deleteResult(adapter.getItem(info.position).getIdentifier());
			break;
		}
		
		bindListAdapter();
		return super.onContextItemSelected(item);
	}

	private void showScanResult(final int position)
	{
		Intent intent = new Intent(PipsListActivity.this, PipsViewResultActivity.class);
		intent.putExtra("scanResult", adapter.getItem(position).getResult());
		startActivity(intent);
	}
	
	/**
	 * It's lame, but as far as I can tell it's absolutely necessary to simply reinitialize
	 * the view after the information in the database is updated.
	 * Let the ugly hacks begin...
	 */
	private void bindListAdapter()
	{
		final List<ScanResult> values = database.fetchAllAsList();
		adapter = new ArrayAdapter<ScanResult>(this, R.layout.scanresults, values);
		setListAdapter(adapter);
	}
}
