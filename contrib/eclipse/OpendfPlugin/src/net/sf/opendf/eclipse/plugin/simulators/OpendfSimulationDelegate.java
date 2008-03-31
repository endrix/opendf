/* 
BEGINCOPYRIGHT X

  Copyright (c) 2007-2008, Xilinx Inc.
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
package net.sf.opendf.eclipse.plugin.simulators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import net.sf.opendf.cli.PhasedSimulator;
import net.sf.opendf.config.*;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.OpendfConfigLaunchDelegate;

public class OpendfSimulationDelegate extends OpendfConfigLaunchDelegate
{
    private static final String consolePrefix = "Simulation";
    
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor ) throws CoreException 
            {
        ConfigGroup configs = new SimulationConfigGroup();
        // Update all the configs with the user settings.
        configs.updateConfig(new ConfigUpdateWrapper(configuration), configs.getConfigs().keySet());

        attachConsole(consolePrefix, configs);

        monitor.beginTask( "Dataflow Simulation", 5 );
        status().println("Starting dataflow simulator" );

        monitor.setTaskName( "Setup" );

        PhasedSimulator simulator = new PhasedSimulator(configs);

        monitor.worked( 1 );
        monitor.setTaskName( "Elaboration" );
        status().println( "Elaborating ..." );

        if( ! simulator.elaborate() )
        {
            error().println( "Elaboration failed" );
            status().println("Closing simulator");
            detachConsole();
            monitor.done();
            return;
        }

        monitor.worked( 1 );
        monitor.setTaskName( "Initialization" );
        status().println( "Initializing ..." );

        simulator.initialize();

        monitor.worked( 1 );
        monitor.setTaskName( "Simulation" );
        status().println( "Simulating ..." );

        int result;

        while( true )
        {
            result = simulator.advanceSimulation( 5000 );

            if( monitor.isCanceled() )
            {
                // print out the final sim status.
                simulator.cleanup();
                error().println("Cancellation requested");
                status().println("Closing simulator");
                detachConsole();
                monitor.done();
                return;
            }

            if( result != PhasedSimulator.RUNNING )
            {
                if( result == PhasedSimulator.FAILED )
                {
                    error().println("Simulation failed");
                    status().println("Closing simulator");
                    detachConsole();
                    monitor.done();
                    return;
                }

                break;
            }
        }

        if( result == PhasedSimulator.COMPLETED )
            status().println("Simulation ran to completion");
        else
            status().println("Simulation reached error limit");

        monitor.worked( 1 );
        monitor.setTaskName( "Cleanup" );

        simulator.cleanup();

        status().println("Closing simulator");

        detachConsole();
        monitor.done();
            }

}
