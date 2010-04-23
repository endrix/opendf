/* 
BEGINCOPYRIGHT X
    
    Copyright (c) 2008, Xilinx Inc.
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

package net.sf.opendf.config;

import net.sf.opendf.config.ConfigGroup;

/**
 * This class defines configuration elements used during simulation.
 * 
 * @author imiller
 *
 */
public class SimulationConfigGroup extends ConfigGroup
{
    
    public SimulationConfigGroup ()
    {
        super();
        
        registerConfig(ENABLE_ASSERTIONS, new ConfigBoolean (ENABLE_ASSERTIONS, "Enable Assertions",
                "-ea", // cla
                "Turns on CAL assertion checking",
                false, // required
                false // default
        ));
        registerConfig(SIM_INPUT_FILE, new ConfigFile(SIM_INPUT_FILE, "Input Stimulus File", 
                "-i",
                "File containing input stimulus for simulation",
                false // required
                ));
        registerConfig(SIM_OUTPUT_FILE, new ConfigFile(SIM_OUTPUT_FILE, "Results File", 
                "-o",
                "Simulation output results file",
                false
                ));
        registerConfig(SIM_TIME, new ConfigInt(SIM_TIME, "Max Sim Time", 
                "-t",
                "Maximum number of simulation ticks (-1 for infinite)",
                false,
                -1
                ));
        registerConfig(SIM_STEPS, new ConfigInt(SIM_STEPS, "Max Steps", 
                "-n",
                "Maximum number of simulation steps (-1 for infinite)",
                false,
                -1
                ));
        registerConfig(SIM_MAX_ERRORS, new ConfigInt(SIM_MAX_ERRORS, "Max Errors", 
                "--max-errors",
                "Maximum number of allowable errors prior to simulation termination",
                false,
                100
                ));
        registerConfig(SIM_INTERPRET_STIMULUS, new ConfigBoolean(SIM_INTERPRET_STIMULUS, "Interpret Stimulus", 
                "--interpret-stimulus",
                "Interpret input stimulus",
                false,
                true
                ));
        registerConfig(SIM_BUFFER_IGNORE, new ConfigBoolean(SIM_BUFFER_IGNORE, "Ignore Buffer Bounds", 
                "-bi",
                "Ignore buffer sizing attributes during simulation",
                false,
                false
                ));
        registerConfig(SIM_BUFFER_RECORD, new ConfigBoolean(SIM_BUFFER_RECORD, "Blocked Buffer Report", 
                "-bbr",
                "Report blocked buffers(queues) during simulation",
                false,
                false
                ));
        registerConfig(SIM_TRACE, new ConfigBoolean(SIM_TRACE, "Simulation trace", 
        		"-trace",
        		"Simulation trace on (deprecated and inactive)",
        		false,
        		false));
        registerConfig(SIM_TRACEFILE, new ConfigFile(SIM_TRACEFILE, "Simulation Trace File", 
                "-tracefile",
                "File to store the trace of simulation execution", 
                false));
        registerConfig(SIM_TYPE_CHECK, new ConfigBoolean(SIM_TYPE_CHECK, "Type Check", 
                "-tc",
                "Perform type checking during simulation",
                false,
                false
                ));
        registerConfig(SIM_BUFFER_SIZE_WARNING, new ConfigInt(SIM_BUFFER_SIZE_WARNING, "Buffer Size Warning", 
                "-bq",
                "Generate warnings if buffer size exceeds specified capacity",
                false,
                -1
                ));
    }

    public ConfigGroup getEmptyConfigGroup ()
    {
        return new SimulationConfigGroup();
    }
    
    @Override
    public ConfigGroup canonicalize ()
    {
        return super.canonicalize();
    }

}
