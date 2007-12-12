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

package net.sf.opendf.cli;

import static net.sf.opendf.util.xml.Util.applyTransformAsResource;
import static net.sf.opendf.util.xml.Util.applyTransformsAsResources;
import static net.sf.opendf.util.xml.Util.createTransformer;
import static net.sf.opendf.cli.Util.createActorParameters;
import static net.sf.opendf.cli.Util.elaborate;
import static net.sf.opendf.cli.Util.extractPath;
import static net.sf.opendf.cli.Util.initializeLocators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.transform.Transformer;

import org.w3c.dom.Node;

import net.sf.opendf.cal.interpreter.ExprEvaluator;
import net.sf.opendf.cal.interpreter.environment.Environment;
import net.sf.opendf.cal.interpreter.util.DefaultPlatform;
import net.sf.opendf.cal.interpreter.util.Platform;
import net.sf.opendf.cal.util.SourceReader;
import net.sf.opendf.cli.lib.EvaluatedStreamCallback;
import net.sf.opendf.util.exception.ExceptionHandler;
import net.sf.opendf.util.exception.ReportingExceptionHandler;
import net.sf.opendf.hades.des.DiscreteEventComponent;
import net.sf.opendf.hades.des.util.OutputBlockRecord;
import net.sf.opendf.hades.models.ModelInterface;
import net.sf.opendf.hades.models.lib.XDFModelInterface;
import net.sf.opendf.hades.simulation.SequentialSimulator;
import net.sf.opendf.hades.simulation.SequentialSimulatorCallback;
import net.sf.opendf.hades.simulation.StreamIOCallback;
import net.sf.opendf.hades.util.NullInputStream;
import net.sf.opendf.hades.util.NullOutputStream;
import net.sf.opendf.util.Loading;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.hades.des.schedule.SchedulerObserver;
import net.sf.opendf.hades.des.EventProcessor;

public class Simulator {
	
