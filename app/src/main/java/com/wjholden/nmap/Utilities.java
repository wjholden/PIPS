package com.wjholden.nmap;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

class Utilities {

	static File su;

	private Utilities() {
	}

	public static String getApplicationFolder(final Context context,
			final String subfolder) {
		File appDir = new File(getDataDirectory(context) + "/" + subfolder);

		try {
			// if it doesn't exist, create it
			if (appDir.exists()) {
				PipsError.log(appDir.getAbsolutePath() + " exists.");
			} else {
				PipsError.log(appDir.getAbsolutePath() + " does not exist.");
				if (appDir.mkdirs()) {
					PipsError.log("Created " + appDir.getAbsolutePath());
				} else {
					throw new CannotCreateDirectoryException(
							"Failed to create " + appDir.getAbsolutePath(),
							appDir);
				}
			}
		} catch (CannotCreateDirectoryException e) {
			PipsError.log(e.toString());
			appDir = new File("/tmp/");
		}

		return appDir.getAbsolutePath() + "/";
	}

	private static String getDataDirectory(final Context context) {
		String dataDirectory = "";
		try {
			dataDirectory = context.getPackageManager().getApplicationInfo(
					"com.wjholden.nmap", 0).dataDir;
		} catch (NameNotFoundException e) {
			// this wouldn't be good, but it is very unlikely to happen.
			PipsError.log(e.toString());
		}
		return dataDirectory;
	}

	/**
	 * Tests if the handset has a "su" command available in
	 * the various locations specified in $PATH. If API 9 or greater,
	 * also tests if that the su command is executable or not.
	 * 
	 * @return
	 */
	public static boolean canGetRoot() {
		String path = System.getenv("PATH");
		StringTokenizer st = new StringTokenizer(path, ":");
		boolean suFound = false;
		try {
			while (st.hasMoreTokens()) {
				File suTest = new File(st.nextToken() + "/su");
				
				// File.canExecute() is a feature only available to
				// SDK 9 and above. This code should fix that compatibility
				// problem.
				if (android.os.Build.VERSION.SDK_INT >= 9)
					suFound = suTest.exists() && suTest.canExecute();
				else
					suFound = suTest.exists();
				
				if (suFound) {
					Utilities.su = suTest;
					break; // stop looking now that you have found it
				}
			}
		} catch (NoSuchMethodError e) {
			PipsError.log(e);
		}
		return suFound;
	}

	/**
	 * Custom exception for handling errors where
	 * Runtime.getRuntime().exec(String) returned null.
	 * 
	 * @author William John Holden
	 * @version 1
	 */
	static class NullProcessException extends Exception {

		private static final long serialVersionUID = -6606740982523502676L;

		/**
		 * Use if Runtime.getRuntime().exec(String) returns null.
		 */
		public NullProcessException() {
			super();
		}
	}

	/**
	 * Custom exception extending IOException. This is useful because
	 * File.mkdirs() does not throw an IOException.
	 * 
	 * @author William John Holden
	 * @version 1
	 */
	static class CannotCreateDirectoryException extends IOException {

		private static final long serialVersionUID = -7566027000405830050L;

		public File directory;

		/**
		 * If you find File.mkdirs() returns false, throw this exception.
		 * 
		 * @param detailMessage
		 *            Absolute path of the directory you attempted to create.
		 */
		public CannotCreateDirectoryException(final String detailMessage,
				final File directory) {
			super(detailMessage);
			this.directory = directory;
		}
	}

	/**
	 * Custom exception for handling case where Nmap executable returned
	 * standard error.
	 * 
	 * @author William John Holden
	 * 
	 */
	static class StandardErrorNotEmptyException extends Exception {

		private static final long serialVersionUID = 866136689299038573L;

		/**
		 * Under normal circumstances there should be nothing on the standard
		 * error stream after running Nmap. If there is, you need to throw this
		 * exception.
		 * 
		 * @param detailMessage
		 *            Everything read from the standard error stream.
		 */
		public StandardErrorNotEmptyException(final String detailMessage) {
			super(detailMessage);
		}
	}

	private static class UtilitiesSingletonHolder {
		private static final Utilities INSTANCE = new Utilities();
	}

	/**
	 * Lazy initialization, how cool is that?
	 * 
	 * @return
	 */
	public static Utilities getInstance() {
		return UtilitiesSingletonHolder.INSTANCE;
	}
}
