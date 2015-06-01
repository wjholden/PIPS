package com.wjholden.nmap;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;

/**
 * Now that I know about this cool thing called a
 * DigestOutputStream, this class is pretty solid, having the
 * potential to catch errors and all.
 * @author John
 *
 */
class Install implements Runnable {

	private static final int BUFFER_SIZE = 8192;
    private static final String ASSET_DIRECTORY = "nmap-6.47";

	private class InstallerBinary {
		public transient String filename;
		public transient int files[];
		public transient boolean executable;

		public InstallerBinary(final String filename, final int files[],
				final boolean executable) {
			this.filename = filename;
			this.files = files.clone();
			this.executable = executable;
		}
	}

	private final transient String binaryDirectory;
	private final Handler handler;
    private final Context context;

	/**
	 * 
	 * @param context Context of the activity launching this installer.
	 * @param binaryDirectory Location to save binaries.
	 * @param hasRoot Does user have root access or not.
	 */
	public Install(final Context context, final String binaryDirectory,
			final boolean hasRoot, final Handler handler) {
		super();
        this.context = context;
		this.binaryDirectory = binaryDirectory;
		this.handler = handler;
	}

	private void deleteExistingFile(final File myFile) {
		if (myFile.exists()) {
			if (myFile.delete()) {
				PipsError.log("Deleted existing " + myFile.getAbsolutePath());
			} else {
				PipsError.log("Unable to delete existing " + myFile.getAbsolutePath());
			}
		} else {
            PipsError.log(myFile.getAbsolutePath() + " does not exist.");
        }
	}

	public void run () {
        AssetManager assetManager = context.getAssets();
        try {
            PipsError.log(Thread.currentThread().getName());
            Message.obtain(handler, Constants.PROGRESS_DIALOG_START,
                    (Object) "Installing Nmap binaries...").sendToTarget();

            String files[] = assetManager.list(ASSET_DIRECTORY);
            PipsError.log("Found " + files.length + " assets.");

            for (String s : files) {
                Message.obtain(handler,
                        Constants.PROGRESS_DIALOG_CHANGE_TEXT,
                        "Installing " + s).sendToTarget();

                File file = new File(binaryDirectory, s);
                deleteExistingFile(file);

                InputStream inputStream = assetManager.open(ASSET_DIRECTORY + "/" + s);

                // http://csrc.nist.gov/publications/nistpubs/800-131A/sp800-131A.pdf
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                DigestOutputStream digestOutputStream = new DigestOutputStream(
                        new FileOutputStream(file), sha256);

                // http://stackoverflow.com/a/4530294
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digestOutputStream.write(buffer, 0, read);
                }

                inputStream.close();
                digestOutputStream.close();

                if (s.equals("nmap")) {
                    file.setExecutable(true);
                }

                PipsError.log("Installed " + file.getAbsolutePath() +
                        " (SHA-256 checksum = " + getHash(sha256) + ").");
            }

            handler.sendEmptyMessage(Constants.INSTALL_COMPLETE);
        } catch (IOException e) {
            PipsError.log(e);
            Message.obtain(handler, Constants.INSTALL_ERROR, e.toString()).sendToTarget();
        } catch (NoSuchAlgorithmException e) {
            PipsError.log(e);
            Message.obtain(handler, Constants.INSTALL_ERROR, e.toString()).sendToTarget();
        } finally {
            handler.sendEmptyMessage(Constants.PROGRESS_DIALOG_DISMISS);
        }
	}

	private String getHash (MessageDigest digest) {
		StringBuffer hexString = new StringBuffer();
		byte[] hash = digest.digest();

		for (int i = 0; i < hash.length; i++) {
			if ((0xff & hash[i]) < 0x10) {
				hexString.append("0" + Integer.toHexString((0xFF & hash[i])));
			} else {
				hexString.append(Integer.toHexString(0xFF & hash[i]));
			}
		}

		return hexString.toString();
	}
}
