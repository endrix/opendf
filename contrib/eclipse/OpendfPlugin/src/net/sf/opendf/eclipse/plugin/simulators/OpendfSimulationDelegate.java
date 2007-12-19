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
import org.eclipse.ui.console.*;
import net.sf.opendf.eclipse.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import net.sf.opendf.util.logging.*;
import java.util.logging.*;
import java.lang.Runnable;

public class OpendfSimulationDelegate implements ILaunchConfigurationDelegate 
{
  private MessageConsoleStream status;  // Launch progress
  private MessageConsoleStream error;   // Simulator error messages
  private MessageConsoleStream info;    // Simulator info messages
  private MessageConsoleStream output;  // Simulation output

  private Handler errorHandler;
  private Handler infoHandler;
  private Handler outputHandler;
  
  private MessageConsole statusConsole;
  
  private void attachConsole()
  {
    MessageConsole outputConsole = OpendfPlugin.getDefault().findConsole( "Simulation Output" );
    statusConsole = OpendfPlugin.getDefault().findConsole( "Simulation Status" );
    
    status = statusConsole.newMessageStream();
    error  = statusConsole.newMessageStream();
    info   = statusConsole.newMessageStream();
    output = outputConsole.newMessageStream();

    // Now do some work in the UI thread
    Display display = OpendfPlugin.getDefault().getWorkbench().getDisplay();
    
    display.syncExec
    (
      new Runnable()
      {
        public void run()
        {
          IWorkbench bench = OpendfPlugin.getDefault().getWorkbench();
          Color red  = bench.getDisplay().getSystemColor( SWT.COLOR_RED );
          Color blue = bench.getDisplay().getSystemColor( SWT.COLOR_BLUE );
          
          status.setColor( blue );
          status.setFontStyle( SWT.BOLD );

          error.setColor( red );
          
          // make the status console visible
          try
          {
            IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
            IWorkbenchPage page = win.getActivePage();
            IConsoleView view = (IConsoleView) page.showView( IConsoleConstants.ID_CONSOLE_VIEW );
            view.display( statusConsole );
          }
          catch( Exception e )
          {
            OpendfPlugin.logErrorMessage( "Failed to make console visible", e );
          }
        }
      }
    );
    
    errorHandler  = new FlushedStreamHandler( error , new BasicLogFormatter() );
    infoHandler   = new FlushedStreamHandler( info  , new BasicLogFormatter() );
    outputHandler = new FlushedStreamHandler( output, new BasicLogFormatter() );
    
    Logging.dbg().addHandler( errorHandler );
    Logging.user().addHandler( infoHandler );
    Logging.simout().addHandler( outputHandler );
  }
  
  private void detachConsole()
  {
    Logging.dbg().removeHandler( errorHandler );
    Logging.user().removeHandler( infoHandler );
    Logging.simout().removeHandler( outputHandler );
  }
  
  public void launch(ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor ) throws CoreException 
  {
    theWork( configuration, monitor );
  }
  
  private void theWork( ILaunchConfiguration configuration, IProgressMonitor monitor ) throws CoreException 
  {
    attachConsole();
    
    monitor.beginTask( "Dataflow Simulation", 5 );
    status.println("Starting dataflow simulator" );
    
    monitor.setTaskName( "Setup" );

    PhasedSimulator simulator = new PhasedSimulator();

    String arg1Prefix = OpendfConfigurationTab.Export( "SIM.ARG1", "" );
    String arg2Prefix = OpendfConfigurationTab.Export( "SIM.ARG2", "" );
    int l = arg2Prefix.length();
      
    info.print( "simulation command: sb sim");
    try
    {
      for( Object obj: configuration.getAttributes().keySet() )
      {
        String key = (String) obj;
         
        if( key.startsWith( arg1Prefix ) )
        {
          String value = configuration.getAttribute( key, (String) null );
          simulator.setArg( value );
          info.print( " " + value );
        }
        else if( key.startsWith( arg2Prefix ) )
        {
          String name = key.substring( l ); 
          String value = configuration.getAttribute( key, (String) null );
          simulator.setArg( name, value );
          info.print(" " + name + " " + value );
        }

      }
    }
    catch( CoreException e ) {}
    info.println();
    
    monitor.worked( 1 );
    monitor.setTaskName( "Elaboration" );
    status.println( "Elaborating ..." );
      
    if( ! simulator.elaborate() )
    {
      error.println( "Elaboration failed" );
      status.println("Closing simulator");
      detachConsole();
      monitor.done();
      return;
    }
      
    monitor.worked( 1 );
    monitor.setTaskName( "Initialization" );
    status.println( "Initializing ..." );
      
    simulator.initialize();

    monitor.worked( 1 );
    monitor.setTaskName( "Simulation" );
    status.println( "Simulating ..." );
      
    int result;
      
    while( true )
    {
      result = simulator.advanceSimulation( 5000 );
        
      if( monitor.isCanceled() )
      {
        error.println("Cancellation requested");
        status.println("Closing simulator");
        detachConsole();
        monitor.done();
        return;
      }
        
      if( result != PhasedSimulator.RUNNING )
      {
        if( result == PhasedSimulator.FAILED )
        {
          status.println("Closing simulator");
          detachConsole();
          monitor.done();
          return;
        }
          
        break;
      }
    }

    if( result == PhasedSimulator.COMPLETED )
      status.println("Simulation ran to completion");
    else
      status.println("Simulation reached error limit");
    
    monitor.worked( 1 );
    monitor.setTaskName( "Cleanup" );
      
    simulator.cleanup();
       
    status.println("Closing simulator");
    
    detachConsole();
    monitor.done();
  }

}
