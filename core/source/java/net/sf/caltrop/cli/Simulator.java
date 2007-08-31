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

package net.sf.caltrop.cli;

import static net.sf.caltrop.util.xml.Util.applyTransformAsResource;
import static net.sf.caltrop.util.xml.Util.applyTransformsAsResources;
import static net.sf.caltrop.util.xml.Util.createTransformer;
import static net.sf.caltrop.cli.Util.createActorParameters;
import static net.sf.caltrop.cli.Util.elaborate;
import static net.sf.caltrop.cli.Util.extractPath;
import static net.sf.caltrop.cli.Util.initializeLocators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
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

import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.util.SourceReader;
import net.sf.caltrop.cli.lib.EvaluatedStreamCallback;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.des.util.OutputBlockRecord;
import net.sf.caltrop.hades.models.ModelInterface;
import net.sf.caltrop.hades.models.lib.XDFModelInterface;
import net.sf.caltrop.hades.simulation.SequentialSimulator;
import net.sf.caltrop.hades.simulation.SequentialSimulatorCallback;
import net.sf.caltrop.hades.simulation.StreamIOCallback;
import net.sf.caltrop.hades.util.NullInputStream;
import net.sf.caltrop.hades.util.NullOutputStream;
import net.sf.caltrop.util.Loading;
import net.sf.caltrop.util.logging.Logging;
import net.sf.caltrop.util.source.LoadingErrorException;
import net.sf.caltrop.util.source.LoadingErrorRuntimeException;
import net.sf.caltrop.util.xml.Util.TransformFailedException;

public class Simulator {
	
	public static void  main(String [] args) throws Exception
    {
		String inFile = null;
		String outFile = null;
		double time = -1;
		long nSteps = -1;
		String [] modelPath = null;
		String actorClass = null;
		String cachePath = null;
        Level userVerbosity = Logging.user().getLevel();
        boolean elaborate = false;
		boolean debug = false;
        boolean interpretStimulus = true;
        boolean bufferBlockRecord = false;
		
		Map<String, String> params = new HashMap<String, String>();
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-n")) {
				if (nSteps >= 0) usage();
				i += 1;
				if (i >= args.length) usage();
				nSteps = Long.parseLong(args[i]);
			} else if (args[i].equals("-t")) {
				if (time >= 0) usage();
				i += 1;
				if (i >= args.length) usage();
				time = Double.parseDouble(args[i]);				
			} else if (args[i].equals("-i")) {
				if (inFile != null) usage();
				i += 1;
				if (i >= args.length) usage();
				inFile = args[i];
			} else if (args[i].equals("-o")) {
				if (outFile != null) usage();
				i += 1;
				if (i >= args.length) usage();
				outFile = args[i];
			} else if (args[i].equals("--no-interpret-stimulus")) {
                interpretStimulus = false;
			} else if (args[i].equals("-e")) {
				elaborate = true;
			} else if (args[i].equals("-ea")) {
				System.setProperty("EnableAssertions", "true");
			} else if (args[i].equals("-tc")) {
				System.setProperty("EnableTypeChecking", "true");
			} else if (args[i].equals("-bbr")) {
				System.setProperty("CalBufferBlockRecord", "true");
				bufferBlockRecord = true;
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

				params.put(v, expr);
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
				if (cachePath != null) usage();
				i += 1;
				if (i >= args.length) usage();
				cachePath = args[i];				
			} else if (args[i].equals("-mp")) {
				if (modelPath != null) usage();
				i += 1;
				if (i >= args.length) usage();
				modelPath = extractPath(args[i]);
            } else if (args[i].equals("--version")) {
                VersionInfo.printVersion();
                System.exit(0);
            }
            else if (!args[i].startsWith("-")) {
				if (actorClass != null)	usage();
				actorClass = args[i];
			} else {
				usage();
			}
		}
		
		if (actorClass == null) {
			usage();
		}
		
		Logging.user().info(NameString);

        if (modelPath == null) {
            modelPath = new String [] {"."};
        }
        Logging.user().info("Model Path: " + Arrays.asList(modelPath));
        ClassLoader classLoader = new SimulationClassLoader(Simulator.class.getClassLoader(), modelPath, cachePath);

        Platform platform = DefaultPlatform.thePlatform;

        DiscreteEventComponent dec = createDEC(modelPath, actorClass,	elaborate, params, classLoader, platform);
        
            
        InputStream is = inFile == null ? new NullInputStream() : new FileInputStream(inFile);
        OutputStream os = outFile == null ? new NullOutputStream() : 
        (".".equals(outFile) ? System.out : new FileOutputStream(outFile));
        SequentialSimulatorCallback callback = interpretStimulus ? new EvaluatedStreamCallback(is, os, platform):new StreamIOCallback(is, os);

        SequentialSimulator sim = null;
        try
        {
            sim = new SequentialSimulator(dec, callback);
        }
        catch (LoadingErrorRuntimeException lere)
        {
            Logging.user().severe("Could not initialize Simulator");
            lere.getErrorContainer().logTo(Logging.user());
            System.exit(-1);
        }
        catch (Exception e)
        {
            Logging.dbg().throwing("Simulator", "main", e);
            Logging.user().severe(e.toString());
            Logging.user().severe("Could not initialize simulator.\nAn error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
            
        long stepCount = 0;
        double currentTime = 0;
        double lastTime = 0;
        Logging.user().info("Running...");
        long beginWallclockTime = System.currentTimeMillis();
        try
        {
            while (sim.hasEvent() && 
                (nSteps < 0 || stepCount < nSteps) &&
                (time < 0 || currentTime <= time)) {
                sim.step();
                stepCount += 1;
                lastTime = currentTime;
                currentTime = sim.currentTime();
            }
        }
        catch (Exception e)
        {
            Logging.dbg().throwing("Simulator", "main", e);
            Logging.user().severe(e.toString());
            Logging.user().severe("Error during simulation.\nAn error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
        
        long endWallclockTime = System.currentTimeMillis();
        long wcTime = endWallclockTime - beginWallclockTime;
        if (os != null)
        	os.close();
        
        postSimulationMessage(userVerbosity, bufferBlockRecord, sim, stepCount, lastTime, wcTime);
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
        catch (LoadingErrorException lee)
        {
            Logging.user().severe(lee.getMessage());
            lee.getErrorContainer().logTo(Logging.user());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
        catch (LoadingErrorRuntimeException lere)
        {
            Logging.user().severe(lere.getMessage());
            lere.getErrorContainer().logTo(Logging.user());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
        catch (DECLoadException dle)
        {
            Logging.user().severe("Could not load simulation model." + dle.getMessage());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
        catch (ClassNotFoundException cnfe)
        {
            Logging.user().severe("Could not load simulation model." + cnfe.getMessage());
            Logging.user().severe("Specify the simulatable entity as a class (no extension)");
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
            System.exit(-1);
        }
        catch (TransformFailedException tfe)
        {
            Logging.user().severe(tfe.getMessage());
            Logging.user().severe("Could not elaborate network " + actorClass);
            System.exit(-1);
        }
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
        		StringBuffer msg = new StringBuffer("Blocked: ");
        		msg.append(obr.getComponentName() + " (");
        		boolean first = true;
        		for (String p : obr.getBlockedOutputConnectors()) {
        			if (!first) msg.append(", ");
        			msg.append(p);
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
	
	private static void usage() {
		System.out.println("Usage: Simulator [options] actor-class");
        System.out.println("  -ea                 enables assertion checking during simulation");
        System.out.println("  -n <##>             defines an upper bound for number of simulation steps");
        System.out.println("  -t <##>             defines an upper bound for the simulation time");
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

