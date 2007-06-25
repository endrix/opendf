/* 
BEGINCOPYRIGHT X,ETH
	
	Copyright (c) 1999, Computer Engineering and Communication Networks Lab (TIK)
 	                    Swiss Federal Institute of Technology (ETH) Zurich, Switzerland	
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
	- Neither the names of the copyright holders nor the names 
	  of contributors may be used to endorse or promote 
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


package net.sf.caltrop.hades.des;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *  StatData is a basic facility for collecting and transmitting statistical data.
 *  A StatData always comes with each information exchange, it may be null if no stats are
 *  required.
 */

public class StatData implements Serializable {

  private Object    defaultValue = null;
  private Map map;

  protected StatData() {
    map = new HashMap();
  }


  static public  boolean   containsKey(StatData sd, Object key) {
    if (sd == null)
      return false;
    else
      return sd.map.containsKey(key);
  }

  static public  Object    get(StatData sd) {
    return (sd == null) ? null : sd.defaultValue;
  }

  static public  Object    get(StatData sd, Object key) {
    if (sd == null)
      return null;
    else {
      if (sd.map.containsKey(key))
	return sd.map.get(key);
      else
	return null;
    }
  }

  static public  StatData  put(StatData sd, Object d) {

    StatData s;

    if (sd == null)
      s = new StatData();
    else
      s = sd;

    s.defaultValue = d;

    return s;
  }

  static public  StatData  put(StatData sd, Object key, Object d) {

    StatData s;

    if (sd == null)
      s = new StatData();
    else
      s = sd;

    s.map.put(key, d);

    return s;
  }
}



