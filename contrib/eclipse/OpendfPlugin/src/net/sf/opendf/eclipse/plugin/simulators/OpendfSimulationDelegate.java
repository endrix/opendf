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
package net.sf.opendf.eclipse.plugin.simulators;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import net.sf.opendf.cli.PhasedSimulator;
import net.sf.opendf.eclipse.plugin.simulators.tabs.*;

public class OpendfSimulationDelegate implements ILaunchConfigurationDelegate 
{

  public void launch(ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor) throws CoreException 
  {
    monitor.beginTask( "Dataflow Simulation", 5 );

    monitor.setTaskName( "Setup" );

    System.out.println( "cwd is " + System.getProperty( "user.dir" ));
    
    PhasedSimulator simulator = new PhasedSimulator();

    String arg1 = OpendfConfigurationTab.Export( "SIM.ARG1", "" );
    String arg2 = OpendfConfigurationTab.Export( "SIM.ARG2", "" );
    int l = arg2.length();
    
    for( Object obj: configuration.getAttributes().keySet() )
    {
      String key = (String) obj;
      
      if( key.startsWith( arg1 ) )
      {
        String value = configuration.getAttribute( key, (String) null );
        simulator.setArg( value );
        System.out.println("set arg " + value );
      }
      else if( key.startsWith( arg2 ) )
      {
        String name = key.substring( l ); 
        String value = configuration.getAttribute( key, (String) null );
        simulator.setArg( name, value );
        System.out.println("set arg " + name + ", " + value );
      }

    }

    monitor.worked( 1 );
    monitor.setTaskName( "Elaboration" );

    if( ! simulator.elaborate() )
    {
      monitor.done();
      return;
    }
    
    monitor.worked( 1 );
    monitor.setTaskName( "Initialization" );
    
    simulator.initialize();

    monitor.worked( 1 );
    monitor.setTaskName( "Simulation" );

    while( true )
    {
      int result = simulator.advanceSimulation( 5000 );
      
      if( monitor.isCanceled() )
      {
        monitor.done();
        return;
      }
      
      if( result != PhasedSimulator.RUNNING )
      {
        if( result == PhasedSimulator.FAILED )
        {
          monitor.done();
          return;
        }
        
        break;
      }
    }

    monitor.worked( 1 );
    monitor.setTaskName( "Cleanup" );
    
    simulator.cleanup();
    
    monitor.done();
  }

}
