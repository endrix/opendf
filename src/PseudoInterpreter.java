import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import socket.SocketServer;

/**
 * A small test program that mimics the debugger interface of an opendf
 * execution engine
 * 
 * @author Rob Esser
 * @version 22 March 2009
 * 
 */
public class PseudoInterpreter extends Thread {

	private static final int MAXTIMEOUT = 100; //actual delay = LOOPDELAY * MAXTIMEOUT (ms)
	private static final int LOOPDELAY = 100; //in ms
	private SocketServer cmdServer;
	private SocketServer eventServer;
	private String myName = "MyName";
	
	private String state = "Reset";
	


	public PseudoInterpreter(int cmdPortNumber, int eventPortNumber) {
		System.out.println("Opendf Test Debugger Execution Engine");
		cmdServer = new SocketServer(cmdPortNumber);
		eventServer = new SocketServer(eventPortNumber);
		startup();
		behaviour();
	}

	private void startup() {
		System.out.println("Waiting to for clients to connect");
		int timeout = 0;
		while (!cmdServer.isConnected() || !eventServer.isConnected()) {
			//wait a while
			try {
				Thread.sleep(LOOPDELAY);
			} catch (InterruptedException e) {
			}
			timeout++;
			if (timeout > MAXTIMEOUT) {
				cmdServer.destroy();
				eventServer.destroy();
				System.err.println("Debug Session did not commence in time. Abort.");
				System.exit(1);
			}
		}
		System.out.println("Connected...");
		System.out.println("");
		writeEvent("started");
		//start the receive command thread
		start();
	}

	private void writeEvent(String event) {
		synchronized (eventServer) {
			System.out.println("Sent event: " + event);
			eventServer.getOutputStream().println(event);
			eventServer.getOutputStream().flush();
		}
	}

	private void sendReply(String event) {
		synchronized (cmdServer) {
			cmdServer.getOutputStream().println(event);
			cmdServer.getOutputStream().flush();
		}
	}

	/**
	 * In this method we mimic the behaviour of the running dataflow program
	 */
	private void behaviour() {
		while (!state.equals("Terminated")) {
			if (state.equals("Running")) {
				// we can let this state persist for a while before we hit a breakpoint
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				// say we hit a breakpoint
				state = "Suspended";
				writeEvent("suspended " + myName + ":breakpoint 10");
			} else {
				//just wait a moment
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Here we implement the command interface

	 step N - single step forward in component N; 
	   reply is ok | nok
	   
	 stepAll - step every component;
	   reply is ok

	 suspend N - suspend execution of component N;
	   reply is ok
	   
	 suspendAll - suspend execution of all components;
	   reply is ok

	 resume N - resume execution of component N; 
	   reply is ok

	 resumeAll - resume execution of all components; 
	   reply is ok

   exit - terminate execution
       reply is ok

   // variables

	 getvar N M - return the contents of variable M in component N;
	   reply is variable value

	 setvar N M V - set the contents of variable M in component N to value V; 
	   reply is ok

	 watch N V K - set a watchpoint on variable V in component N to the kind K;
	   the kind K corresponds to read | write | readwrite | clear;
	   reply is ok
	   
	 stack N - return the current stack frame for component N;
	   reply is control stack from oldest to newest as a single string
	   frame#frame#frame...#frame where each frame is a string
	   "componentName|pc|function name|variable name|variable name|...|variable name"
	   

   // status

   getComponentStatus N - return the status of component N
     reply is running | suspended | guards(g1, g2, ... gn) where
     gx = insufficientTokens | incorrectState | insufficientPriority | enabled 
     for each action whether or not it has an explicit guard

   // breakpoints

	 clear N:L - clear the breakpoint in component N on line L;
	   reply is ok | nok

	 set N:L - set a line breakpoint in component N on line L (lines are indexed from 0); 
	   reply is ok | nok
	 */
	public void run() {
		boolean terminated = false;
		String command = "";
		//PrintStream output = cmdServer.getOutputStream();
		BufferedReader input = new BufferedReader(new InputStreamReader(cmdServer.getInputStream()));
		try {
			while (!terminated) {
				command = input.readLine();
				// parse events
				System.out.println("In State : " + state + ", Command received: " + command);
				if (command.startsWith("exit")) {
					sendReply("ok");
					terminate();
					terminated = true;
					state = "Terminated";
				} else if (command.startsWith("resumeAll")) {
				  sendReply("ok");
				  writeEvent("resumed " + myName  + ":client");
					state = "Running";
				} else if (command.startsWith("resume")) {
					sendReply("ok");
					int index = command.indexOf(" ");
					String compName = command.substring(index);
					writeEvent("resumed " + compName + ":client");
					state = "Running";
				} else if (command.startsWith("suspend")) {
					sendReply("ok");
					int index = command.indexOf(" ");
					String compName = command.substring(index);
					state = "Suspended";
				} else if (command.startsWith("stack")) {
					int index = command.indexOf(" ");
					String compName = command.substring(index);
					sendReply(compName + "|10|action 1|a|b");
				} else { 
					System.err.println("Unknown debugger command received: " + command);
					sendReply("ok");
					state = "Error";
				}
			}

			// clean up local interfaces
			input.close();
			// output.close();
		} catch (IOException e) {
			// clean up local interfaces
			terminate();
		}
	}
	
	private void terminate() {
		writeEvent("terminated");
		System.out.println("Opendf Test Debugger Execution Engine. Exit.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// first parse the command line
		if (args.length == 0) {
			usage();
			System.exit(1);
		} else {
			for (int i = 0; i < args.length; i++) {
				// System.out.println(args[i]);
				if (args[i].equalsIgnoreCase("-help")) {
					usage();
					System.exit(0);
				} else if (args[i].equalsIgnoreCase("-debug")) {
					if ((i + 3) < args.length) {
						// not enough arguments left
						usage();
						System.exit(1);
					}
					// extract the ip port numbers
					int cmdPortNumber = -1;
					int eventPortNumber = -1;
          try {
  					i++;
            cmdPortNumber = Integer.parseInt(args[i]);
  					i++;
            eventPortNumber = Integer.parseInt(args[i]);
            new PseudoInterpreter(cmdPortNumber, eventPortNumber);
          } catch (Exception e) {
            System.err.println("*** Error " + args[i] + " is not a valid port number");
          }
				}
			}
		}
	}

	/**
	 * Inform the world how to start the program
	 */
	private static final void usage() {
		System.out.println("usage: debugger options");
		System.out.println("  where options are");
		System.out.println("  -help           this message");
		System.out.println("  -debug <command port> <event port>");
		System.out.println();
	}

}
