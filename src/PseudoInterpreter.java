import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import socket.SocketServer;

/**
 * A small test program that mimics the debugger interface of an opendf
 * execution engine
 * 
 * @author Rob Esser
 * @version 24 March 2009
 * 
 */
public class PseudoInterpreter extends Thread {

	private static final int MAXTIMEOUT = 100; //actual delay = LOOPDELAY * MAXTIMEOUT (ms)
	private static final int LOOPDELAY = 100; //in ms
	private SocketServer cmdServer;
	private SocketServer eventServer;
	private Map<String, String> componentNames = new HashMap<String, String>();
	private boolean terminateInterpreter = false;
	
	private Map<String, String> variables = new HashMap<String, String>();

	public PseudoInterpreter(int cmdPortNumber, int eventPortNumber) {
		System.out.println("Opendf Test Debugger Execution Engine");
		cmdServer = new SocketServer(cmdPortNumber);
		eventServer = new SocketServer(eventPortNumber);
		startup();
		behaviour();
	}

	private void startup() {
		System.out.println("Waiting for clients to connect");
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
			System.out.println("Sent cmd ack: " + event);
			cmdServer.getOutputStream().println(event);
			cmdServer.getOutputStream().flush();
		}
	}

	/**
	 * In this method we mimic the behaviour of the running dataflow program
	 */
	private void behaviour() {
		while (!terminateInterpreter) {
			for (String compName : componentNames.keySet()) {
				if (componentNames.get(compName).equals("Running")) {
					// we can let this state persist for a while before we hit a breakpoint
					componentNames.put(compName, "Suspended");
					writeEvent("suspended " + compName + ":breakpoint 10");
				} else if (componentNames.get(compName).equals("Stepping")) {
					// we can let this state persist for a while before we complete
					componentNames.put(compName, "Suspended");
					writeEvent("suspended " + compName + ":step");
				}
			}
			try {
				Thread.sleep(2500);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Here we implement the command interface

	 step N - single step forward in component N; 
	   reply is ok | nok
	   
	 suspend N - suspend execution of component N;
	   reply is ok
	   
	 resume N - resume execution of component N; 
	   reply is ok

   exit - terminate execution
       reply is ok

   // variables

	 getvar N F M - return the contents of variable M in function F of component N;
	   reply is variable value

	 setvar N F M V - set the contents of variable M in function F of component N to value V; 
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

	 clear N L - clear the breakpoint in component N on line L;
	   reply is ok | nok

	 set N L - set a line breakpoint in component N on line L (lines are indexed from 0); 
	   reply is ok | nok
	 */
	public void run() {
		String command = "";
		//PrintStream output = cmdServer.getOutputStream();
		BufferedReader input = new BufferedReader(new InputStreamReader(cmdServer.getInputStream()));
		try {
			while (!terminateInterpreter) {
				command = input.readLine();
				// parse events
				System.out.println("Command received: " + command);
				if (command.startsWith("exit")) {
					sendReply("ok");
					terminate();
				} else if (command.startsWith("resume")) {
					String compName = extractComponentName(command);
					sendReply("ok");
 					writeEvent("resumed " + compName + ":client");
 					componentNames.put(compName, "Running");
				} else if (command.startsWith("suspend")) {
					String compName = extractComponentName(command);
					sendReply("ok");
 					writeEvent("suspended " + compName + ":client");
 					componentNames.put(compName, "Suspended");
				} else if (command.startsWith("stack")) {
				  //"fileName|componentName|function name|location|variable name|variable name|...|variable name"
					String compName = extractComponentName(command);
					sendReply(compName + ".cal|" + compName + "|func_2|20|aFunVar|bFunVar#" + compName + ".cal|" + compName + "|action_1|10|aVar|bVar" );
				} else if (command.startsWith("getComponents")) {
					// "componentName|componentName|componentName|..."
					String[] actors = { "Actor_A", "Actor_B" };
					String reply = "";
					for (int i = 0; i < actors.length; i++) {
						String name = actors[i];
						if (!componentNames.containsKey(name)) {
							componentNames.put(name, "Running");
						}
						if (i == 0) {
							reply += name;
						} else {
							reply = reply + "|" + name;
						}
					}
					sendReply(reply);
				} else if (command.startsWith("getvar")) {
					//getvar N F M
					StringTokenizer tokenizer = new StringTokenizer(command, " ");
					//skip getvar command
					tokenizer.nextToken();
					String compName = tokenizer.nextToken();
					String funcName = tokenizer.nextToken();
					String varName = tokenizer.nextToken();
					String key = compName + "." + funcName + "." + varName;
					//System.out.println("-getvar-: " + key);
					if (variables.containsKey(key)) {
						sendReply(variables.get(key));
					} else {
						sendReply("" + key.hashCode());
						variables.put(key, "" + key.hashCode());
					}
				} else if (command.startsWith("setvar")) {
					//setvar Actor_A action_1 bVar 24
					StringTokenizer tokenizer = new StringTokenizer(command, " ");
					//skip setvar command
					tokenizer.nextToken();
					String compName = tokenizer.nextToken();
					String funcName = tokenizer.nextToken();
					String varName = tokenizer.nextToken();
					String value = tokenizer.nextToken();
					String key = compName + "." + funcName + "." + varName;
					//System.out.println("-setvar-: " + key);
					if (variables.containsKey(key)) {
						variables.put(key, value);
					} else {
						System.err.println("Variable not present, compName: " + compName + " func: " + funcName + " varName: " + varName);
					}
					sendReply("ok");
				} else if (command.startsWith("step")) {
					String compName = extractComponentName(command);
					sendReply("ok");
 					writeEvent("resumed " + compName + ":step");
 					componentNames.put(compName, "Stepping");
				} else { 
					System.err.println("Unknown debugger command received: " + command);
					sendReply("ok");
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
		terminateInterpreter = true;
	}

	
	private String extractComponentName(String command) {
		int index = command.indexOf(" ");
		String compName = command.substring(index + 1);
		index = compName.indexOf(" ");
		return index < 0 ? compName : compName.substring(0, index);
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
