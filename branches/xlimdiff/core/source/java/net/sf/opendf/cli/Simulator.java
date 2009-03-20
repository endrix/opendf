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
import java.util.logging.Level;

import net.sf.opendf.config.*;
import net.sf.opendf.config.AbstractConfig.ConfigError;
import net.sf.opendf.util.logging.Logging;

public class Simulator
{  
  public static void main(String [] args)
  {
      ConfigGroup configuration = new SimulationConfigGroup();
      List<String> unparsed = ConfigCLIParseFactory.parseCLI(args, configuration);
      
      if (((ConfigBoolean)configuration.get(ConfigGroup.VERSION)).getValue().booleanValue())
      {
          configuration.usage(Logging.user(), Level.INFO);
          return;
      }

      ConfigString topName = (ConfigString)configuration.get(ConfigGroup.TOP_MODEL_NAME);
      if (!topName.isUserSpecified())
      {
          // Take the first unparsed arg with no leading '-'.
          for (String arg : new ArrayList<String>(unparsed))
          {
              if (!arg.startsWith("-"))
              {
                  unparsed.remove(arg);
                  topName.setValue(arg, true);
              }
          }
      }
      
        
      boolean valid = unparsed.isEmpty();
      for (AbstractConfig cfg : configuration.getConfigs().values())
      {
          if (!cfg.validate())
          {
              for (ConfigError err : cfg.getErrors())
              {
                  Logging.user().severe(err.getMessage());
                  valid = false;
              }
          }
      }
      
      if (!valid)
      {
          Logging.user().info("Unknown args: " + unparsed);
          configuration.usage(Logging.user(), Level.INFO);
          return;
      }
      
          
    PhasedSimulator simulator = new PhasedSimulator(configuration);

    if( ! simulator.elaborate() ) System.exit(-1);
    
    simulator.initialize();

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

