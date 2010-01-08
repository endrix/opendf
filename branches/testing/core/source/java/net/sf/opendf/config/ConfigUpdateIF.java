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

import java.util.*;

/**
 * This interface is implemented by classes that can be updated from, or
 * pushed to, a {@link ConfigGroup}.
 * 
 * @author imiller
 *
 */
public interface ConfigUpdateIF
{
    /** 
     * Update the {@link ConfigBoolean} from the data in the implementing class
     */
    public void importConfig (ConfigBoolean config);
    /** 
     * Update the {@link ConfigInt} from the data in the implementing class
     */
    public void importConfig (ConfigInt config);
    /** 
     * Update the {@link ConfigList} from the data in the implementing class
     */
    public void importConfig (ConfigList config);
    /** 
     * Update the {@link ConfigMap} from the data in the implementing class
     */
    public void importConfig (ConfigMap config);
    /** 
     * Update the {@link ConfigSet} from the data in the implementing class
     */
    public void importConfig (ConfigSet config);
    /** 
     * Update the {@link ConfigString} from the data in the implementing class
     */
    public void importConfig (ConfigString config);
    
    /** 
     * Update the implementing class from the {@link ConfigBoolean}
     */
    public void exportConfig (ConfigBoolean config);
    /** 
     * Update the implementing class from the {@link ConfigInt}
     */
    public void exportConfig (ConfigInt config);
    /** 
     * Update the implementing class from the {@link ConfigList}
     */
    public void exportConfig (ConfigList config);
    /** 
     * Update the implementing class from the {@link ConfigMap}
     */
    public void exportConfig (ConfigMap config);
    /** 
     * Update the implementing class from the {@link ConfigSet}
     */
    public void exportConfig (ConfigSet config);
    /** 
     * Update the implementing class from the {@link ConfigString}
     */
    public void exportConfig (ConfigString config);
        
    
}
