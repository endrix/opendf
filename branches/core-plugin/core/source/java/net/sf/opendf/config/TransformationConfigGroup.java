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

import java.io.File;

import net.sf.opendf.config.ConfigGroup;

/**
 * This class defines configuration elements used during XSLT Transformation processes.
 * 
 * @author imiller
 *
 */
public class TransformationConfigGroup extends ConfigGroup
{
    
    public TransformationConfigGroup ()
    {
        super();

        registerConfig(XSLT_PRESERVE_INTERMEDIATE, new ConfigBoolean (XSLT_PRESERVE_INTERMEDIATE, 
                "Preserve Intermediates",
                "--preserve", // cla
                "Preserve significant intermediate xml files in multi-stage transformation",
                false, // required
                false // default
        ));
    }

    public ConfigGroup getEmptyConfigGroup ()
    {
        return new TransformationConfigGroup();
    }
    
    @Override
    public ConfigGroup canonicalize ()
    {
        ConfigGroup canon = super.canonicalize();
        
        String runDir = ((ConfigFile)canon.get(ConfigGroup.RUN_DIR)).getValue();
        if ("".equals(runDir))
        {
            String abs = (new File(System.getProperty("user.dir"))).getAbsolutePath();
            canon.get(ConfigGroup.RUN_DIR).setValue(abs, false);
            runDir = ((ConfigFile)canon.get(ConfigGroup.RUN_DIR)).getValue();
        }

        // Make the top file absolute
        ConfigFile topFile = (ConfigFile)canon.get(ConfigGroup.TOP_MODEL_FILE);
        if (!topFile.getValueFile().isAbsolute())
        {
            File abs = new File(runDir, topFile.getValue());
            topFile.setValue(abs, topFile.isUserSpecified());
        }
        
        ConfigString topName = (ConfigString)canon.get(ConfigGroup.TOP_MODEL_NAME);
        if (!topName.isUserSpecified() && topFile.isUserSpecified())
        {
            topName.setValue(topFile.getValueFile().getName(), true);
        }

        return canon;
    }

}
