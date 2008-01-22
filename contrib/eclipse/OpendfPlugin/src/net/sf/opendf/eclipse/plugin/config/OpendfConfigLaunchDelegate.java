package net.sf.opendf.eclipse.plugin.config;

import net.sf.opendf.config.*;
import net.sf.opendf.eclipse.plugin.OpendfPlugin;
import net.sf.opendf.util.logging.*;

import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.*;
import java.util.logging.*;

public abstract class OpendfConfigLaunchDelegate implements ILaunchConfigurationDelegate
{
    private MessageConsoleStream status; // Launch progress
    private MessageConsoleStream error; // Simulator error messages
    private MessageConsoleStream info; // Simulator info messages
    private MessageConsoleStream output; // Simulation output

    private Handler errorHandler;
    private Handler infoHandler;
    private Handler outputHandler;

    private MessageConsole statusConsole;

    private Level origUserLevel=Logging.user().getLevel();
    private Level origSimLevel=Logging.simout().getLevel();
    private Level origDbgLevel=Logging.dbg().getLevel();
    
    protected MessageConsoleStream status () { return this.status; }
    protected MessageConsoleStream error () { return this.error; }
    
    protected void attachConsole (ConfigGroup configs)
    {
        MessageConsole outputConsole = findOrCreateConsole("Compilation Output");
        statusConsole = findOrCreateConsole("Compilation Status");

        status = statusConsole.newMessageStream();
        error = statusConsole.newMessageStream();
        info = statusConsole.newMessageStream();
        output = outputConsole.newMessageStream();

        // Now do some work in the UI thread
        Display display = OpendfPlugin.getDefault().getWorkbench().getDisplay();

        display.syncExec(new Runnable() {
            public void run ()
            {
                IWorkbench bench = OpendfPlugin.getDefault().getWorkbench();
                Color red = bench.getDisplay().getSystemColor(SWT.COLOR_RED);
                Color blue = bench.getDisplay().getSystemColor(SWT.COLOR_BLUE);

                status.setColor(blue);
                status.setFontStyle(SWT.BOLD);

                error.setColor(red);

                // make the status console visible
                try
                {
                    IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
                    IWorkbenchPage page = win.getActivePage();
                    IConsoleView view = (IConsoleView) page
                            .showView(IConsoleConstants.ID_CONSOLE_VIEW);
                    view.display(statusConsole);
                } catch (Exception e)
                {
                    OpendfPlugin.logErrorMessage(
                            "Failed to make console visible", e);
                }
            }
        });

        errorHandler = new FlushedStreamHandler(error, new BasicLogFormatter());
        infoHandler = new FlushedStreamHandler(info, new BasicLogFormatter());
        outputHandler = new FlushedStreamHandler(output,
                new BasicLogFormatter());

        Logging.dbg().addHandler(errorHandler);
        Logging.user().addHandler(infoHandler);
        Logging.simout().addHandler(outputHandler);
        
        //Logging.dbg().setLevel(Level.ALL);
        if (configs.get(ConfigGroup.LOG_LEVEL_USER).isUserSpecified())
        {
            this.origUserLevel = Logging.user().getLevel();
            Logging.user().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_USER)).getValue()));
        }
        if (configs.get(ConfigGroup.LOG_LEVEL_SIM).isUserSpecified())
        {
            this.origSimLevel = Logging.user().getLevel();
            Logging.simout().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_SIM)).getValue()));
        }
        if (configs.get(ConfigGroup.LOG_LEVEL_DBG).isUserSpecified())
        {
            this.origDbgLevel = Logging.user().getLevel();
            Logging.dbg().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_DBG)).getValue()));
        }
    }

    protected void detachConsole ()
    {
        Logging.dbg().removeHandler(errorHandler);
        Logging.user().removeHandler(infoHandler);
        Logging.simout().removeHandler(outputHandler);
        
        Logging.user().setLevel(this.origUserLevel);
        Logging.simout().setLevel(this.origSimLevel);
        Logging.dbg().setLevel(this.origDbgLevel);
    }

    private static MessageConsole findOrCreateConsole (String name)
    {
        IConsoleManager manager = ConsolePlugin.getDefault()
                .getConsoleManager();
        IConsole existing[] = manager.getConsoles();

        for (int i = 0; i < existing.length; i++)
        {
            if (name.equals(existing[i].getName()))
            {
                return (MessageConsole) existing[i];
            }
        }

        MessageConsole console = new MessageConsole(name, null);
        manager.addConsoles(new IConsole[] { console });

        return console;

    }

    
}
