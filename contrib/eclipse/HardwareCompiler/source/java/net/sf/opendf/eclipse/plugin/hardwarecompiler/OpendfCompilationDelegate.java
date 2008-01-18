/* 
BEGINCOPYRIGHT X
  
  Copyright (c) 2007, Xilinx Inc.
  All rights reserved.
  
  Redistribution and use in source and binary forms, 
  with or without modification, are permitted provided 
  that the following conditions are met:
  - Redistributions of source code must retain the above 
    copyright notice, this list of conditions and the 
    following disclaimer.
  - Redistributions in binary form must reproduce the 
    above copyright notice, this list of conditions and 
    the following disclaimer in the documentation and/or 
    other materials provided with the distribution.
  - Neither the name of the copyright holder nor the names 
    of its contributors may be used to endorse or promote 
    products derived from this software without specific 
    prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  
ENDCOPYRIGHT
 */
package net.sf.opendf.eclipse.plugin.hardwarecompiler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import net.sf.opendf.config.ConfigBoolean;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.ConfigString;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import org.eclipse.ui.console.*;
import net.sf.opendf.eclipse.plugin.*;
import net.sf.opendf.util.exception.ReportingExceptionHandler;
import net.sf.opendf.util.logging.BasicLogFormatter;
import net.sf.opendf.util.logging.FlushedStreamHandler;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;

import com.xilinx.systembuilder.cli_private.Synthesizer;

import java.util.*;
import java.util.logging.*;
import java.lang.Runnable;

/**
 * Front end for HDL compilation of network.  Does not yet support compilation of CAL directly 
 * (must be instantiated in a network).
 * TODO: Handle exceptions with graceful reporting.
 * 
 * @author imiller
 *
 */
public class OpendfCompilationDelegate implements ILaunchConfigurationDelegate
{

    private MessageConsoleStream status; // Launch progress
    private MessageConsoleStream error; // Simulator error messages
    private MessageConsoleStream info; // Simulator info messages
    private MessageConsoleStream output; // Simulation output

    private Handler errorHandler;
    private Handler infoHandler;
    private Handler outputHandler;

    private MessageConsole statusConsole;

    public void launch (ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException
    {
        final int MONITOR_STEPS = 1000; 

        ConfigGroup synthConfigs = new ConfigGroup();
        // Update the configs with the user settings.  Assume that any values in the
        // configuration have been set by the user.
        synthConfigs.updateConfig(new ConfigUpdateWrapper(configuration), synthConfigs.getConfigs().keySet());
        
        attachConsole(synthConfigs);
        
        monitor.beginTask("Dataflow HDL Compilation", MONITOR_STEPS);
        status.println("Starting dataflow HDL compiler");

        monitor.setTaskName("Setup");

        Synthesizer synth = new Synthesizer(synthConfigs);

        monitor.worked(1);
        monitor.setTaskName("Elaboration");
        status.println("Elaborating ...");
        try{
            synth.synthElaborate();
        }catch (Exception e){
            status.println("Error Elaborating Network");
            (new ReportingExceptionHandler()).process(e);
            detachConsole();
            monitor.done();
            launch.terminate();
            return;
            //throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL Compilation Elaboration failed", e));
        }
        monitor.worked(99);
        if (monitor.isCanceled())
        {
            cancel(monitor);
            return;
        }

        /*
         * if( ! simulator.elaborate() ) { error.println( "Elaboration failed" );
         * status.println("Closing simulator"); detachConsole(); monitor.done();
         * return; }
         */
        monitor.setTaskName("Top level VHDL Generation");
        status.println("Generating top level VHDL ...");
        try {
            synth.generateNetworkHDL();
        }catch (Exception e) {
            status.println("Error generating HDL Network");
            (new ReportingExceptionHandler()).process(e);
            detachConsole();
            monitor.done();
            launch.terminate();
            return;
            //e.printStackTrace();
            //detachConsole();
            //throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL top level VDHL generation failed", e));        
        }
        monitor.worked(100);
        if (monitor.isCanceled())
        {
            cancel(monitor);
            return;
        }
        
        monitor.setTaskName("Compilation");
        status.println("Compiling " + synth.remainingInstances() + " instances ...");

        final int instanceWorkUnit = 800 / synth.remainingInstances();  
        // int result;

        try
        {
            boolean remaining = synth.generateNextInstanceHDL();
            while (remaining)
            {
                remaining = synth.generateNextInstanceHDL();
                monitor.worked(instanceWorkUnit);

                // result = simulator.advanceSimulation( 5000 );

                if (monitor.isCanceled())
                {
                    cancel(monitor);
                    return;
                }
            }
        } catch (Exception e){
            status.println("Error Generating HDL Instances");
            (new ReportingExceptionHandler()).process(e);
            detachConsole();
            monitor.done();
            launch.terminate();
            return;
            //throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL instance HDL generation failed", e));
        }

        status.println("Compilation complete");
        detachConsole();
        monitor.done();
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

    private Level origUserLevel=Logging.user().getLevel();
    private Level origSimLevel=Logging.simout().getLevel();
    private Level origDbgLevel=Logging.dbg().getLevel();
    
    private void attachConsole (ConfigGroup configs)
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

    private void cancel (IProgressMonitor monitor)
    {
        error.println("Cancellation requested");
        status.print("Canceled.  Closing compiler");
        detachConsole();
        monitor.done();
    }
    
    private void detachConsole ()
    {
        Logging.dbg().removeHandler(errorHandler);
        Logging.user().removeHandler(infoHandler);
        Logging.simout().removeHandler(outputHandler);
        
        Logging.user().setLevel(this.origUserLevel);
        Logging.simout().setLevel(this.origSimLevel);
        Logging.dbg().setLevel(this.origDbgLevel);
    }

}
