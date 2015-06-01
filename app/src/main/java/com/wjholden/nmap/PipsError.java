package com.wjholden.nmap;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public final class PipsError {
	
	@SuppressWarnings("unused")
	private static final PipsError SINGLETON = new PipsError();
	
	private static boolean debug = false;
	private static List<String> errorLog = new ArrayList<String>();
	
	private PipsError ()
	{
	}
	
	public static void log (final Throwable e)
	{
		Writer stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		errorLog.add(stringWriter.toString());
	}
	
	public static void log (final String string)
	{
		errorLog.add(string);
		if (debug)
		{
			Log.d(Constants.TAG, string);
		}
	}
	
	public static List<String> getLog()
	{
		return errorLog;
	}
	
	public static void setLogcatVisible(final boolean showLogcat) {
		debug = showLogcat;
		if (debug) {
			log("Turned logcat on.");
		} else {
			log("Turned logcat off.");
		}
	}
}
