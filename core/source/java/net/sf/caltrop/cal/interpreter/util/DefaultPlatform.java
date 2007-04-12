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


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.InetAddress;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.Function;
import net.sf.caltrop.cal.interpreter.InterpreterException;
import net.sf.caltrop.cal.interpreter.Procedure;
import net.sf.caltrop.cal.interpreter.environment.Environment;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;


/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class DefaultPlatform implements Platform {
	
	public final static Platform thePlatform = new DefaultPlatform();
	public final static Context  theContext = thePlatform.context();
	public final static Environment theDefaultEnvironment = thePlatform.createGlobalEnvironment();
	
    public Context context() {
        return DefaultContext.theContext;
    }

    public Environment createGlobalEnvironment() {
        return this.createGlobalEnvironment(null);
    }
    
    public ImportHandler []  getImportHandlers(ClassLoader loader) {
		return new ImportHandler [] {
				new EnvironmentFactoryImportHandler(this),
				new CalScriptImportHandler(this),
				new ClassLoadingImportHandler(this, loader)
		};
	}
	
	public ImportMapper []  getImportMappers() {
		return new ImportMapper [] {
				new ReplacePrefixImportMapper(new String [] {"caltrop", "lib"}, 
											  new String [] {"net", "sf", "caltrop", "cal", "lib"})
		};
    }

    /*
     * A map of allocated Sockets, used by the socketGetToken and
     * socketPutToken calls.
     */
    private Map allocatedSockets = new HashMap();
    private Socket getSocket (String ipAddr, int socketNum)
    {
        String key = ipAddr + ":" + socketNum;
        Socket socket = (Socket)allocatedSockets.get(key);
        if (socket == null)
        {
            try
            {
                socket = new Socket(InetAddress.getByName(ipAddr), socketNum);
            }
            catch (UnknownHostException uhe){
                throw new IllegalArgumentException("Could not resolve the host " + key + ".  Exception creating socket " + uhe.getMessage());
            } catch (IOException ioe){
                throw new IllegalArgumentException("IO Exception opening socket for " + key + ".  Exception creating socket " + ioe.getMessage());
            }
            allocatedSockets.put(key, socket);
        }
        return socket;
    }

    private int argToInt (Object o)
    {
        if (o instanceof String)
            return Integer.parseInt((String)o);
        
        return context().intValue(o);
    }
    
    public Environment createGlobalEnvironment(Environment parent) {

        Environment env = context().newEnvironmentFrame(parent);
        
        /*
         * socketGetToken("x.x.x.x", socket, numBytes)
         * Returns the specified number of bytes read from the stream
         * in an integer.  The last byte received is in the LSB.  
         */
        env.bind("socketGetToken", context().createFunction(new Function() {
                public Object apply (Object[] args) 
                {
                    String address = context().stringValue(args[0]);
                    int socketNum = argToInt(args[1]);
                    int bytes = argToInt(args[2]);
                    Socket socket = DefaultPlatform.this.getSocket(address, socketNum);
                    if (bytes < 1 || bytes > 4)
                        throw new IllegalArgumentException("socketGetToken must retrieve 1-4 bytes.  "+bytes+" requested");
                    int value = 0;
                    try
                    {
                        for (int i=0; i < bytes; i++)
                            value = (value << 8) | (socket.getInputStream().read() & 0xFF);
                    } catch (IOException ioe){
                        throw new IllegalArgumentException("IO Exception reading from stream " + address + ":" + socketNum +".  Exception reading from socket " + ioe.getMessage());
                    }
                    
                    

                    //return context().createInteger( context().intValue( value ) );
                    return context().createInteger( context().intValue( new Integer(value) ) );
                }
                
                public int arity () { return 1; }
            }));

        /*
         * socketPutToken("x.x.x.x", socket, numBytes, value)
         * Returns the specified number of bytes read from the stream
         * in an integer.  The last byte received is in the LSB.  
         */
        env.bind("socketPutToken", context().createProcedure(new Procedure() {
                public void call (Object[] args)
                {
                    String address = context().stringValue(args[0]);
                    int socketNum = argToInt(args[1]);
                    int bytes = argToInt(args[2]);
                    int value = argToInt(args[3]);
                    Socket socket = DefaultPlatform.this.getSocket(address, socketNum);
                    if (bytes < 1 || bytes > 4)
                        throw new IllegalArgumentException("socketPutToken must send 1-4 bytes.  "+bytes+" requested");
                    try
                    {
                        OutputStream outStream = socket.getOutputStream();
                        for (int i=(bytes-1); i >= 0; i--)
                        {
                            // The last byte written is to be the LSB (to
                            // match socketGetToken).  write ignores the
                            // MSB 24 bits.
                            outStream.write(value >> (8 * i));
                        }
                        outStream.flush();
                    } catch (IOException ioe) {
                        throw new IllegalArgumentException("IO Exception writing to stream " + address + ":" + socketNum +".  Exception writing to socket " + ioe.getMessage());
                    }
                }
                
                public int arity () { return 1; }
            }));

        
        
        env.bind("print", context().createProcedure(new Procedure() {
            public void call(Object[] args) {
                System.out.print(args[0]);
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("println", context().createProcedure(new Procedure() {
            public void call(Object[] args) {
                System.out.println(args[0]);
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("sin", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                return context().createReal( java.lang.Math.sin( context().realValue(args[0]) ) );
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("cos", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                return context().createReal( java.lang.Math.cos( context().realValue(args[0]) ) );
            }

            public int arity() {
                return 1;
            }
        }));
       
        env.bind("int", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                return context().createInteger( context().intValue( args[0] ) );
            }

            public int arity() {
                return 1;
            }
        }));
        
        env.bind("SOP", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        System.out.println(args[0]);
                        return args[0];
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$not': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("Integers", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        int a = context().intValue(args[0]);
                        int b = context().intValue(args[1]);
                        List res = (b < a) ? Collections.EMPTY_LIST : new IntegerList(context(), a, b);
                        return context().createList(res);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function 'Integers': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));
        
        env.bind("toInt", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    return context().createInteger(context().intValue(args[0]));
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'toInt': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("$domain", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	Map m = context().getMap(args[0]);
                	return context().createSet(m.keySet());
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$domain': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("$not", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    return context().createBoolean(!context().booleanValue(args[0]));
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$not': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("$and", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context().createBoolean(context().booleanValue(args[0])
                                                    && context().booleanValue(args[1]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$and': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$or", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context().createBoolean(context().booleanValue(args[0])
                                                    || context().booleanValue(args[1]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$or': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$eq", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context().isNull(args[0])) {
                		return context().createBoolean(context().isNull(args[1]));
                	} else {
                		return context().createBoolean(args[0].equals(args[1]));
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$eq': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$ne", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context().isNull(args[0])) {
                		return context().createBoolean(!context().isNull(args[1]));
                	} else {
                		return context().createBoolean(!args[0].equals(args[1]));
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$ne': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$lt", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Comparable a = (Comparable)args[0];
                        Comparable b = (Comparable)args[1];
                        if (a instanceof Integer && b instanceof Integer) {
                            return context().createBoolean(a.compareTo(b) < 0);                        	
                        } else if (a instanceof Number && b instanceof Number) {
                        	return context().createBoolean(((Number)a).doubleValue() < ((Number)b).doubleValue());
                        } else
                        	return context().createBoolean(a.compareTo(b) < 0);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$lt': Cannot apply (to " + args[0] + " and " + args[1] + ").", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$le", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Comparable a = (Comparable)args[0];
                        Comparable b = (Comparable)args[1];
                        if (a instanceof Integer && b instanceof Integer) {
                            return context().createBoolean(a.compareTo(b) <= 0);                        	
                        } else if (a instanceof Number && b instanceof Number) {
                        	return context().createBoolean(((Number)a).doubleValue() <= ((Number)b).doubleValue());
                        } else
                        	return context().createBoolean(a.compareTo(b) <= 0);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$le': Cannot apply (to " + args[0] + " and " + args[1] + ").", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$gt", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Comparable a = (Comparable)args[0];
                        Comparable b = (Comparable)args[1];
                        if (a instanceof Integer && b instanceof Integer) {
                            return context().createBoolean(a.compareTo(b) > 0);                        	
                        } else if (a instanceof Number && b instanceof Number) {
                        	return context().createBoolean(((Number)a).doubleValue() > ((Number)b).doubleValue());
                        } else
                        	return context().createBoolean(a.compareTo(b) > 0);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$gt': Cannot apply (to " + args[0] + " and " + args[1] + ").", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$ge", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Comparable a = (Comparable)args[0];
                        Comparable b = (Comparable)args[1];
                        if (a instanceof Integer && b instanceof Integer) {
                            return context().createBoolean(a.compareTo(b) >= 0);                        	
                        } else if (a instanceof Number && b instanceof Number) {
                        	return context().createBoolean(((Number)a).doubleValue() >= ((Number)b).doubleValue());
                        } else
                        	return context().createBoolean(a.compareTo(b) >= 0);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$ge': Cannot apply (to " + args[0] + " and " + args[1] + ").", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$negate", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Number n = (Number)args[0];
                        if (n instanceof Integer)
                            return new Integer(-n.intValue());
                        else if (n instanceof Double)
                            return new Double(-n.doubleValue());
                        else if (n instanceof Float)
                            return new Float(-n.floatValue());
                        else if (n instanceof Long)
                            return new Long(-n.longValue());
                        else if (n instanceof Short)
                            return new Integer(-n.shortValue());
                        else if (n instanceof BigInteger)
                            return ((BigInteger)n).negate();
                        else if (n instanceof BigDecimal)
                            return ((BigDecimal)n).negate();
                        else
                            throw new RuntimeException("Not a scalar: " + n);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$negate': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("$add", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Object a = args[0];
                        Object b = args[1];
                        if (a instanceof String || b instanceof String) {
                            return "" + a + b;
                        } else if (a instanceof Number) {
                            if (! (b instanceof Number))
                                throw new RuntimeException("Cannot add to number: " + b);

                            // FIXME: handle big integers
                            if ( (a instanceof Short || a instanceof Integer || a instanceof Long)
                                 && (b instanceof Short || b instanceof Integer || b instanceof Long)) {

                                int va = ((Number)a).intValue();
                                int vb = ((Number)b).intValue();
                                int res = va + vb;
                                return new Integer(res);
                            } else {                            	
                                double va = ((Number)a).doubleValue();
                                double vb = ((Number)b).doubleValue();
                                return new Double(va + vb);
                            }
                        } else if (a instanceof List) {
                            if (b instanceof List) {
                                List res = new ArrayList((List)a);
                                res.addAll((List)b);
                                return res;
                            } else if (b instanceof Collection) {
                                Set res = new HashSet((List)a);
                                res.addAll((Collection)b);
                                return res;
                            } else
                                throw new RuntimeException("Cannot add to list: " + b);
                        } else if (a instanceof Collection) {
                            if (b instanceof Collection) {
                                Set res = new HashSet((List)a);
                                res.addAll((Collection)b);
                                return res;
                            } else
                                throw new RuntimeException("Cannot add to collection: " + b);
                        } else if (a instanceof Map) {
                            if (b instanceof Map) {
                                Map res = new HashMap((Map)a);
                                res.putAll((Map)b);
                                return res;
                            } else
                                throw new RuntimeException("Cannot add to map: " + b);
                        } else {
                            throw new RuntimeException("Cannot add to: " + a);
                        }
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$add': Cannot apply (to " + args[0] + " and " + args[1] + ").", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$mul", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object a = args[0];
                    Object b = args[1];
                    if (a instanceof Number) {
                        if (! (b instanceof Number))
                            throw new RuntimeException("Cannot multiply with number: " + b);

                        // FIXME: handle big integers
                        if ( (a instanceof Short || a instanceof Integer || a instanceof Long)
                                && (b instanceof Short || b instanceof Integer || b instanceof Long)) {

                            int va = ((Number)a).intValue();
                            int vb = ((Number)b).intValue();
                            int res = va * vb;

                            return new Integer(res);
                        } else {
                            double va = ((Number)a).doubleValue();
                            double vb = ((Number)b).doubleValue();
                            return new Double(va * vb);
                        }
                    } else if (a instanceof Collection) {
                        if (b instanceof Collection) {
                            Set res = new HashSet((Collection)a);
                            res.retainAll((Collection)b);
                            return res;
                        } else
                            throw new RuntimeException("Cannot multiply with collection: " + b);
                    } else if (a instanceof Map) {
                        if (b instanceof Map) {
                            Map m1 = (Map)a;
                            Map m2 = (Map)b;
                            Map res = new HashMap();

                            for(Iterator i = m1.keySet().iterator(); i.hasNext(); ) {
                                Object k = i.next();
                                Object v1 = m1.get(k);
                                Object v2 = m2.get(v1);
                                res.put(k, v2);
                            }
                           return res;
                        } else if (b instanceof Function) {
                            Map m1 = (Map)a;
                            Function f = (Function)b;
                            Map res = new HashMap();

                            Object [] arg = new Object [1];
                            for(Iterator i = m1.keySet().iterator(); i.hasNext(); ) {
                                Object k = i.next();
                                arg[0] = m1.get(k);
                                Object v = f.apply(arg);
                                res.put(k, v);
                            }
                           return res;
                        } else
                            throw new RuntimeException("Cannot multiply with map: " + b);
                    } else if (a instanceof Function) {
                        if (b instanceof Function) {
                            return new FunctionComposition ((Function)a, (Function)b);
                        } else
                            throw new RuntimeException("Cannot multiply with function: " + b);
                    }  else {
                        throw new RuntimeException("Cannot multiply: " + a);
                    }
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$mul': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }

        }));

        env.bind("$sub", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object a = args[0];
                    Object b = args[1];
                    if (a instanceof Number) {
                        if (! (b instanceof Number))
                            throw new RuntimeException("Cannot subtract from number: " + b);

                        // FIXME: handle big integers
                        if ( (a instanceof Short || a instanceof Integer || a instanceof Long)
                             && (b instanceof Short || b instanceof Integer || b instanceof Long)) {

                            int va = ((Number)a).intValue();
                            int vb = ((Number)b).intValue();
                            int res = va - vb;
                            return new Integer(res);
                        } else {
                            double va = ((Number)a).doubleValue();
                            double vb = ((Number)b).doubleValue();
                            return new Double(va - vb);
                        }
                    } else if (a instanceof Collection) {
                        if (b instanceof Collection) {
                            Set res = new HashSet((Collection)a);
                            res.removeAll((Collection)b);
                            return res;
                        } else
                            throw new RuntimeException("Cannot subtract from collection: " + b);
                    } else if (a instanceof Map) {
                        if (b instanceof Collection) {
                            Map res = new HashMap((Map)a);
                            for (Iterator i = ((Collection)b).iterator(); i.hasNext(); ) {
                                Object k = i.next();
                                res.remove(k);
                            }
                            return res;
                        } else
                            throw new RuntimeException("Cannot subtract from map: " + b);
                    } else {
                        throw new RuntimeException("Cannot subtract from: " + a);
                    }
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$sub': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$div", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object a = args[0];
                    Object b = args[1];
                    if (a instanceof Number) {
                        if (! (b instanceof Number))
                            throw new RuntimeException("Cannot divide number: " + b);

                        // FIXME: handle big integers
                        if ( (a instanceof Short || a instanceof Integer || a instanceof Long)
                             && (b instanceof Short || b instanceof Integer || b instanceof Long)) {

                            int va = ((Number)a).intValue();
                            int vb = ((Number)b).intValue();
                            if (! (va % vb == 0)) {
//                              FIXME: behave like the div operator for testing
//                                return new Double(((double)va) / ((double)vb));
                            	return new Integer(va/vb);
                            }
                            int res = va / vb;
                            return new Integer(res);
                        } else {
                            double va = ((Number)a).doubleValue();
                            double vb = ((Number)b).doubleValue();
                            return new Double(va / vb);
                        }
                    } else if (a instanceof Map) {
                        if (b instanceof Collection) {
                            Map res = new HashMap((Map)a);
                            for (Iterator i = ((Map)a).keySet().iterator(); i.hasNext(); ) {
                                Object k = i.next();
                                if (! ((Collection)b).contains(k))
                                    res.remove(k);
                            }
                            return res;
                        } else
                            throw new RuntimeException("Cannot constrain map: " + b);
                    } else {
                        throw new RuntimeException("Cannot be divided/constrained: " + a);
                    }
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$div': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$mod", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object a = args[0];
                    Object b = args[1];
                    // FIXME: handle big integers
                    if ( (a instanceof Short || a instanceof Integer || a instanceof Long)
                            && (b instanceof Short || b instanceof Integer || b instanceof Long)) {

                        int va = ((Number)a).intValue();
                        int vb = ((Number)b).intValue();
                        int res = va % vb;

                        return new Integer(res);
                    } else
                        throw new RuntimeException("Arguments to mod need be integral scalars.");
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$mod': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$size", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object s = args[0];
                    if (s instanceof Collection) {
                        return context().createInteger(((Collection)s).size());
                    } else if (s instanceof Map) {
                        return context().createInteger(((Map)s).size());
                    } else
                        throw new RuntimeException("Required map or collection: " + s);
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$size': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("$createList", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Collection c = context().getCollection(args[0]);
                    Function f = (Function) args[1];
                    Object [] argument = new Object [1];
                    List res = new ArrayList();
                    for (Iterator i = c.iterator(); i.hasNext(); ) {
                        argument[0] = i.next();
                        Object listFragment = context().applyFunction(f, argument);
                        res.addAll(context().getCollection(listFragment));
                    }
                    return context().createList(res);
                }
                catch (Exception ex) {
                    throw new InterpreterException("Cannot create list.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$createSet", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Collection c = context().getCollection(args[0]);
                        Function f = (Function) args[1];
                        Object [] argument = new Object [1];
                        Set res = new HashSet();
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            Object setFragment = context().applyFunction(f, argument);
                            res.addAll(context().getCollection(setFragment));
                        }
                        return context().createSet(res);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot create set.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$createMap", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Collection c = context().getCollection(args[0]);
                        Function f = (Function) args[1];
                        Object [] argument = new Object [1];
                        Map res = new HashMap();
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            Object mapFragment = context().applyFunction(f, argument);
                            res.putAll(context().getMap(mapFragment));
                        }
                        return context().createMap(res);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot create map.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$iterate", context().createProcedure(new Procedure() {
                public void call(Object[] args) {
                    try {
                        Collection c = context().getCollection(args[0]);
                        Object proc = args[1];
                        Object [] argument = new Object [1];
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            context().callProcedure(proc, argument);
                        }
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot iterate.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("iterate", env.get("$iterate"));

        env.bind("accumulate", context().createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Function f = (Function)args[0];
                    Object v = args[1];
                    Collection c = (Collection)args[2];

                    Object [] a = new Object [2];
                    for (Iterator i = c.iterator(); i.hasNext(); ) {
                        Object k = i.next();
                        a[0] = v;
                        a[1] = k;
                        v = f.apply(a);
                    }

                    return v;
                }
                catch (Exception ex) {
                    throw new InterpreterException("Cannot accumulate.", ex);
                }
            }

            public int arity() {
                return 3;
            }
        }));

        env.bind("zip", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Function f = (Function)args[0];
                        Collection c1 = (Collection)args[1];
                        Collection c2 = (Collection)args[2];

                        Object [] a = new Object [2];
                        Iterator i1 = c1.iterator();
                        Iterator i2 = c2.iterator();
                        List res = new ArrayList();
                        while (i1.hasNext() && i2.hasNext()) {
                        	a[0] = i1.next();
                        	a[1] = i2.next();
                            Object v = f.apply(a);
                        	res.add(v);
                        }
                        return context().createList(res);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot accumulate.", ex);
                    }
                }

                public int arity() {
                    return 3;
                }
            }));

        env.bind("selectf", context().createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Object f = args[0];
                        Collection c = context().getCollection(args[1]);

                        if (c.size() == 0)
                            return args[2];
                        else {
                            Iterator i = c.iterator();
                            assert i.hasNext();

                            Object [] a = new Object [2];
                            a[0] = i.next();
                            Collection s = (c instanceof List) ? (Collection)new ArrayList() : (Collection)new HashSet();
                            while (i.hasNext())
                                s.add(i.next());
                            a[1] = (c instanceof List) ? context().createList((List)s) : context().createSet((Set)s);
                            return context().applyFunction(f, a);
                        }
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot apply selectf function.", ex);
                    }
                }

                public int arity() {
                    return 3;
                }
            }));

        env.bind("selectp", context().createProcedure(new Procedure() {
                public void call(Object[] args) {
                    try {
                        Object p = args[0];
                        Collection c = context().getCollection(args[1]);

                        if (c.size() == 0)
                            return;

                        Iterator i = c.iterator();
                        assert i.hasNext();

                        Object [] a = new Object [2];
                        a[0] = i.next();
                        Collection s = (c instanceof List) ? (Collection)new ArrayList() : (Collection)new HashSet();
                        while (i.hasNext())
                            s.add(i.next());
                        a[1] = (c instanceof List) ? context().createList((List)s) : context().createSet((Set)s);
                        context().callProcedure(p, a);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot call selectp procedure.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));
        
        env.bind("readByte", context().createFunction(new Function () {
        	public Object apply(Object[] args) {
        		try {
        			InputStream s = (InputStream)context().toJavaObject(args[0]);
        			return context().createInteger(s.read());
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'readByte': Cannot apply.", ex);
        		}
        	}

        	public int arity() {
        		return 1;
        	}
        }));
        
        env.bind("writeByte", context().createProcedure(new Procedure () {
        	public void call(Object[] args) {
        		try {
        			OutputStream s = (OutputStream)context().toJavaObject(args[0]);
        			int b = context().intValue(args[1]);
        			s.write(b);
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'readByte': Cannot apply.", ex);
        		}
        	}

        	public int arity() {
        		return 2;
        	}
        }));
        
        env.bind("openFile", context().createFunction(new Function () {
        	public Object apply(Object[] args) {
                String a = null;
        		try {
                    a = context().stringValue(args[0]);
        			return new FileInputStream(a);
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'openFile': Cannot apply to \'"+a+"\'", ex);
        		}
        	}

        	public int arity() {
        		return 1;
        	}
        }));

        env.bind("createFile", context().createFunction(new Function () {
        	public Object apply(Object[] args) {
                String a = null;
        		try {
                    a = context().stringValue(args[0]);
                    return new FileOutputStream(a);
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'createFile': Cannot apply to \'"+a+"\'", ex);
        		}
        	}

        	public int arity() {
        		return 1;
        	}
        }));
        
        env.bind("closeInputStream", context().createProcedure(new Procedure () {
        	public void call(Object[] args) {
        		try {
        			InputStream s = (InputStream)context().toJavaObject(args[0]);
        			s.close();
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'readByte': Cannot apply.", ex);
        		}
        	}

        	public int arity() {
        		return 1;
        	}
        }));
        
        env.bind("closeOutputStream", context().createProcedure(new Procedure () {
        	public void call(Object[] args) {
        		try {
        			OutputStream s = (OutputStream)context().toJavaObject(args[0]);
        			s.close();
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'readByte': Cannot apply.", ex);
        		}
        	}

        	public int arity() {
        		return 1;
        	}
        }));
        
        

        env.bind("currentSystemTime", context().createFunction(new Function () {
        	public Object apply(Object[] args) {
        		try {
        			return new Long(System.currentTimeMillis());
        		} catch (Exception ex) {
        			throw new InterpreterException("Function 'currentTime': Cannot apply.", ex);
        		}
        	}

        	public int arity() {
        		return 0;
        	}
        }));

        return env;
    }

    public static class FunctionComposition implements Function {
        public Object apply(Object[] args) {

            assert f2.arity() > 0;
            assert args.length == this.arity();

            if (f2.arity() == 1) {
                Object [] v = new Object [] {f1.apply(args)};
                return f2.apply(v);
            } else {
                Object [] a = new Object [f2.arity()];
                Object [] b = new Object [f1.arity()];
                for (int i = 0; i < a.length; i++) {
                    for (int j = 0; j < b.length; j++) {
                        b[j] = args[i * b.length + j];
                    }
                    a[i] = f1.apply(b);
                }
                return f2.apply(a);
            }
        }

        public int arity() {
            return f1.arity() * f2.arity();
        }

        public FunctionComposition (Function f1, Function f2) {

            assert f2.arity() > 0;

            this.f1 = f1;
            this.f2 = f2;
        }

        private Function f1;
        private Function f2;
    }
}


