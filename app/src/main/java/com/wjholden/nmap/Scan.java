package com.wjholden.nmap;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * The general workflow for this program is:<ol>
 * <li>User initiates scan from PIPSActivity, which creates a thread of type Scan.</li>
 * <li>The Scan thread sends messages to PIPSActivity.handler with errors or success, and includes output in Message.obj</li>
 * <li>TODO</li></ol>
 * <br /><b>Now implements Runnable</b> instead of extends Thread.
 */
class Scan implements Runnable {

	private final transient String binaryDirectory;
	private final transient boolean hasRoot;
	private final transient boolean saveHistory;
	private final transient String target;
	private final transient String arguments;
	private final transient int help;
	private final transient String shell;
	private final transient PipsDatabase db;
	private final transient Handler handler;

	/**
	 * Thread that handles the actual scanning. Yes, it's a little more
	 * intimidating than I had wanted when I first started this rewrite, but
	 * it's not nearly as bad as what I had before.
	 * 
	 * @param binaryDirectory
	 *            Directory the binaries will be stored. See Utilities class.
	 * @param hasRoot
	 *            Whether the user has access to root or not. See Utilities
	 *            class.
	 * @param target
	 *            Target the user wants to scan.
	 * @param arguments
	 *            Arguments the user wants. The program will probably break if
	 *            the user specifies -oA.
	 * @param help
	 *            Whether or not you want -h command-line help. 1 for help, 0 for normal output.
	 * @param context
	 *            Context the activity is running from (required to save results to database).
	 *            @param saveHistory
	 *            If true, history will be saved in the database.
	 */
	Scan(final String binaryDirectory, final boolean hasRoot,
			final String target, final String arguments, final int help, final Context context,
			final boolean saveHistory, final Handler handler) {
		super();
		this.binaryDirectory = binaryDirectory;
		this.hasRoot = hasRoot;
		this.target = target;
		this.arguments = arguments;
		this.help = help;
		this.saveHistory = saveHistory;
		this.handler = handler;

		this.shell = hasRoot ? "su" : "sh";
		this.db = new PipsDatabase(context, handler);
		Thread dbThread = new Thread(db);
		dbThread.run();
	}

	public synchronized void run() {
		String line;
		Process process = null;
		DataOutputStream outputStream = null;
		BufferedReader inputStream, errorStream;
		inputStream = errorStream = null;
		StringBuilder errorOutput, normalOutput, command;
		
		PipsError.log(Thread.currentThread().getName());

		Message.obtain(handler, Constants.PROGRESS_DIALOG_START,
				"Scanning...").sendToTarget();
		
		command = new StringBuilder(binaryDirectory);
		command.append("nmap ").append(arguments).append(' ').append(target);

		if (!arguments.contains("privileged")) {
			if (hasRoot)
			{
				command.append(" --privileged ");
			}
			else
			{
				command.append(" --unprivileged ");
			}
		}

		if (help == 1 && !arguments.contains("-h")) {
			command.append(" -h ");
		}

		try {
			process = Runtime.getRuntime().exec(shell);
			if (process == null) {
				throw new Utilities.NullProcessException(); // this condition is very unlikely
			} else {
				PipsError.log("Started shell process (" + shell + ").");
			}

			outputStream = new DataOutputStream(process.getOutputStream());
			inputStream = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			errorStream = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));

			outputStream.writeBytes(command.toString() + "\n");
			PipsError.log(command.toString());
			outputStream.flush();
			outputStream.writeBytes("exit\n");
			outputStream.flush();

			normalOutput = new StringBuilder();
			while ((line = inputStream.readLine()) != null) {
				normalOutput.append(line).append((char) '\n');
				PipsError.log(line);
			}
			if (normalOutput.length() > 0) {
				if (saveHistory)
				{
					db.insert(normalOutput.toString(), target);
				}
				Message.obtain(handler, Constants.SCAN_COMPLETE,
						help, 0, normalOutput.toString()).sendToTarget();
			} else if (normalOutput.length() <= 0) {
				PipsError.log("Ran nmap but received no input from stdout.");
			}

			// Catch errors last. Errors are reported to user as Dialog,
			// and they might need to know what's on both stdout and stderr,
			// show both.
			errorOutput = new StringBuilder();
			while ((line = errorStream.readLine()) != null) {
				errorOutput.append(line).append((char) '\n');
				PipsError.log(line);
			}
			if (errorOutput.length() > 2) { // newline above counts as 1
				throw new Utilities.StandardErrorNotEmptyException(
						errorOutput.toString());
			}

		} catch (IOException e) {
			Message.obtain(handler,
					Constants.SCAN_ERROR_IOEXCEPTION, (String) e.toString())
					.sendToTarget();
		} catch (Utilities.NullProcessException e) {
			Message.obtain(handler,
					Constants.SCAN_ERROR_NULL_PROCESS).sendToTarget();
		} catch (Utilities.StandardErrorNotEmptyException e) {
			Message.obtain(handler,
					Constants.SCAN_ERROR_STANDARD_ERROR, e.getMessage())
					.sendToTarget();
		} finally {
			Message.obtain(handler,
					Constants.PROGRESS_DIALOG_DISMISS).sendToTarget();
			if (process != null) {
				process.destroy();
			}

			// I don't like nested try/catch statements, but it really needs to
			// be this way to make sure
			// everything gets closed correctly.
			try {
				if (outputStream != null) {
					outputStream.close();
				} else {
					PipsError.log("Cannot close null outputStream.");
				}
			} catch (IOException e) {
				PipsError.log("Unable to close outputStream: ");
				PipsError.log(e);
			}
			try {
				if (errorStream != null) {
					errorStream.close();
				} else {
					PipsError.log("Cannot close null errorStream.");
				}
			} catch (IOException e) {
				PipsError.log("Unable to close errorStream: ");
				PipsError.log(e);
			}
			db.close();
		}
	}
}
