package net.sf.opendf.eclipse.plugin.config;

import java.util.logging.Handler;
import java.util.logging.Level;

import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.ConfigString;
import net.sf.opendf.eclipse.plugin.OpendfPlugin;
import net.sf.opendf.util.logging.BasicLogFormatter;
import net.sf.opendf.util.logging.FlushedStreamHandler;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Abstract class implementing base launch delegate functionality
 * 
 * @version 18 March 2009
 * 
 */
public abstract class OpendfConfigLaunchDelegate extends
		LaunchConfigurationDelegate {
	private MessageConsoleStream status; // Launch progress
	private MessageConsoleStream error; // Simulator error messages
	private MessageConsoleStream info; // Simulator info messages
	private MessageConsoleStream output; // Simulation output

	private Handler infoHandler;
	private Handler outputHandler;
	private Handler dbgHandler;

	private Level origUserLevel = Logging.user().getLevel();
	private Level origSimLevel = Logging.simout().getLevel();
	private Level origDbgLevel = Logging.dbg().getLevel();

	protected MessageConsoleStream status() {
		return this.status;
	}

	protected MessageConsoleStream error() {
		return this.error;
	}

	protected void attachConsole(String consolePrefix, ConfigGroup configs) {
		// First update the loggers with the specified level.
		this.origUserLevel = Logging.user().getLevel();
		this.origSimLevel = Logging.user().getLevel();
		this.origDbgLevel = Logging.user().getLevel();
		if (configs.get(ConfigGroup.LOG_LEVEL_USER).isUserSpecified())
			Logging.user().setLevel(Level.parse(((ConfigString) configs.get(ConfigGroup.LOG_LEVEL_USER)).getValue()));
		if (configs.get(ConfigGroup.LOG_LEVEL_SIM).isUserSpecified())
			Logging.simout().setLevel(Level.parse(((ConfigString) configs.get(ConfigGroup.LOG_LEVEL_SIM)).getValue()));
		if (configs.get(ConfigGroup.LOG_LEVEL_DBG).isUserSpecified())
			Logging.dbg().setLevel(Level.parse(((ConfigString) configs.get(ConfigGroup.LOG_LEVEL_DBG)).getValue()));

		final MessageConsole outputConsole = findOrCreateConsole(consolePrefix + " Output");
		final MessageConsole statusConsole = findOrCreateConsole(consolePrefix + " Status");
		final MessageConsole dbgConsole = Logging.dbg().isLoggable(Level.INFO) ? findOrCreateConsole(consolePrefix + " debug") : statusConsole;

		status = statusConsole.newMessageStream();
		error = statusConsole.newMessageStream();
		info = statusConsole.newMessageStream();
		output = outputConsole.newMessageStream();
		final MessageConsoleStream dbg = dbgConsole.newMessageStream();

		// Now do some work in the UI thread
		Display display = OpendfPlugin.getDefault().getWorkbench().getDisplay();

		display.syncExec(new Runnable() {
			public void run() {
				IWorkbench bench = OpendfPlugin.getDefault().getWorkbench();
				Color red = bench.getDisplay().getSystemColor(SWT.COLOR_RED);
				Color blue = bench.getDisplay().getSystemColor(SWT.COLOR_BLUE);

				status.setColor(blue);
				status.setFontStyle(SWT.BOLD);

				error.setColor(red);
				// If we are debugging to the user status console, use red
				if (dbgConsole == statusConsole)
					dbg.setColor(red);

				// make the status console visible
				try {
					IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
					IWorkbenchPage page = win.getActivePage();
					IConsoleView view = (IConsoleView) page
							.showView(IConsoleConstants.ID_CONSOLE_VIEW);
					view.display(statusConsole);
				} catch (Exception e) {
					OpendfPlugin.logErrorMessage("Failed to make console visible", e);
				}
			}
		});

		dbgHandler = new FlushedStreamHandler(dbg, new BasicLogFormatter());
		infoHandler = new FlushedStreamHandler(info, new BasicLogFormatter());
		outputHandler = new FlushedStreamHandler(output, new BasicLogFormatter());

		Logging.removeDefaultHandler(Logging.dbg());
		Logging.removeDefaultHandler(Logging.user());
		Logging.removeDefaultHandler(Logging.simout());

		Logging.dbg().addHandler(dbgHandler);
		Logging.user().addHandler(infoHandler);
		Logging.simout().addHandler(outputHandler);

	}

	protected void detachConsole() {
		Logging.dbg().removeHandler(dbgHandler);
		Logging.user().removeHandler(infoHandler);
		Logging.simout().removeHandler(outputHandler);

		Logging.user().setLevel(this.origUserLevel);
		Logging.simout().setLevel(this.origSimLevel);
		Logging.dbg().setLevel(this.origDbgLevel);
	}

	private static MessageConsole findOrCreateConsole(String name) {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole existing[] = manager.getConsoles();

		for (int i = 0; i < existing.length; i++) {
			if (name.equals(existing[i].getName())) {
				return (MessageConsole) existing[i];
			}
		}

		MessageConsole console = new MessageConsole(name, null);
		manager.addConsoles(new IConsole[] { console });

		return console;
	}

}
