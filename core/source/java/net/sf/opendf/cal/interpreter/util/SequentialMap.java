/* 
BEGINCOPYRIGHT X,UC
	
	Copyright (c) 2007, Xilinx Inc.
	Copyright (c) 2003, The Regents of the University of California
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

package net.sf.caltrop.cal.interpreter.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A SequentialMap works very much like an ordinary HashMap (which in fact it encapsulates), and adds
 * the ability to retrieve a list of its keys in the order in which they were added to the map.
 *
 * @see HashMap
 *
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class SequentialMap implements Map {

    //
    //  Map
    //

    public void clear() {
        mappings.clear();
        keyList.clear();
    }

    public boolean containsKey(Object key) {
        return mappings.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return mappings.containsValue(value);
    }

    public Set entrySet() {
        return mappings.entrySet();
    }

    public Object get(Object key) {
        return mappings.get(key);
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    public Set keySet() {
        return mappings.keySet();
    }

    public Object put(Object key, Object value) {
        if (keyList.contains(key)) {
            assert mappings.containsKey(key);

            keyList.remove(key);
        }

        keyList.add(key);
        return mappings.put(key, value);
    }

    public void putAll(Map t) {
        for (Iterator i = t.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            this.put(e.getKey(), e.getValue());
        }
    }

    public Object remove(Object key) {
        if (keyList.contains(key)) {
            assert mappings.containsKey(key);

            keyList.remove(key);
        }
        return mappings.remove(key);
    }

    public int size() {
        return mappings.size();
    }

    public Collection values() {
        return mappings.values();
    }

    //
    //  SequentialMap
    //

    public List  keyList() {
        return new ArrayList(keyList);
    }


    //
    //  ctor
    //

    public SequentialMap() {
    }

    public SequentialMap(Map m) {
        this.putAll(m);
    }


    //
    // private
    //

    private Map   mappings = new HashMap();
    private List  keyList = new ArrayList();
}
