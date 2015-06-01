package com.wjholden.nmap;

import java.io.Serializable;

// code borrowed largely from
// http://www.vogella.de/articles/AndroidSQLite/article.html
public class ScanResult implements Serializable {

	private static final long serialVersionUID = -2069537600693440379L;

	private long identifier;
	private String result;
	private String target;
	private String date;
	
	public ScanResult(final long identifier, final String result, final String target, final String date)
	{
		super();
		this.identifier = identifier;
		this.result = result;
		this.target = target;
		this.date = date;
	}

	public long getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final long identifier) {
		this.identifier = identifier;
	}

	public String getResult() {
		return result;
	}

	public void setResult(final String result) {
		this.result = result;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(final String target) {
		this.target = target;
	}

	public String getDate() {
		return date;
	}

	public void setDate(final String date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return "Date: " + date + "\nTarget: " + target;
	}
}