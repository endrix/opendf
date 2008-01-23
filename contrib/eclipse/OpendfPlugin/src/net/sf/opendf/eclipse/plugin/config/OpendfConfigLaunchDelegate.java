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

    private Handler infoHandler;
    private Handler outputHandler;
    private Handler dbgHandler;


    private Level origUserLevel=Logging.user().getLevel();
    private Level origSimLevel=Logging.simout().getLevel();
    private Level origDbgLevel=Logging.dbg().getLevel();
    
    protected MessageConsoleStream status () { return this.status; }
    protected MessageConsoleStream error () { return this.error; }
    
    protected void attachConsole (ConfigGroup configs)
    {
        // First update the loggers with the specified level.
        this.origUserLevel = Logging.user().getLevel();
        this.origSimLevel = Logging.user().getLevel();
        this.origDbgLevel = Logging.user().getLevel();
        if (configs.get(ConfigGroup.LOG_LEVEL_USER).isUserSpecified())
            Logging.user().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_USER)).getValue()));
        if (configs.get(ConfigGroup.LOG_LEVEL_SIM).isUserSpecified())
            Logging.simout().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_SIM)).getValue()));
        if (configs.get(ConfigGroup.LOG_LEVEL_DBG).isUserSpecified())
            Logging.dbg().setLevel(Level.parse(((ConfigString)configs.get(ConfigGroup.LOG_LEVEL_DBG)).getValue()));

        final MessageConsole outputConsole = findOrCreateConsole("Compilation Output");
        final MessageConsole statusConsole = findOrCreateConsole("Compilation Status");
        final MessageConsole dbgConsole = Logging.dbg().isLoggable(Level.INFO) ? findOrCreateConsole("Compilation debug"):statusConsole; 
            
        status = statusConsole.newMessageStream();
        error = statusConsole.newMessageStream();
        info = statusConsole.newMessageStream();
        output = outputConsole.newMessageStream();
        final MessageConsoleStream dbg = dbgConsole.newMessageStream();

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
                // If we are debugging to the user status console, use red
                if (dbgConsole == statusConsole)
                    dbg.setColor(red);

                // make the status console visible
                try
                {
                    IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
                    IWorkbenchPage page = win.getActivePage();
                    IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
                    view.display(statusConsole);
                } catch (Exception e)
                {
                    OpendfPlugin.logErrorMessage(
                            "Failed to make console visible", e);
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

    protected void detachConsole ()
    {
        Logging.dbg().removeHandler(dbgHandler);
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
