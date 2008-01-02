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

import static net.sf.opendf.util.xml.Util.applyTransformsAsResources;
import static net.sf.opendf.cli.Util.extractPath;

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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.w3c.dom.Node;
import org.w3c.dom.Element;

import net.sf.opendf.cal.i2.Environment;
import net.sf.opendf.cal.i2.Evaluator;
import net.sf.opendf.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.opendf.cal.i2.platform.DefaultTypedPlatform;
import net.sf.opendf.cal.i2.util.Platform;
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
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.hades.des.schedule.SchedulerObserver;
import net.sf.opendf.hades.des.EventProcessor;
import net.sf.opendf.xslt.util.*;

public class PhasedSimulator {

  private String inFile;
  private String outFile;
  private double time;
  private long nSteps;
  private String [] modelPath;
  private String actorClass;
  private String cachePath;
  private Level userVerbosity;
  private boolean elaborate;
  // private boolean debug;
  private boolean interpretStimulus;
  private boolean bufferBlockRecord;
  private int maxErrs;
  private Map<String, String> params;
  
  private boolean elaborated;
  private boolean initialized;
  private boolean done;
  private boolean dusted;
  private boolean failed;
    // A list of string identifiers used in the XML semantic checks to
    // identify issues.  Any id contained in this list will NOT be
    // displayed to the user.  Note that ids are hierarchical, thus a
    // generated message from the check will be tested to see if it
    // _starts_with_ any id from this list.  If so it will be
    // suppressed. 
    private final List<String> suppressIDs = new ArrayList();

  public PhasedSimulator()
  {
    inFile = null;
    outFile = null;
    time = -1;
    nSteps = -1;
    modelPath = null;
    actorClass = null;
    cachePath = null;
    userVerbosity = Logging.user().getLevel();
    elaborate = false;
    // debug = false;
    interpretStimulus = true;
    bufferBlockRecord = false;
    maxErrs = 100;
    params = new HashMap<String, String>();
    
    elaborated = false;
    initialized = false;
    done = false;
    dusted = false;
    failed = false;
  }
  
  // all one token arguments
  public boolean setArg( String arg )
  {
    if( elaborated ) throw new RuntimeException( "Too late to set args" );
    
    if (arg.equals("--no-interpret-stimulus")) interpretStimulus = false;
    else if (arg.equals("-e")) elaborate = true;
    else if (arg.equals("-ea")) System.setProperty("EnableAssertions", "true");
    else if (arg.equals("-tc")) System.setProperty("EnableTypeChecking", "true");
    else if (arg.equals("-bbr"))
    {
      System.setProperty("CalBufferBlockRecord", "true");
      bufferBlockRecord = true;
    } 
    else if (arg.equals("-trace")) System.setProperty("CalFiringTrace", "true");
    else if (arg.equals("-bi")) System.setProperty("CalBufferIgnoreBounds", "true");
    else if (arg.equals("-qq")) Logging.user().setLevel(Level.OFF);
    else if (arg.equals("-q"))
    {
      if (Logging.user().isLoggable(Level.WARNING))
      {
          // If WARNING level is allowed, then make it the
          // least restrictive level.  If it is not, then do
          // NOT further relax to the WARNING level
          Logging.setUserLevel(Level.WARNING);
      }
    } 
    else if (arg.equals("-v")) Logging.user().setLevel(Level.ALL);                
    else if (arg.equals("-debug")) Logging.user().setLevel(Level.ALL);
    else if (arg.equals("-debug0")) Logging.dbg().setLevel(Level.ALL);
    else if (! arg.startsWith("-"))
    {
      if (actorClass != null) return false;
      actorClass = arg;
    } 
    else return false;
    
    return true;
  }

  // all two token arguments
  
  // note: --version not supported here
  
