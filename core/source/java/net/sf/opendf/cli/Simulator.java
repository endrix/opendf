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

import java.util.*;

// import net.sf.opendf.xslt.util.*;
// import net.sf.opendf.util.logging.Logging;
// import org.w3c.dom.*;

public class Simulator
{  
  public static void main(String [] args)
  {
    PhasedSimulator simulator = new PhasedSimulator();

    for( int i=0; i<args.length; i++ )
      if (args[i].equals("--version"))
      { 
        VersionInfo.printVersion();
        System.exit( 0 );
      }

    if( ! simulator.setArgs(args) )
    { 
      usage();
      System.exit(-1);
    }

//     final List<String> suppressIDs = new ArrayList();
//     suppressIDs.add("priorityChecks.priorityQID.timingDependent");
//     ProblemListenerIF reportListener = new ProblemListenerIF()
//         {
//             public void report (Node report, String message)
//             {
//                 try
//                 {
//                     Node reportNode = net.sf.opendf.util.xml.Util.xpathEvalElement("Note[@kind='Report']", report);
                    
//                     String severity = ((Element)report).getAttribute("severity");
//                     String id = ((Element)report).getAttribute("id");

//                     boolean suppress = false;
//                     for (String suppressable : suppressIDs)
//                     {
//                         if (suppressable.startsWith(id))
//                         {
//                             suppress = true;
//                             break;
//                         }
//                     }
                    
//                     if (!suppress)
//                     {
//                         if (severity.toUpperCase().equals("ERROR"))
//                         {
//                             Logging.user().severe("IDM"+message);
//                         }
//                         else if (severity.toUpperCase().startsWith("WARN"))
//                         {
//                             Logging.user().warning("IDM"+message);
//                         }
//                         else
//                         {
//                             Logging.user().info("IDM"+severity + ": " + message);
//                         }
//                     }
//                 }
//                 catch (Exception e)
//                 {
//                     Logging.user().info("IDM"+" " + message);
//                 }
//             }
//         };

//     // Register a listener which will report any issues in loading
//     // back to the user.
//     XSLTProcessCallbacks.registerProblemListener(reportListener);
    
    if( ! simulator.elaborate() ) System.exit(-1);
    
    simulator.initialize();

//     // No longer needed.
//     XSLTProcessCallbacks.removeProblemListener(reportListener);
    
    int result = simulator.advanceSimulation( -1 );
    
    if( result == PhasedSimulator.FAILED ) System.exit(-1);
    
    simulator.cleanup();
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
        System.out.println("  --version           Display Version information and exit");
		System.exit(-1);
	}
}

