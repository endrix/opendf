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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.simulators.tabs.*;
import org.eclipse.ui.console.*;
import net.sf.opendf.eclipse.plugin.*;
import net.sf.opendf.util.logging.BasicLogFormatter;
import net.sf.opendf.util.logging.FlushedStreamHandler;
import net.sf.opendf.util.logging.Logging;

import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;

import com.xilinx.systembuilder.cli_private.PrefMap;
import com.xilinx.systembuilder.cli_private.Synthesizer;

import java.util.*;
import java.util.logging.*;
import java.lang.Runnable;

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
        attachConsole();
        final int MONITOR_STEPS = 1000; 
        monitor.beginTask("Dataflow HDL Compilation", MONITOR_STEPS);
        status.println("Starting dataflow HDL compiler");

        monitor.setTaskName("Setup");

        ConfigGroup synthConfigs = new ConfigGroup();
        // Update the configs with the user settings.  Assume that any values in the
        // configuration have been set by the user.
        synthConfigs.updateConfig(new ConfigUpdateWrapper(configuration));
        
        /*
        PrefMap synthPrefs = new PrefMap();
        synthPrefs.set(PrefMap.OUTPUT_ACTOR_DIR, "Actorx");
        synthPrefs.set(PrefMap.CACHE_PATH, "cache");
        
        Map<String, String> prefCorrelation = new HashMap();
        String modelDirKey = SimulationModelTab.Export(SimulationModelTab.TAB_NAME, SimulationModelTab.KEY_MODELDIR);
        String modelPathKey = SimulationModelTab.Export(SimulationModelTab.TAB_NAME, SimulationModelTab.KEY_MODELSEARCHPATH);        
        String useDirKey = SimulationModelTab.Export(SimulationModelTab.TAB_NAME, SimulationModelTab.KEY_USEDEFAULTPATH);        
        prefCorrelation.put(SimulationModelTab.Export(SimulationModelTab.TAB_NAME, SimulationModelTab.KEY_MODELDIR), PrefMap.MODEL_PATH);
        prefCorrelation.put(SimulationModelTab.Export(SimulationModelTab.TAB_NAME, SimulationModelTab.KEY_MODELFILE), PrefMap.TOP_LEVEL_NAME);

        info.println(prefCorrelation.toString());
        String arg1Prefix = OpendfConfigurationTab.Export("SIM.ARG1", "");
        String arg2Prefix = OpendfConfigurationTab.Export("SIM.ARG2", "");
        int l = arg2Prefix.length();

        String modelDir = "";
        String modelPath = "";
        boolean useDir = true;
        try
        {
            for (Object obj : configuration.getAttributes().keySet())
            {
                String key = (String) obj;
                String value = configuration.getAttribute(key, (String)null);
                if (prefCorrelation.containsKey(key))
                {
                    info.println("Known Key " + key + " value " + value);
                    String prefKey = prefCorrelation.get(key);
                    if (prefKey.equals(PrefMap.TOP_LEVEL_NAME))
                    {
                        String topLevelName = value;
                        if (topLevelName.indexOf('.') >= 0)
                            topLevelName = topLevelName.substring(0, topLevelName.lastIndexOf('.'));
                        synthPrefs.set(PrefMap.TOP_LEVEL_NAME, topLevelName);
                        info.println("Set top level model name to " + topLevelName);
                    }
                    else
                    {
                        synthPrefs.set(prefKey, value);
                        info.println("Set "+prefKey+"name to " + value);                        
                    }
                }
                else if (key.equals(modelDirKey)) { modelDir = value; }
                else if (key.equals(modelPathKey)) { modelPath = value; }
                else if (key.equals(useDirKey)) { useDir = "TRUE".equalsIgnoreCase(value); }
                else
                {
                    info.println("Unknown Key " + key + " value " + value);
                }
            }
            if (useDir)
                synthPrefs.set(PrefMap.MODEL_PATH, modelDir);
            else
                synthPrefs.set(PrefMap.MODEL_PATH, modelPath);
                
        } catch (CoreException e)
        {
            info.println("Exception during argument processing " + e);
        }
        info.println();
        
        Synthesizer synth = new Synthesizer(synthPrefs);
         */
        Synthesizer synth = new Synthesizer(synthConfigs);

        monitor.worked(1);
        monitor.setTaskName("Elaboration");
        status.println("Elaborating ...");
        try{
            synth.synthElaborate();
        }catch (Exception e){
            status.println("Error Elaborating Network");
            detachConsole();
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL Compilation Elaboration failed", e));
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
            detachConsole();
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL top level VDHL generation failed", e));        
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
                /*
                 * if( result != PhasedSimulator.RUNNING ) { if( result ==
                 * PhasedSimulator.FAILED ) { error.println("Simulation failed");
                 * status.println("Closing simulator"); detachConsole();
                 * monitor.done(); return; }
                 * 
                 * break; }
                 */
            }
        } catch (Exception e){
            status.println("Error Generating HDL Instances");
            detachConsole();
            Throwable t = e;
            while (t != null)
            {
                status.println(t.toString());
                t = t.getCause();
            }
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL instance HDL generation failed", e));
        }
        
        detachConsole();
        monitor.done();
        /*
         * if( result == PhasedSimulator.COMPLETED ) status.println("Compiler
         * ran to completion"); else status.println("Compiler reached error
         * limit");
         * 
         * monitor.worked( 1 ); monitor.setTaskName( "Compiler" ); //
         * simulator.cleanup();
         * 
         * status.println("Closing simulator");
         * 
         * detachConsole(); monitor.done();
         */
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

    
    private void attachConsole ()
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
    }

}
