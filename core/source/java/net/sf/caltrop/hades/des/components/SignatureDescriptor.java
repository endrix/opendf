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


package net.sf.caltrop.hades.des.components;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
    A SignatureDiscriptor provides structured data describing the I/O signature of a component.
    
    The component may either be a proper DEC, or it may be an interface, essentially a subclass
    of Interface, which is never instantiated but simply defines an I/O signature.
    
    A signature may contain atomic connectors and also named sub-signatures which are contained 
    in interfaces.
    
    @see    InterfaceDescriptor
    @see    AtomicConnectorDescriptor
    
*/

public class SignatureDescriptor implements SignatureConstants {


  public static final String hierarchySeparator = ".";
    
  //
  // vars
  //
    
  protected Set         atomicConnectors = new HashSet();
  protected Set         interfaces = new HashSet();
    
  protected Class	signatureClass;

  protected String	prefix;
  protected boolean	inverted;

  protected int         signatureVersion;


  //
  // aux
  //
    
  private void    addConnector(String nm, int dir, Class type) {
	
    atomicConnectors.add(new AtomicConnectorDescriptor(nm, dir, type));
  }
        
  protected void  addInterface(String nm, Class interfaceClass, boolean inv) {
    	
    try {	    
      ComponentDescriptor cd = new ComponentDescriptor(interfaceClass, 
						       prefix + nm + hierarchySeparator, 
						       inv ^ inverted);
      if (cd.isInterface())
	interfaces.add(new InterfaceDescriptor(nm, cd.getSignature(), inv));
      else
	throw new RuntimeException("Class '" + interfaceClass.getName() + "' is not an interface.");
    }
    catch (Exception e) { e.printStackTrace(); throw new RuntimeException(e.getMessage()); }
  }
    
  //
  //  public access
  //

  public int            getSignatureVersion() { return signatureVersion;  }
 
  public Class	        getSignatureClass() { return signatureClass; }

  public String	        getPrefix()             { return prefix; }
  public boolean	isInverted()	{ return inverted; }

  public Set            getResolvedAtomicConnectors() {
    Set s = new HashSet();
    addResolvedConnectors(s);
    return s;
  }

  public Set            getAllAtomicConnectors() {
    Set s = getResolvedAtomicConnectors();

    return s;
  }

  //
  // aux
  //
    
  private void    addResolvedConnectors(Set s) {
	
    addConnectors(s);
  }
    

  private void    addConnectors(Set s) {
    s.addAll(atomicConnectors);
  }

  //
  //  convenience
  //

  public Set    getAllAtomicInputConnectorNames() {
    Set s = new HashSet();
    for (Iterator i = getAllAtomicConnectors().iterator(); i.hasNext(); ) {
      AtomicConnectorDescriptor ac = (AtomicConnectorDescriptor)i.next();
      if (ac.isInput())
	s.add(ac.getName());
    }
    return s;
  }

  public Set    getAllAtomicOutputConnectorNames() {
    Set s = new HashSet();
    for (Iterator i = getAllAtomicConnectors().iterator(); i.hasNext(); ) {
      AtomicConnectorDescriptor ac = (AtomicConnectorDescriptor)i.next();
      if (ac.isOutput())
	s.add(ac.getName());
    }
    return s;
  }


  //
  //  ctor
  //


    
  public SignatureDescriptor(Class c) {
    this(c, "", false);
  }
    
  public SignatureDescriptor(Class c, String prefix, boolean inv) {
    
    signatureClass = c;
    this.prefix = prefix;
    inverted = inv;

    Method sigver = null;

    Object v;
    try {
      sigver = signatureClass.getMethod(signatureVersionMethod, new Class [0]);
      v = sigver.invoke(null, new Object [0]);
    }
    catch (Exception e) {
      readSignatureV0();
      return;
    }

    signatureVersion = ((Integer)v).intValue();
    switch (signatureVersion) {
    case 1:
      readSignatureV1();
      break;
    default:
      throw new RuntimeException("Unknown signature version " + signatureVersion + ".");
    }
  }

  //
  // signature reader
  //

  private void   readSignatureV1() {
    
    try {
      Method m = signatureClass.getMethod(atomicConnectorsMethod, new Class [0]);
      atomicConnectors = (Set)m.invoke(null, new Object [0]);
    }
    catch (Exception e) {
      atomicConnectors = new HashSet();
    }

    try {
      Method m = signatureClass.getMethod(interfacesMethod, new Class [0]);
      interfaces = (Set)m.invoke(null, new Object [0]);
    }
    catch (Exception e) {
      interfaces = new HashSet();
    } 
  }

  private final static String signatureVersionMethod = "getComponentSignatureVersion";
  private final static String atomicConnectorsMethod = "getComponentAtomicConnectors";
  private final static String interfacesMethod = "getComponentInterfaces";



  private void   readSignatureV0() {
    
    signatureVersion = 0;

    Method [] methods = signatureClass.getMethods();
    for (int i = 0; i < methods.length; i++) {
      String s = methods[i].getName();
      if (s.startsWith(inputPrefix) && methods[i].getParameterTypes().length == 0) {
	String nm = s.substring(inputPrefix.length());
	if (nm.length() > 0)
	  addConnector(nm, AtomicConnectorDescriptor.directionInput, methods[i].getReturnType());
      }
      if (s.startsWith(outputPrefix) && methods[i].getParameterTypes().length == 0) {
	String nm = s.substring(outputPrefix.length());
	if (nm.length() > 0)
	  addConnector(nm, AtomicConnectorDescriptor.directionOutput, methods[i].getReturnType());
      }
      if (s.startsWith(interfacePrefix) && methods[i].getParameterTypes().length == 0) {
	String nm = s.substring(interfacePrefix.length());
	if (nm.length() > 0)
	  addInterface(nm, methods[i].getReturnType(), false); // YYY
      }
    }
  }

  private static final String	inputPrefix = "input";
  private static final String	outputPrefix = "output";
  private static final String interfacePrefix = "interface";
  private static final String invertedPrefix = "inverted";

}





