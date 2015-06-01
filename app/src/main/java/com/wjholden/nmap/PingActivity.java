package com.wjholden.nmap;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Tragically, it appears ICMP is fundamentally broken in early versions of Android:
 * http://code.google.com/p/android/issues/detail?id=20106
 * @author john
 *
 */
public class PingActivity extends Activity implements OnItemSelectedListener {
	
	private TextView pingTextView;
	private Button pingButton;
	private EditText pingEditText;
	private Spinner spinner;
	private List<NetworkInterface>  interfaces;
	private NetworkInterface selectedInterface;
	private List<PingWorker> pingList;
	
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			pingList.add((PingWorker) msg.obj);
			
			StringBuilder sb = new StringBuilder();
			
			for (PingWorker ping : pingList) {
				if (ping.isComplete()) {
					sb.append(ping.getResult());
					sb.append("\n");
				}
			}
			
			pingTextView.setText(sb.toString());
		}
		
	};
	
	{
		interfaces = new ArrayList<NetworkInterface>();
		selectedInterface = null;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ping);
		
		pingTextView = (TextView) findViewById(R.id.pingTextView);
		pingButton = (Button) findViewById(R.id.pingButton);
		pingEditText = (EditText) findViewById(R.id.pingEditText);
		
		
		pingButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				pingList = new ArrayList<PingWorker>();
				PingWorker ping = new PingWorker(pingEditText.getText().toString());
				ping.setNetworkInterface(selectedInterface);
				ping.registerHandlerCallback(handler);
				ping.setCount(5);
				
				Thread thread = new Thread(ping);
				thread.start();
			}
		});
		
		showWelcome();
	}
	
	private void showWelcome() {
		Context context = getApplicationContext();
		CharSequence text = getString(R.string.pingWelcome);
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		final List<String> interfaceTextList = new ArrayList<String> ();
		
		spinner = (Spinner) findViewById(R.id.pingSpinner);
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, interfaceTextList);
		adapter.setDropDownViewResource(R.layout.spinner_custom);
		
		try
		{
			Enumeration<NetworkInterface> ifaceEnumeration = NetworkInterface.getNetworkInterfaces();
			while (ifaceEnumeration.hasMoreElements()) {
				NetworkInterface iface = ifaceEnumeration.nextElement();
				
				Enumeration <InetAddress> iaddressEnumeration = iface.getInetAddresses();
				while (iaddressEnumeration.hasMoreElements()) {
					InetAddress iaddress = iaddressEnumeration.nextElement();
					interfaces.add(iface);
					interfaceTextList.add(iface.getDisplayName() + " (" + iaddress.getHostAddress() + ")");
				}
			}
		}
		catch (SocketException e)
		{
			PipsError.log(e);
		}
		finally
		{
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(this);
		}
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long id) {
		this.selectedInterface = interfaces.get(position);
		PipsError.log(this.selectedInterface.getDisplayName() + " selected.");
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		PipsError.log("Nothing was selected from network interface spinner.");
	}
}