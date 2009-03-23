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


package net.sf.opendf.hades.des.components;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
    A ComponentDescriptor object provides information about a given DEC class. In this way,
    the I/O signature of component classes may be analyzed without instantiating them, provided
    that the DEC conforms to a standardized naming scheme ('design pattern').
    
    The I/O signature identifies the input and output connectors of a DEC and optionally their
    types (i.e. the types of the tokens flowing across these connectors). Each connector is
    defined by a ConnectorDescriptor.
    
    @see    SignatureDescriptor
    @see    net.sf.opendf.hades.des.DiscreteEventComponent
    
*/

public class ComponentDescriptor {

  private static final String componentRootName = "net.sf.opendf.hades.des.DiscreteEventComponent";
  private static final String interfaceRootName = "net.sf.opendf.hades.des.components.Interface";

  private static Class componentRootClass = net.sf.opendf.hades.des.DiscreteEventComponent.class;
  private static Class interfaceRootClass = net.sf.opendf.hades.des.components.Interface.class;

  private Class		componentClass;
  private SignatureDescriptor	signature;
    
  private ParameterDescriptor	[] parameters;
    
  private Constructor		defaultCtor = null;
  private Constructor		parameterCtor = null;

    
  //
  //  aux
  //
    
  //
  //  public
  //

  public boolean isDEC() {
    return this.isComponent();
  }

  public boolean isComponent() {
    return componentRootClass.isAssignableFrom(componentClass);
  }
    
  public boolean isInterface() {
    return interfaceRootClass.isAssignableFrom(componentClass);
  }
    
  public String getClassName() {
    return componentClass.getName();
  }

  public SignatureDescriptor	getSignature() {
    return signature;
  }
    
  public ParameterDescriptor [] getParameters() {
    return parameters;
  }
    
  public Constructor	getDefaultCtor() {
    return defaultCtor;
  }
    
  public Constructor	getParameterCtor() {
    return parameterCtor;
  }

  //
  //  ctor
  //
    
  public ComponentDescriptor(Class c) {
    this(c, "", false);
  }
    
  public ComponentDescriptor(Class c, String prefix, boolean inv) {
	
    componentClass = c;
	
    if (isDEC() || isInterface()) {
      signature = new SignatureDescriptor(componentClass, prefix, inv);
    }
	
    if (isDEC()) {
	
      try {
	Method parameterMethod = componentClass.getMethod(parameterMethodName, new Class[0]);
	parameters = (ParameterDescriptor []) parameterMethod.invoke(null, new Object[0]);
      }
      catch (Exception e) { parameters = new ParameterDescriptor[0]; }
	    
      try { defaultCtor = componentClass.getConstructor(new Class[0]); }
      catch (Exception e) {}
	    
      try {
	Class [] pars = new Class[] { Object.class };
	parameterCtor = componentClass.getConstructor(pars);
      }
      catch (Exception e) { }
    }
  }

  private final static String parameterMethodName = "getComponentParameters";



  //
  // command line interface
  //

  private static void printUsage() {
    System.err.println("<java> net.sf.opendf.hades.des.components.ComponentDecsriptor <fullclassname>");
  }

  private String format() {
    if ( (!this.isComponent()) && (!this.isInterface()) ) 
      return this.getClassName() + ": No component or interface class.\n";

    String s = this.getClassName() + ": [" + (this.isInterface() ? "Interface" : "Component") + "]\n";

    if (this.isComponent()) {
      ParameterDescriptor [] pd = this.getParameters();
      if (pd != null) {
	s += "\tParameters:\n";
	
	for (int i = 0; i < pd.length; i++) {
	  s += "\t\t" + pd[i].getName() + ": "
                      + ((pd[i].getType() == null) ? "<undef>" : pd[i].getType().getName()) 
	              + " [" + pd[i].getDefault() + "]\n";
	}
      } else
	s += "\tNo parameters.\n";
    }

    SignatureDescriptor sd = this.getSignature();
    s += "\tSignature [v" + sd.getSignatureVersion() + "]\n";

    s += "\t\tInputs:\n";
    for (Iterator i = sd.getAllAtomicConnectors().iterator(); i.hasNext(); ) {
      AtomicConnectorDescriptor acd = (AtomicConnectorDescriptor)i.next();
      if (acd.isInput())
	s += "\t\t\t" + acd.getName() + ": " + acd.getType().getName() + "\n";
    }

    s += "\t\tOutputs:\n";
    for (Iterator i = sd.getAllAtomicConnectors().iterator(); i.hasNext(); ) {
      AtomicConnectorDescriptor acd = (AtomicConnectorDescriptor)i.next();
      if (acd.isOutput())
	s += "\t\t\t" + acd.getName() + ": " + acd.getType().getName() + "\n";
    }

    return s;

  }


  public static void main(String [] args) {
    
    String s = "";
    
    if (args.length != 1) {
      printUsage();
      return;
    }
    
    String name = args[0];
    try {
      Class c = Class.forName(name);

      ComponentDescriptor cd = new ComponentDescriptor(c);
      s += cd.format();
    }
    catch(ClassNotFoundException e) {
      s += name + ": Cannot find class.\n";
    }
    catch(Exception e) { e.printStackTrace(); }
    
    System.out.println(s);
  }
}






