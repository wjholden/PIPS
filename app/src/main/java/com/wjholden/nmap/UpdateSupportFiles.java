package com.wjholden.nmap;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;

/**
 * This class will download and install the support files
 * used by Nmap.
 * @author John
 *
 */
public class UpdateSupportFiles implements Runnable {
	
	private String urls[] = {
		"https://svn.nmap.org/nmap/nmap-mac-prefixes",
		"https://svn.nmap.org/nmap/nmap-os-db",
		"https://svn.nmap.org/nmap/nmap-payloads",
		"https://svn.nmap.org/nmap/nmap-protocols",
		"https://svn.nmap.org/nmap/nmap-rpc",
		"https://svn.nmap.org/nmap/nmap-service-probes",
		"https://svn.nmap.org/nmap/nmap-services"
	};
	
	private int numSuccessful;
	private String binaryDirectory;
	private List<String> errors;
	private Handler handler;

	private static final int BUFFER_SIZE = 8192;
	
	/**
	 * This variant of the constructor allows the caller to specify what
	 * directory to write the support files into.
	 * @param binaryDirectory Location to write downloaded files.
	 */
	public UpdateSupportFiles (String binaryDirectory, Handler handler) {
		this.handler = handler;
		numSuccessful = 0;
		errors = new ArrayList<String>();
		this.binaryDirectory = binaryDirectory;
	}

	public void run() {
		int i = 0;
		for (String s : urls) {
			PipsError.log("Downloading " + s);
			
			try {
				download(s);
				numSuccessful++;
				
				Message msg = handler.obtainMessage(Constants.UPDATE_IN_PROGRESS_STRING);
				String f = s.substring(s.lastIndexOf('/') + 1, s.length());
				msg.obj = f;
				msg.sendToTarget();
			} catch (MalformedURLException e) {
				PipsError.log(e);
				errors.add(e.getMessage());
			} catch (IOException e) {
				PipsError.log(e);
				errors.add(e.getMessage());
			} catch (NoSuchAlgorithmException e) {
				PipsError.log(e);
				errors.add(e.getMessage());
				break; // This should never happen.
			} finally {
				Message msg = handler.obtainMessage(Constants.UPDATE_IN_PROGRESS);
				msg.arg1 = ++i;
				msg.sendToTarget();
			}
		}
		
		// QC
		if (urls.length != numSuccessful + errors.size()) {
			PipsError.log("QC Warning: a strange condition has occurred.");
			String strangeError = "The program may not have attempted each update file.";
			PipsError.log(strangeError);
			errors.add(strangeError);
		}
		
		if (errors.size() == 0) {
			handler.sendEmptyMessage(Constants.UPDATE_COMPLETE_NO_ERRORS);
		} else {
			Message message = Message.obtain(handler, Constants.UPDATE_COMPLETE_WITH_ERRORS, errors);
			message.sendToTarget();
		}
		
		
	}
	
	private void download(String url) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		URL u = new URL(url);
        URLConnection con = u.openConnection();
        File file = new File(binaryDirectory + url.substring(url.lastIndexOf('/') + 1, url.length()));
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                PipsError.log("Deleted " + file);
            } else {
                PipsError.log(file + "exists, but could not be deleted.");
            }
        }
        InputStream inputStream = con.getInputStream();
		DigestOutputStream digestOutputStream = new DigestOutputStream(
				new FileOutputStream(file), md);

		byte[] buffer = new byte[BUFFER_SIZE];
		int read;
		while ((read = inputStream.read(buffer)) != -1) {
			digestOutputStream.write(buffer, 0, read);
		}
        digestOutputStream.close();
        inputStream.close();
		
		// Looks like we got it all. Let's see if it was right:
		String signature = new BigInteger(1, md.digest()).toString(16);
		while (signature.length() < 64) {
			signature = "0" + signature;
		}
		PipsError.log("Downloaded " + file + " (SHA-256: " + signature + ").");
	}
	
	public int getNumberOfFiles() {
		return this.urls.length;
	}

}