  public boolean setArg( String arg0, String arg1 )
  {

    if( elaborated ) throw new RuntimeException( "Too late to set args" );

    // All numerical arguments
    try
    {
      if( arg0.startsWith( "-n" ))
      {
        if (nSteps >= 0) return false;
        nSteps = Long.parseLong( arg1 );
        return true;
      }
      
      if (arg0.startsWith("-t"))
      {
        if (time >= 0) return false;
        time = Double.parseDouble( arg1 );       
        return true;
      }
      if (arg0.equals("--max-errors"))
      {
        maxErrs = Integer.parseInt(arg1);
        return true;
      } 
    }
    catch( NumberFormatException e ) { return false; }

    if (arg0.equals("-i"))
    {
       if (inFile != null) return false;
       inFile = arg1;
    }
    else if (arg0.equals("-o"))
    {
      if (outFile != null) return false;
      outFile = arg1;
    }
    else if (arg0.equals("-bq")) System.setProperty("CalBufferWarning", arg1);
    else if (arg0.equals("-D")) 
    {
      String s = arg1.trim();
      int n = s.indexOf('=');
      if (n < 0) return false;

      String v = s.substring(0, n).trim();
      if ("".equals(v)) return false;
      String expr = s.substring(n + 1);
      params.put(v, expr);
    }
    else if (arg0.equals("-cache"))
    {
      if (cachePath != null) return false;
      cachePath = arg1;        
    }
    else if (arg0.equals("-mp"))
    {
      if (modelPath != null) return false;
      modelPath = extractPath(arg1);
    }
    else if (arg0.equals("--suppress-message"))
    {
        this.suppressIDs.add(arg1);
    }
    else return false;
    
    return true;
  }
  
	public boolean setArgs( String[] args )
  {
    if( elaborated ) throw new RuntimeException( "Too late to set args" );

	  for (int i = 0; i < args.length; i++ )
	  {
      if( setArg( args[i] ) ) continue;
      
      if( i < args.length - 1 && setArg( args[i], args[i+1] ) )
      {
        i ++;
        continue;
      }
      
      return false;
	  }
	  
	  Logging.user().info(NameString);
	  return true;
  }
	
  private SequentialSimulator sim;

  private OutputStream os;
  
	public boolean elaborate()
	{
    if( elaborated ) throw new RuntimeException( "Already elaborated" );
    elaborated = true;
    
    if (modelPath == null)  modelPath = new String [] {"."};
    Logging.user().info("Model Path: " + Arrays.asList(modelPath));
    
    // Register a listener which will report any issues in loading
    // back to the user.
    XSLTProcessCallbacks.registerListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);
    
    ClassLoader classLoader = new SimulationClassLoader(Simulator.class.getClassLoader(), modelPath, cachePath);

    Platform platform = DefaultTypedPlatform.thePlatform;

    InputStream is = null;
    os = null;
    
