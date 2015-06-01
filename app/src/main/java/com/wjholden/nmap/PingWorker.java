package com.wjholden.nmap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Date;

import android.os.Handler;
import android.os.Message;

class PingWorker implements Runnable, Comparable<PingWorker> {
	
	private int timeout = 2000;
	private int ttl = 127;
	private int count = 1;
	
	private InetAddress ip;
	private boolean reachable;
	private Date timeStarted;
	private Date timeCompleted;
	private NetworkInterface iface;
	private long roundTripTime;
	private Handler callbackHandler;
	private IOException networkError;
	private String host = null;
	
	public PingWorker () {
		this.reachable = false;
	}
	
	public PingWorker (final String host) {
		this();
		this.host = host;
	}
	
	public void registerHandlerCallback (Handler handler) {
		callbackHandler = handler;
	}
	
	public void setNetworkInterface(NetworkInterface networkInterface) {
		this.iface = networkInterface;
	}

	public void run() {
		timeStarted = new Date();
		try {
			if (this.host == null) {
				this.ip = InetAddress.getLocalHost();
			} else {
				this.ip = InetAddress.getByName(host);
			}
			
			if (this.iface != null) {
				PipsError.log("Pinging " + ip.getHostAddress() + " from " + iface.getDisplayName() + "...");
				reachable = ip.isReachable(iface, ttl, timeout);
			} else {
				PipsError.log("Pinging " + ip.getHostAddress() + "...");
				reachable = ip.isReachable(timeout);
			}
		} catch (IOException e) {
			networkError = e;
			reachable = false;
		} finally {
			timeCompleted = new Date();
			roundTripTime = timeCompleted.getTime() - timeStarted.getTime();
			Message msg = Message.obtain(callbackHandler);
			msg.obj = this;
			msg.sendToTarget();
			
			// Never done this kind of recursion before. This thread launches a new thread
			// recursively until count reaches zero.
			--count;
			if (count > 0) {
				PingWorker ping;
				if (host != null) {
					ping = new PingWorker(host);
				} else {
					ping = new PingWorker();
				}
				ping.setNetworkInterface(this.iface);
				ping.registerHandlerCallback(this.callbackHandler);
				ping.setCount(this.count);
				ping.setTimeout(this.timeout);
				ping.setTTL(this.ttl);
				
				Thread thread = new Thread(ping);
				thread.start();
			}
		}
	}
	
	public String getResult() {
		String result;
		
		if (networkError == null && reachable) {
			result = "Reply from " + ip.getHostAddress() + ": time=" + roundTripTime + " TTL=" + ttl;
		} else if (networkError == null & !reachable) {
			result = ip.getHostAddress() + " is not reachable. time=" + roundTripTime + " TTL=" + ttl;
		} else {
			result = networkError.getMessage();
		}
		
		PipsError.log(result);
		return result;
	}
	
	public void setTTL(int ttl) {
		this.ttl = ttl;
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public void setCount (int count) {
		this.count = count;
	}
	
	public boolean isComplete () {
		return (this.timeCompleted != null);
	}
	
	public Date getTimeCompleted() {
		return this.timeCompleted;
	}

	public int compareTo(PingWorker ping) {
		return this.timeCompleted.compareTo(ping.getTimeCompleted());
	}
	
}
