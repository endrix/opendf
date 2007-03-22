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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.sf.caltrop.cal.interpreter.ExprEvaluator;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.interpreter.util.SourceReader;
import net.sf.caltrop.cli.lib.EvaluatedStreamCallback;
import net.sf.caltrop.hades.des.DiscreteEventComponent;
import net.sf.caltrop.hades.simulation.SequentialSimulator;
import net.sf.caltrop.hades.simulation.SequentialSimulatorCallback;
import net.sf.caltrop.hades.simulation.StreamIOCallback;
import net.sf.caltrop.hades.util.NullInputStream;
import net.sf.caltrop.hades.util.NullOutputStream;
import net.sf.caltrop.util.Logging;

public class Simulator {
	
	public static void  main(String [] args) throws Exception
    {
		String inFile = null;
		String outFile = null;
		double time = -1;
		long nSteps = -1;
		String [] modelPath = null;
		String actorClass = null;
		String platformName = null;
		String cachePath = null;
        Level userVerbosity = Logging.user().getLevel();
		boolean debug = false;
        boolean interpretStimulus = true;
		
		Map params = new HashMap();
		
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
			} else if (args[i].equals("-ea")) {
				System.setProperty("EnableAssertions", "true");
			} else if (args[i].equals("-P")) {
				if (platformName != null) usage();
				i += 1;
				if (i >= args.length) usage();
				platformName = args[i];
			} else if (args[i].equals("-bq")) {
				i += 1;
				if (i >= args.length) usage();
				System.setProperty("CalBufferWarning", args[i]);
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

        try
        {        	
        	if (modelPath == null) {
        		modelPath = new String [] {"."};
        	}
        	Logging.user().info("Model Path: " + Arrays.asList(modelPath));
            ClassLoader classLoader = new SimulationClassLoader(Simulator.class.getClassLoader(), modelPath, cachePath);
            
            Platform platform = null;
            try
            {
                platform = (platformName == null) ? DefaultPlatform.thePlatform
                : (Platform)classLoader.loadClass(platformName).newInstance();
            }
            catch (ClassNotFoundException cnfe)
            {
                Logging.dbg().throwing("Simulator", "main", cnfe);
                Logging.user().severe("Could not load specified platform: '" + platformName + "'");
                System.exit(-1);
            }
		
            System.setProperty("CalPlatform", platform.getClass().getName());

            Environment env = platform.context().newEnvironmentFrame(platform.createGlobalEnvironment());
            env.bind("__ClassLoader", platform.context().fromJavaObject(classLoader));
            ExprEvaluator evaluator = new ExprEvaluator(platform.context(), env);
		
            Map paramValues = new HashMap();
            paramValues.put("__ClassLoader", classLoader);
            for (Iterator i = params.keySet().iterator(); i.hasNext(); ) {
                String var = (String)i.next();
			
//                 Lexer calLexer = new Lexer(new StringReader((String)params.get(var)));
//                 CalExpressionParser parser = new CalExpressionParser(calLexer);
//                 Document exprDom = parser.doParse();
//                 Expression exprAst = ASTFactory.buildExpression(exprDom);
//                 Object value = evaluator.evaluate(exprAst);
                Object value = evaluator.evaluate(SourceReader.readExpr(new StringReader((String)params.get(var))));
                
                paramValues.put(var, value);
            }
				
            DiscreteEventComponent dec = loadDEC(actorClass, paramValues, classLoader);
            
            InputStream is = inFile == null ? new NullInputStream() : new FileInputStream(inFile);
            OutputStream os = outFile == null ? new NullOutputStream() : 
            	                        (".".equals(outFile) ? System.out : new FileOutputStream(outFile));
            SequentialSimulatorCallback callback = interpretStimulus ? new EvaluatedStreamCallback(is, os, platform):new StreamIOCallback(is, os);
            SequentialSimulator sim = new SequentialSimulator(dec, callback);
            
            long stepCount = 0;
            double currentTime = 0;
            double lastTime = 0;
            long beginWallclockTime = System.currentTimeMillis();
            while (sim.hasEvent() && 
                (nSteps < 0 || stepCount < nSteps) &&
                (time < 0 || currentTime <= time)) {
                sim.step();
                stepCount += 1;
                lastTime = currentTime;
                currentTime = sim.currentTime();
            }
            long endWallclockTime = System.currentTimeMillis();
            long wcTime = endWallclockTime - beginWallclockTime;
            
            Logging.user().info("Done after " + stepCount + " steps. Last step at time " + lastTime + ".");
            Logging.user().info("Execution time: " + wcTime + "ms."
            		            + (wcTime > 0 ? " (" + (1000.0 * (double)stepCount / (double)wcTime) + " steps/s)" : ""));
            Logging.user().info("The network is " + (sim.hasEvent() ? "live" : "dead")+ ".");

        }
        catch (Exception e)
        {
            Logging.dbg().throwing("Simulator", "main", e);
            Logging.dbg().warning(e.getMessage());
            StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            Logging.user().finest(s.toString());
            Logging.user().fine(e.getMessage());
            Logging.user().severe("An error has occurred.  Exiting abnormally.\n");
        }

        Logging.setUserLevel(userVerbosity); // Re-set the verbosity
	}
	
