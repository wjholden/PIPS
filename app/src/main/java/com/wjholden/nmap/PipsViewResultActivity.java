/**
 * 
 */
package com.wjholden.nmap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

/**
 * @author john
 *
 */
public class PipsViewResultActivity extends Activity {
	
	private transient TextView textLabel;
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.viewresult);
		
		textLabel = (TextView) findViewById(R.id.viewResultText);
		String scanResult = getIntent().getExtras().getString("scanResult");
		textLabel.setText(scanResult);
		
		textLabel.setOnLongClickListener(new OnLongClickListener() {

			public boolean onLongClick(View arg0) {
				final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
				emailIntent.setType("plain/text");
				emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Nmap Scan Results");
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, textLabel.getText());
				PipsViewResultActivity.this.startActivity(emailIntent);
				return true;
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		return super.onOptionsItemSelected(item);
	}
}