    try
    {
      DiscreteEventComponent dec = createDEC(modelPath, actorClass, elaborate, params, classLoader, platform);
      
      if( dec == null )
      {
        failed = true;
        return false;
      }
      
      is = inFile == null ? new NullInputStream() : new FileInputStream(inFile);
      os = outFile == null ? new NullOutputStream() : 
         (".".equals(outFile) ? System.out : new FileOutputStream(outFile));
      SequentialSimulatorCallback callback = interpretStimulus ? new EvaluatedStreamCallback(is, os, platform):new StreamIOCallback(is, os);

      sim = new SequentialSimulator(dec, callback);
    }
    catch (Throwable t)
    {
      ExceptionHandler handler = new ReportingExceptionHandler();
      handler.process(t);
      failed = true;
      // Cleanup
      XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);
      return false;
    }
    
    // No longer needed.
    XSLTProcessCallbacks.removeListener(XSLTProcessCallbacks.SEMANTIC_CHECKS, reportListener);
    
    return true;
	}
	
  private long stepCount;
  private double currentTime;
  private double lastTime;
  private long beginWallclockTime;
  private /* final DBP: hope this doesn't hurt */ SimRuntimeExceptionHandler simExcHandler;
  
	public void initialize()
	{
    if( failed ) throw new RuntimeException( "Simulation has failed" );
    if( ! elaborated ) throw new RuntimeException( "Can't initialize before elaboration" );
    if( initialized ) throw new RuntimeException( "Already initialized" );
    initialized = true;

    simExcHandler = new SimRuntimeExceptionHandler();
    sim.addSchedulerObserver
    (
      new SchedulerObserver()
      {
         public void schedulerException(double now, Exception e)
         {
           simExcHandler.process(e);
         }
         public void schedulerSchedule(double now, double time, double precedence, EventProcessor ep){}
         public void schedulerUnschedule(double now, EventProcessor ep){}
         public void schedulerExecute(double time, double precedence, EventProcessor ep, boolean weak, boolean result){}
      }
    );
        
    stepCount = 0;
    currentTime = 0;
    lastTime = 0;
    
    Logging.user().info("Running...");
    
    beginWallclockTime = System.currentTimeMillis();
	}
	
	public static final int RUNNING = 0;
	public static final int COMPLETED = 1;
	public static final int ERRORLIMIT = 2;
  public static final int FAILED = 3;
	
	public int advanceSimulation( long deltaSteps )
	{
    if( failed ) throw new RuntimeException( "Simulation has failed" );
    if( ! initialized ) throw new RuntimeException( "Can't simulate before initialization" );
    if( done ) throw new RuntimeException( "Already done" );

	  long deltaLimit = deltaSteps < 0 ? -1 : stepCount + deltaSteps;
	  
    try
    {
      while (sim.hasEvent() && 
            (nSteps < 0 || stepCount < nSteps) &&
            (time < 0 || currentTime <= time))
      {
        if( stepCount == deltaLimit ) return RUNNING;
        
        sim.step();
        stepCount += 1;
        lastTime = currentTime;
        currentTime = sim.currentTime();
        if (simExcHandler.getErrorCount() >= maxErrs)
        {
          Logging.user().severe("Too many errors (" + simExcHandler.getErrorCount() + ")");
          long endWallclockTime = System.currentTimeMillis();
          long wcTime = endWallclockTime - beginWallclockTime;
          postSimulationMessage(userVerbosity, bufferBlockRecord, sim, stepCount, lastTime, wcTime);
          
          done = true;
          return ERRORLIMIT;
        }
      }
    }
    catch (Throwable t)
    {
      ExceptionHandler handler = new ReportingExceptionHandler();
      handler.process(t);
      
      done = true;
      failed = true;
      return FAILED;
    }
    
    done = true;
    return COMPLETED;
	}
	
	public void cleanup()
	{
    if( failed ) throw new RuntimeException( "Simulation has failed" );
    if( ! done ) throw new RuntimeException( "Can't clean up before simulation ends" );
    if( dusted ) throw new RuntimeException( "Already cleaned up" );
    dusted = true;
       
    long endWallclockTime = System.currentTimeMillis();
    long wcTime = endWallclockTime - beginWallclockTime;
    try
    {
      if (os != null) os.close();
    }
    catch( Exception e ) {}
        
    postSimulationMessage(userVerbosity, bufferBlockRecord, sim, stepCount, lastTime, wcTime);
	}
  
  private static DiscreteEventComponent createDEC(String[] modelPath, String actorClass, 
      boolean elaborate,
      Map<String, String> params, ClassLoader classLoader, Platform platform)
      throws Exception {
    DiscreteEventComponent dec = null;
        try {
          if (elaborate) {
              Node res = Util.elaborate(actorClass, modelPath, classLoader, params, false, false);
              
//               res = applyTransformsAsResources(res, Elaborator.postElaborationTransformNames);
                
              Logging.user().info("Network successfully elaborated.");
              
              dec = createNetworkDEC(res, classLoader);
          } else {
                DynamicEnvironmentFrame env = new DynamicEnvironmentFrame(platform.createGlobalEnvironment());
                env.bind("__ClassLoader", platform.configuration().convertJavaResult(classLoader), null);
                Evaluator evaluator = new Evaluator(env, platform.configuration());
            
                Map paramValues = new HashMap();
                paramValues.put("__ClassLoader", classLoader);
                for (String var : params.keySet()) {
              
                    Object value = evaluator.valueOf(SourceReader.readExpr(new StringReader((String)params.get(var))));
                        
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
            return null;
        }
        catch (Throwable t)
        {
            ExceptionHandler handler = new ReportingExceptionHandler();
            handler.process(t);
            return null;
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
	
	// private static final String optPath = "path";
	// private static final String optModel = "model";
	
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

    // A listener that is registered to pick up semantic check reports
    // from CAL or NL source reading.  This class allows suppression
    // of any report based on values passed in via --suppress-message
    private final NodeListenerIF reportListener = new NodeListenerIF()
        {
            public void report (Node report, String message)
            {
                try
                {
                    Node reportNode = net.sf.opendf.util.xml.Util.xpathEvalElement("Note[@kind='Report']", report);
                    
                    String severity = ((Element)report).getAttribute("severity");
                    String id = ((Element)report).getAttribute("id");

                    boolean suppress = false;
                    for (String suppressable : PhasedSimulator.this.suppressIDs)
                    {
                        if (id.startsWith(suppressable))
                        {
                            suppress = true;
                            break;
                        }
                    }
                    
                    if (!suppress)
                    {
                        String msg = message + "[" + id +"]";
                        if (severity.toUpperCase().equals("ERROR")) { Logging.user().severe(msg); }
                        else if (severity.toUpperCase().startsWith("WARN")) { Logging.user().warning(msg); }
                        else { Logging.user().info(severity + ": " + msg); }
                    }
                }
                catch (Exception e)
                {
                    Logging.user().severe(message);
                }
            }
        };

    
}

