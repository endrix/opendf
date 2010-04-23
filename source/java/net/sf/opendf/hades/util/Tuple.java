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


package net.sf.opendf.hades.util;


public class Tuple extends java.util.AbstractList{

  private Object [] a;

  public Tuple(Object [] a) { this.a = a; }

  public Tuple(Object a) {
	this (new Object [] {a});
  }

  public Tuple(Object a, Object b) {
	this (new Object [] {a, b});
  }

  public Tuple(Object a, Object b, Object c) {
	this (new Object [] {a, b, c});
  }

  public Tuple(Object a, Object b, Object c, Object d) {
	this (new Object [] {a, b, c, d});
  }

  public Tuple(Object a, Object b, Object c, Object d, Object e) {
	this (new Object [] {a, b, c, d, e});
  }


  //
  //  (unmodifiable) List
  //

  public int size() { return a.length; }
  public Object get(int i) { return a[i]; }

  public boolean equals(Object x) {
	if (x instanceof Tuple) {
	  Tuple t = (Tuple)x;
	  if (a.length != t.a.length) return false;
	  for (int i = 0; i < a.length; i++)
	if (a[i] == null) {
	  if (t.a[i] != null) return false;
	} else {
	  if (!a[i].equals(t.a[i])) return false;
	}
	  return true;
	} else
	  return false;
  }

  public int hashCode() {
	int s = 0;
	for (int i = 0; i < a.length; i++)
	  s += a[i].hashCode();
	return s;
  }

  public String toString() {
	String s = "(";
	for (int i = 0; i < a.length; i++) {
	  if (i != 0) s += ", ";
	  //s += a[i].toString();
	  s += a[i];  // changed by Yan
	}
	s += ")";
	return s;
  }
}