	public static void  main(String [] args) throws Exception
    {
		Configuration conf = new Configuration();
        		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-n")) {
				if (conf.nSteps >= 0) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.nSteps = Long.parseLong(args[i]);
			} else if (args[i].equals("-t")) {
				if (conf.time >= 0) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.time = Double.parseDouble(args[i]);				
			} else if (args[i].equals("-i")) {
				if (conf.inFile != null) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.inFile = args[i];
			} else if (args[i].equals("-o")) {
				if (conf.outFile != null) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.outFile = args[i];
			} else if (args[i].equals("--no-interpret-stimulus")) {
                conf.interpretStimulus = false;
			} else if (args[i].equals("-e")) {
				conf.elaborate = true;
			} else if (args[i].equals("--max-errors")) {
				i += 1;
				if (i >= args.length) usage();
				conf.maxErrs = Integer.parseInt(args[i]);
			} else if (args[i].equals("-ea")) {
				System.setProperty("EnableAssertions", "true");
			} else if (args[i].equals("-tc")) {
				System.setProperty("EnableTypeChecking", "true");
			} else if (args[i].equals("-bbr")) {
				System.setProperty("CalBufferBlockRecord", "true");
				conf.bufferBlockRecord = true;
			} else if (args[i].equals("-trace")) {
				System.setProperty("CalFiringTrace", "true");
			} else if (args[i].equals("-bq")) {
				i += 1;
				if (i >= args.length) usage();
				System.setProperty("CalBufferWarning", args[i]);
			} else if (args[i].equals("-bi")) {
				System.setProperty("CalBufferIgnoreBounds", "true");
			} else if (args[i].equals("-D")) {
				i += 1;
				if (i >= args.length) usage();
				String s = args[i].trim();
				int n = s.indexOf('=');
				if (n < 0) usage();

				String v = s.substring(0, n).trim();
				if ("".equals(v)) usage();
				String expr = s.substring(n + 1);

				conf.params.put(v, expr);
            } else if (args[i].equals("-qq")) {
                Logging.user().setLevel(Level.OFF);
            } else if (args[i].equals("-q")) {
                if (Logging.user().isLoggable(Level.WARNING))
                {
                    // If WARNING level is allowed, then make it the
                    // least restrictive level.  If it is not, then do
                    // NOT further relax to the WARNING level
                    Logging.setUserLevel(Level.WARNING);
                }
            } else if (args[i].equals("-v")) {
                Logging.user().setLevel(Level.ALL);                
            } else if (args[i].equals("-debug")) {
                Logging.user().setLevel(Level.ALL);
            } else if (args[i].equals("-debug0")) {
                Logging.dbg().setLevel(Level.ALL);
			} else if (args[i].equals("-cache")) {
				if (conf.cachePath != null) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.cachePath = args[i];				
			} else if (args[i].equals("-mp")) {
				if (conf.modelPath != null) usage();
				i += 1;
				if (i >= args.length) usage();
				conf.modelPath = extractPath(args[i]);
            } else if (args[i].equals("--version")) {
                VersionInfo.printVersion();
                System.exit(0);
            }
            else if (!args[i].startsWith("-")) {
				if (conf.actorClass != null)	usage();
				conf.actorClass = args[i];
			} else {
				usage();
			}
		}
		
		if (conf.actorClass == null) {
			usage();
		}
		
		execute(conf);
    }
	
	/**
	 * The object containing the configuration data for the main simulation loop.
	 * 
	 * @author jornj
	 *
	 */	
	public static class Configuration {
		/**
		 * Path to the file containing the input stimuli, if any. Null if none. 
		 */
		public String inFile = null;
		
		/**
		 * Path to the file receiving the output tokens, if any. Null if none.
		 */
		public String outFile = null;
		
		/**
		 * Upper bound for the simulation time. No event with a time stamp beyond this 
		 * time will be executed. Negative if no upper bound.
		 */
		public double time = -1;
		
		/**
		 * Upper bound for the number of steps executed by the simulation. Negative is none.
		 */
		public long nSteps = -1;
		
		/**
		 * The paths along which the simulator will try to locate the models. Any models not 
		 * located in this path will be loaded as Java classes along the classpath.
		 */
		public String [] modelPath = null;
		
		/**
		 * The class to be simulated.
		 */
		public String actorClass = null;
		
		/**
		 * The path to the cache used to store preprocessed actors, to speed up loading.
		 */
		public String cachePath = null;
		
		/**
		 * The logging level. Used to control the verbosity of the logged output.
		 */
		public Level userVerbosity = Logging.user().getLevel();
		
		/**
		 * The flag used to control whether the model is fully elaborated prior to instantiation, 
		 * or whether elaboration is part of the instantiation process.
		 */
		public boolean elaborate = false;
		
		/**
		 * Flag controlling whether the stimulus file contains expressions that need to be
		 * evaluated.
		 */
		public boolean interpretStimulus = true;
		
		/**
		 * If true, the simulation will track actors blocking on output, and emit a list of 
		 * output-blocked actors in case the system deadlocks.
		 */
		public boolean bufferBlockRecord = false;
		
		/**
		 * Upper bound to the number of errors produced by the simulator.
		 */
		public int maxErrs = 100;		
		
		/**
		 * The parameters of the model. The parameter values are stored as unevaluated strings,
		 * the simulator will perform the evaluation.
		 */
		Map<String, String> params = new HashMap<String, String>();
	}
	
	public static void execute(Configuration conf) throws IOException {
		
		Logging.user().info(NameString);

        if (conf.modelPath == null) {
            conf.modelPath = new String [] {"."};
        }
        Logging.user().info("Model Path: " + Arrays.asList(conf.modelPath));
        ClassLoader classLoader = new SimulationClassLoader(Simulator.class.getClassLoader(), conf.modelPath, conf.cachePath);

        Platform platform = DefaultPlatform.thePlatform;

        SequentialSimulator sim = null;
        InputStream is = null;
        OutputStream os = null;
        try
        {
            DiscreteEventComponent dec = createDEC(conf.modelPath, conf.actorClass,	conf.elaborate, conf.params, classLoader, platform);
            
            is = conf.inFile == null ? new NullInputStream() : new FileInputStream(conf.inFile);
            os = conf.outFile == null ? new NullOutputStream() : 
            (".".equals(conf.outFile) ? System.out : new FileOutputStream(conf.outFile));
            SequentialSimulatorCallback callback = conf.interpretStimulus ? new EvaluatedStreamCallback(is, os, platform):new StreamIOCallback(is, os);

            sim = new SequentialSimulator(dec, callback);
        }
        catch (Throwable t)
        {
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(t);
            System.exit(-1);
        }

        
        final SimRuntimeExceptionHandler simExcHandler = new SimRuntimeExceptionHandler();
        sim.addSchedulerObserver(new SchedulerObserver()
            {
                public void schedulerException(double now, Exception e)
                {
                    simExcHandler.process(e);
                }
                public void schedulerSchedule(double now, double time, double precedence, EventProcessor ep){}
                public void schedulerUnschedule(double now, EventProcessor ep){}
                public void schedulerExecute(double time, double precedence, EventProcessor ep, boolean weak, boolean result){}
            });
        
        
        long stepCount = 0;
        double currentTime = 0;
        double lastTime = 0;
        Logging.user().info("Running...");
        long beginWallclockTime = System.currentTimeMillis();
        try
        {
            while (sim.hasEvent() && 
                (conf.nSteps < 0 || stepCount < conf.nSteps) &&
                (conf.time < 0 || currentTime <= conf.time)) {
                sim.step();
                stepCount += 1;
                lastTime = currentTime;
                currentTime = sim.currentTime();
                if (simExcHandler.getErrorCount() >= conf.maxErrs)
                {
                    Logging.user().severe("Too many errors (" + simExcHandler.getErrorCount() + ")");
                    long endWallclockTime = System.currentTimeMillis();
                    long wcTime = endWallclockTime - beginWallclockTime;
                    postSimulationMessage(conf.userVerbosity, conf.bufferBlockRecord, sim, stepCount, lastTime, wcTime);
                    System.exit(-1);
                }
            }
        }
        catch (Throwable t)
        {
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(t);
            System.exit(-1);
        }
        
        long endWallclockTime = System.currentTimeMillis();
        long wcTime = endWallclockTime - beginWallclockTime;
        if (os != null)
        	os.close();
        
        postSimulationMessage(conf.userVerbosity, conf.bufferBlockRecord, sim, stepCount, lastTime, wcTime);
	}
	
	private static DiscreteEventComponent createDEC(String[] modelPath, String actorClass, 
			boolean elaborate,
			Map<String, String> params, ClassLoader classLoader, Platform platform)
			throws Exception {
		DiscreteEventComponent dec = null;
        try {
        	if (elaborate) {
        		Node res = elaborate(actorClass, modelPath, classLoader, params);
        		
                res = applyTransformsAsResources(res, Elaborator.postElaborationTransformNames);
                
                Logging.user().info("Network successfully elaborated.");
                
                dec = createNetworkDEC(res, classLoader);
        	} else {
                Environment env = platform.context().newEnvironmentFrame(platform.createGlobalEnvironment());
                env.bind("__ClassLoader", platform.context().fromJavaObject(classLoader));
                ExprEvaluator evaluator = new ExprEvaluator(platform.context(), env);
        		
                Map paramValues = new HashMap();
                paramValues.put("__ClassLoader", classLoader);
                for (String var : params.keySet()) {
        			
                    Object value = evaluator.evaluate(SourceReader.readExpr(new StringReader((String)params.get(var))));
                        
                    paramValues.put(var, value);
                }

                dec = loadDEC(actorClass, paramValues, classLoader);
        	}
        }
        catch (DECLoadException dle)
        {
            Logging.user().severe("Could not load simulation model." + dle.getMessage());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(dle);
            System.exit(-1);
        }
        catch (Throwable t)
        {
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(t);
            System.exit(-1);
        }
//         catch (ClassNotFoundException cnfe)
//         {
//             Logging.user().severe("Could not load simulation model." + cnfe.getMessage());
//             Logging.user().severe("Specify the simulatable entity as a class (no extension)");
//             Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
//             System.exit(-1);
//         }
        Logging.user().info("Model successfully instantiated.");

		return dec;
	}
	
	private static DiscreteEventComponent  createNetworkDEC(Node xdf, ClassLoader classLoader) {
		ModelInterface mi = new XDFModelInterface();
		
		return mi.instantiate(xdf, Collections.EMPTY_MAP, mi.createLocationMap(xdf), classLoader);
	}

	private static void postSimulationMessage(Level userVerbosity,
			boolean bufferBlockRecord, SequentialSimulator sim, long stepCount,
			double lastTime, long wcTime) {
		Logging.user().info("Done after " + stepCount + " steps. Last step at time " + lastTime + ".");
        Logging.user().info("Execution time: " + wcTime + "ms."
            + (wcTime > 0 ? " (" + (1000.0 * (double)stepCount / (double)wcTime) + " steps/s)" : ""));
        Logging.user().info("The network is " + (sim.hasEvent() ? "live" : "dead")+ ".");
        if (bufferBlockRecord && !sim.hasEvent()) {
        	Collection<OutputBlockRecord> obrs = (Collection<OutputBlockRecord>)sim.getProperty("OutputBlockRecords");
        	int n = 0;
        	List<OutputBlockRecord> sortedObrs = obrs == null ? new ArrayList<OutputBlockRecord>():new ArrayList<OutputBlockRecord>(obrs);
        	Collections.sort(sortedObrs, obrComparator);
        	for (OutputBlockRecord obr : sortedObrs) {
            	Map<String, Collection<Object>> blockingSourceMap = obr.getBlockingSourceMap();
        		StringBuffer msg = new StringBuffer("Blocked: ");
        		msg.append(obr.getComponentName() + " (");
        		boolean first = true;
        		for (String p : obr.getBlockedOutputConnectors()) {
        			if (!first) msg.append(", ");
        			msg.append(p);
        			Collection<Object> sources = blockingSourceMap.get(p);
        			if (sources != null && !sources.isEmpty()) {
        				msg.append(" ");
        				msg.append(formatBlockingSources(sources));
        			}
        			first = false;
        		}
        		msg.append(") at step " + obr.getStepNumber() + ", time " + obr.getTime());
        		Logging.user().info(msg.toString());
        		n += 1;
        	}
        	Logging.user().info("" + n + " actors blocked.");
        }

        Logging.setUserLevel(userVerbosity); // Re-set the verbosity
	}
	
	private static String  formatBlockingSources(Collection<Object> sources) {
		StringBuffer msg = new StringBuffer("[");
		boolean first = true;
		for (Object s : sources) {
			if (!first) msg.append(", ");
			msg.append(s.toString());
		}
		msg.append("]");
		return msg.toString();
	}
	
	private static void usage() {
		System.out.println("Usage: Simulator [options] actor-class");
        System.out.println("  -ea                 enables assertion checking during simulation");
        System.out.println("  -n <##>             defines an upper bound for number of simulation steps");
        System.out.println("  -t <##>             defines an upper bound for the simulation time");
        System.out.println("  --max-errors <##>   defines an upper bound for the maximum number of allowable errors during simulation");
        System.out.println("  -i <file>           identifies the input stimuli (vector) file");
        System.out.println("  -o <file>           defines the output vectors");
        System.out.println("  -D <param def>      allows specification of parameter defs");
        System.out.println("  -q                  run quietly");
        System.out.println("  -v                  run verbosely");
        System.out.println("  -bbr                detect and report output-blocked actors on deadlock");
        System.out.println("  -bi                 ignore buffer bounds (all buffers are unbounded)");
        System.out.println("  -bq <##>            produces a warning if an input queue everbecomes bigger than the specified value");
        System.out.println("  -mp <paths>         specifies the search paths for model files");        
        System.out.println("  -cache <path>       the path to use for caching precompiled models");        
        System.out.println("                      If none is specified, caching is turned off.");        
        System.out.println("  --version           Display Version information and quit");
		System.exit(-1);
	}
		
	
	static DiscreteEventComponent loadDEC(String className, Map decParams, ClassLoader classLoader) throws ClassNotFoundException, DECLoadException
    {

		Class cl = classLoader.loadClass(className);
		
		DiscreteEventComponent dec = null;
		// instantiate component
        try
        {
            Constructor ctor = cl.getConstructor(new Class [] {Object.class});
            if (ctor == null) {
                // no constructor for parameters -> use parameterless constructor
                ctor = cl.getConstructor(new Class [] {});
                if (ctor == null) {
                    throw new RuntimeException("Cannot find appropriate constructor.");
                } else {
                    dec = (DiscreteEventComponent) ctor.newInstance(new Object[0]);
                }
            } else {
                Object[] parArray = {decParams};
                dec = (DiscreteEventComponent) ctor.newInstance(parArray);
            }
        }
        catch (NoSuchMethodException nsme) { throw new DECLoadException(nsme.getMessage()); }
        catch (InstantiationException ie) { throw new DECLoadException(ie.getMessage()); }
        catch (IllegalAccessException iae) { throw new DECLoadException(iae.getMessage()); }
        catch (InvocationTargetException ite) { throw new DECLoadException(ite.getMessage()); }

		return dec;
	}	
	
	private static final String optPath = "path";
	private static final String optModel = "model";
	
	private static final String NameString = "Open Dataflow Engine";

    private static final class DECLoadException extends Exception
    {
        public DECLoadException (String msg) { super(msg); }
    }
   
    private static Comparator<OutputBlockRecord>  obrComparator = new Comparator<OutputBlockRecord> () {
    	public int compare(OutputBlockRecord o1, OutputBlockRecord o2) {
    		long d = o1.getStepNumber() - o2.getStepNumber();
    		if (d < 0)
    			return -1;
    		if (d > 0)
    			return 1;
    		return 0;
    	}    	
    };
}