	private static String []  extractPath(String p) {
		List<String> paths = new ArrayList<String>();
		boolean done = false;
		String r = p;
		while (!done) {
			String s = null;
			int n = r.indexOf(File.pathSeparatorChar);
			if (n < 0) {
				s = r;
				done = true;
			} else {
				s = r.substring(0, n);
				r = (r.length() > n) ? r.substring(n + 1) : "";
			}
			s = s.trim();
			if (!"".equals(s)) {
				paths.add(s);
			}
		}
		return paths.toArray(new String [paths.size()]);
	}
	
	private static void usage() {
		System.out.println("Usage: Simulator [options] actor-class");
        System.out.println("  -ea                 enables assertion checking during simulation");
        System.out.println("  -n <##>             defines an upper bound for number of simulation steps");
        System.out.println("  -t <##>             defines an upper bound for the simulation time");
        System.out.println("  -i <file>           identifies the input stimuli (vector) file");
        System.out.println("  -o <file>           defines the output vectors");
        System.out.println("  -P <platform class> defines the platform to use for CAL code interpretation");
        System.out.println("  -D <param def>      allows specification of parameter defs");
        System.out.println("  -q                  run quietly");
        System.out.println("  -v                  run verbosely");        
        System.out.println("  -bq <##>            produces a warning if an input queue everbecomes bigger than the specified value");
        System.out.println("  -mp <paths>         specifies the search paths for model files");        
        System.out.println("  -cache <path>       the path to use for caching precompiled models");        
        System.out.println("                      If none is specified, caching is turned off.");        
        System.out.println("  --version           Display Version information and quit");
		System.exit(-1);
	}
		
	
	static DiscreteEventComponent  loadDEC(String className, Map decParams, ClassLoader classLoader) throws Exception {

		Class cl = classLoader.loadClass(className);
		
		DiscreteEventComponent dec = null;
		// instantiate component
		Constructor ctor = cl.getConstructor(new Class [] {Object.class});
        try
        {
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
        catch (InvocationTargetException ite)
        {
            Logging.dbg().warning(ite.toString());
            // Find the root cause of the error in the hopes that it
            // is meaningful to the user.  It would be nice if there
            // were a better error handling framework across
            // systembuilder, xcal and moses.
            Throwable t = ite;
            while (t.getCause() != null)
            {
                Logging.dbg().warning(t.getCause().toString());
                t = t.getCause();
            }
            Logging.user().warning("Could not load simulation element \'" + className + "\' due to: \n" + t.getMessage());
            throw ite;
        }
        
		return dec;
	}	
	
	private static final String optPath = "path";
	private static final String optModel = "model";
	
	private static final String NameString = "Open Dataflow Engine";
}

