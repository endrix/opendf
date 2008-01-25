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

import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.SynthesisConfigGroup;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.OpendfConfigLaunchDelegate;

import net.sf.opendf.util.exception.ReportingExceptionHandler;

import com.xilinx.systembuilder.cli_private.Synthesizer;

/**
 * Front end for HDL compilation of network.  Does not yet support compilation of CAL directly 
 * (must be instantiated in a network).
 * TODO: Handle exceptions with graceful reporting.
 * 
 * @author imiller
 *
 */
public class OpendfCompilationDelegate extends OpendfConfigLaunchDelegate
{

    public void launch (ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException
    {
        final int MONITOR_STEPS = 1000;

        ConfigGroup synthConfigs = new SynthesisConfigGroup();
        // Update the configs with the user settings.  Assume that any values in the
        // configuration have been set by the user.
        synthConfigs.updateConfig(new ConfigUpdateWrapper(configuration), synthConfigs.getConfigs().keySet());
        
        attachConsole(synthConfigs);
        
        monitor.beginTask("Dataflow HDL Compilation", MONITOR_STEPS);
        status().println("Starting dataflow HDL compiler");

        monitor.setTaskName("Setup");

        Synthesizer synth = new Synthesizer(synthConfigs);

        monitor.worked(1);
        monitor.setTaskName("Elaboration");
        status().println("Elaborating ...");
        try{
            synth.synthElaborate();
        }catch (Exception e){
            status().println("Error Elaborating Network");
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
         * if( ! simulator.elaborate() ) { error().println( "Elaboration failed" );
         * status().println("Closing simulator"); detachConsole(); monitor.done();
         * return; }
         */
        monitor.setTaskName("Top level VHDL Generation");
        status().println("Generating top level VHDL ...");
        try {
            synth.generateNetworkHDL();
        }catch (Exception e) {
            status().println("Error generating HDL Network");
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
        status().println("Compiling " + synth.remainingInstances() + " instances ...");

        final double instanceWorkUnit = 800.0 / synth.remainingInstances();  
        double worked = 0;
        // int result;

        try
        {
            boolean remaining = synth.generateNextInstanceHDL();
            while (remaining)
            {
                remaining = synth.generateNextInstanceHDL();
                worked += instanceWorkUnit;
                if (worked >= 1.0)
                {
                    int wInt = (int)worked;
                    monitor.worked(wInt);
                    worked = worked - wInt;
                }

                // result = simulator.advanceSimulation( 5000 );

                if (monitor.isCanceled())
                {
                    cancel(monitor);
                    return;
                }
            }
        } catch (Exception e){
            status().println("Error Generating HDL Instances");
            (new ReportingExceptionHandler()).process(e);
            detachConsole();
            monitor.done();
            launch.terminate();
            return;
            //throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "HDL instance HDL generation failed", e));
        }

        status().println("Compilation complete");
        detachConsole();
        monitor.done();
    }

    private void cancel (IProgressMonitor monitor)
    {
        error().println("Cancellation requested");
        status().print("Canceled.  Closing compiler");
        detachConsole();
        monitor.done();
    }
    
}
