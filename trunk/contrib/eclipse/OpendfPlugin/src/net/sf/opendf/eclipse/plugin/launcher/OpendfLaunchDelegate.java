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
package net.sf.opendf.eclipse.plugin.launcher;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import net.sf.opendf.cli.Elaborator;
import net.sf.opendf.cli.PhasedSimulator;
import net.sf.opendf.config.ConfigGroup;
import net.sf.opendf.config.SimulationConfigGroup;
import net.sf.opendf.eclipse.plugin.OpendfConstants;
import net.sf.opendf.eclipse.plugin.config.ConfigUpdateWrapper;
import net.sf.opendf.eclipse.plugin.config.OpendfConfigLaunchDelegate;
import net.sf.opendf.eclipse.plugin.debug.model.OpendfDebugTarget;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.w3c.dom.Node;

/**
 * The Launch delegate for opendf implementing simulation and debugging functionality
 * 
 * Note: this could also be extended to implement profiling capability
 * 
 * @version 18 March 2009
 *
 */
public class OpendfLaunchDelegate extends OpendfConfigLaunchDelegate {
	private static final String consolePrefix = "Simulation";

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (mode.equals(ILaunchManager.RUN_MODE)) {
			launchSimulator(configuration, mode, launch, monitor);
		} else if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			launchDebugger(configuration, mode, launch, monitor);
		} else {
			throw new CoreException(new Status(IStatus.ERROR, OpendfConstants.ID_PLUGIN, 0, "Unknown launch mode: " + mode, null));
		}
	}

	/**
	 * Launch a debugging session
	 * 
	 * @param configuration
	 * @param mode
	 * @param launch
	 * @param monitor
	 */
	private void launchDebugger(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// if in debug mode, add debug arguments - i.e. '-debug requestPort eventPort'
		assert mode.equals(ILaunchManager.DEBUG_MODE);

		ConfigGroup configs = new SimulationConfigGroup();
		// Update all the configs with the user settings.
		configs.updateConfig(new ConfigUpdateWrapper(configuration), configs.getConfigs().keySet());

		attachConsole(consolePrefix, configs);
		monitor.beginTask("Starting dataflow debugger", 5);
		status().println("Starting dataflow debugger ...");

		int commandPort = -1;
		int eventPort = -1;
		commandPort = findFreePort();
		eventPort = findFreePort();
		if (commandPort == -1 || eventPort == -1) {
			throw new CoreException(new Status(IStatus.ERROR, OpendfConstants.ID_PLUGIN, 0, "Unable to find free port", null));
		}

		//elaborate the actor network
		//this may need to be moved to the model of the graphical viewer
		//However for debug purposes let's do it here
		
		monitor.worked(5);
		monitor.setTaskName("Elaboration");
		status().println("Elaborating ...");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

//    Node dom = null;
//		try {
//			dom = Elaborator.elaborateModel(configs, null, OpendfLaunchDelegate.class.getClassLoader());
//		} catch (Exception e) {
//			error().println("Elaboration failed: " + e.getMessage());
//			status().println("Closing debugger");
//			detachConsole();
//			monitor.done();
//			return;
//		}
//
//		printDom(dom, 0);
		
		monitor.worked(5);
		monitor.setTaskName("Launching Debugger");
		status().println("Launching Debugger ...");
	
		List<String> commandList = new ArrayList<String>();
		commandList.add("debugger.bat");
		commandList.add("-debug");
		commandList.add("" + commandPort);
		commandList.add("" + eventPort);

  	String[] commandLine = (String[]) commandList.toArray(new String[commandList.size()]);
  	
  	System.out.print("Launching debugger with command: '");
  	for (int i = 0; i < commandLine.length; i++) {
  		System.out.print(commandLine[i] + " ");
  	}
  	System.out.println("'");
  	
		Process process = DebugPlugin.exec(commandLine, null);
		IProcess p = DebugPlugin.newProcess(launch, process, "Opendf Debugger");
		
		monitor.worked(5);
		status().println("Debugging");
		detachConsole();
		monitor.done();

		IDebugTarget target = new OpendfDebugTarget(launch, p, commandPort, eventPort);
		launch.addDebugTarget(target);
	}


	private void printDom(Node dom, int indent) {
		for (int i = 0; i < indent; i++) {
			System.out.print(" ");
		}
		System.out.println("<" + dom.getNodeName() + ">");
		
		if (dom.hasAttributes()) {
			for (int i = 0; i < dom.getAttributes().getLength(); i++) {
				for (int j = 0; j < indent + 2; j++) {
					System.out.print(" ");
				}
				System.out.println(dom.getAttributes().item(i));
			}
		}
		
		for (int i = 0; i < dom.getChildNodes().getLength(); i++) {
			printDom(dom.getChildNodes().item(i), indent + 4);
		}
		
		for (int i = 0; i < indent; i++) {
			System.out.print(" ");
		}
		System.out.println("</" + dom.getNodeName() + ">");

	}

	/**
	 * The original method to launch a dataflow simulation
	 * 
	 * @param configuration
	 * @param mode
	 * @param launch
	 * @param monitor
	 * @throws CoreException
	 */
	private void launchSimulator(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		assert mode.equals(ILaunchManager.RUN_MODE);
		ConfigGroup configs = new SimulationConfigGroup();
		// Update all the configs with the user settings.
		configs.updateConfig(new ConfigUpdateWrapper(configuration), configs.getConfigs().keySet());

		attachConsole(consolePrefix, configs);

		monitor.beginTask("Dataflow Simulation", 5);
		status().println("Starting dataflow simulator");

		monitor.setTaskName("Setup");

		PhasedSimulator simulator = new PhasedSimulator(configs);

		monitor.worked(1);
		monitor.setTaskName("Elaboration");
		status().println("Elaborating ...");

		if (!simulator.elaborate()) {
			error().println("Elaboration failed");
			status().println("Closing simulator");
			detachConsole();
			monitor.done();
			return;
		}

		monitor.worked(1);
		monitor.setTaskName("Initialization");
		status().println("Initializing ...");

		simulator.initialize();

		monitor.worked(1);
		monitor.setTaskName("Simulation");
		status().println("Simulating ...");

		int result;

		while (true) {
			result = simulator.advanceSimulation(5000);

			if (monitor.isCanceled()) {
				// print out the final sim status.
				simulator.cleanup();
				error().println("Cancellation requested");
				status().println("Closing simulator");
				detachConsole();
				monitor.done();
				return;
			}

			if (result != PhasedSimulator.RUNNING) {
				if (result == PhasedSimulator.FAILED) {
					error().println("Simulation failed");
					status().println("Closing simulator");
					detachConsole();
					monitor.done();
					return;
				}

				break;
			}
		}

		if (result == PhasedSimulator.COMPLETED)
			status().println("Simulation ran to completion");
		else
			status().println("Simulation reached error limit");

		monitor.worked(1);
		monitor.setTaskName("Cleanup");

		simulator.cleanup();

		status().println("Closing simulator");

		detachConsole();
		monitor.done();
	}

	/**
	 * Returns a free port number on localhost, or -1 if unable to find a free port.
	 * 
	 * @return a free port number on localhost, or -1 if unable to find a free port
	 */
	public static int findFreePort() {
		ServerSocket socket= null;
		try {
			socket= new ServerSocket(0);
			return socket.getLocalPort();
		} catch (IOException e) { 
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
				}
			}
		}
		return -1;		
	}		

	
	
}
