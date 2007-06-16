package net.sf.caltrop.cal.i2.platform;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.caltrop.cal.i2.Configuration;
import net.sf.caltrop.cal.i2.Environment;
import net.sf.caltrop.cal.i2.Evaluator;
import net.sf.caltrop.cal.i2.Executor;
import net.sf.caltrop.cal.i2.Function;
import net.sf.caltrop.cal.i2.InterpreterException;
import net.sf.caltrop.cal.i2.configuration.DefaultUntypedConfiguration;
import net.sf.caltrop.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.caltrop.cal.i2.util.FunctionOf1;
import net.sf.caltrop.cal.i2.util.FunctionOf2;
import net.sf.caltrop.cal.i2.util.FunctionOf3;
import net.sf.caltrop.cal.i2.util.ImportHandler;
import net.sf.caltrop.cal.i2.util.ImportMapper;
import net.sf.caltrop.cal.i2.util.IntegerFactory;
import net.sf.caltrop.cal.i2.util.Platform;
import net.sf.caltrop.cal.i2.util.ReplacePrefixImportMapper;
import net.sf.caltrop.cal.i2.Procedure;
import net.sf.caltrop.cal.i2.util.IntegerList;

public class DefaultUntypedPlatform implements Platform {

	public Configuration configuration() {
		return theConfiguration;
	}

	public Environment createGlobalEnvironment() {
		return createGlobalEnvironment(null);
	}

	public Environment createGlobalEnvironment(Environment parent) {
		DynamicEnvironmentFrame env = new DynamicEnvironmentFrame(parent);
		
		populateGlobalEnvironment(env);
		return env;
	}

	public ImportHandler[] getImportHandlers(ClassLoader loader) {
		// TODO Auto-generated method stub
		return null;
	}

	public ImportMapper []  getImportMappers() {
		return new ImportMapper [] {
				new ReplacePrefixImportMapper(new String [] {"caltrop", "lib"}, 
											  new String [] {"net", "sf", "caltrop", "cal", "lib"})
		};
    }


	public static final Platform      	thePlatform = new DefaultUntypedPlatform();
	public static final Configuration 	theConfiguration = new DefaultUntypedConfiguration();
	
	private static final IntegerFactory theIntegerFactory = new DefaultIntegerFactory();
		
	static class DefaultIntegerFactory implements IntegerFactory {
		public Object create(long v) {
			return BigInteger.valueOf(v);
		}
	};

	private static void populateGlobalEnvironment(DynamicEnvironmentFrame env) {
		
		env.bind("PI", Double.valueOf(Math.PI), null);   // TYPEFIXME
		
		env.bind("$add", new FunctionOf2 () {

			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger && b instanceof BigInteger) {
					return ((BigInteger)a).add((BigInteger)b);
				} else {
					throw new InterpreterException("+ operator: Cannot handle arguments. (" + a + ", " + b+ ")");
				}
			}			
		}, null);
		
		env.bind("__PlatformName", DefaultUntypedPlatform.class.getName(), null);

		env.bind("socketGetToken", new FunctionOf3() {
			@Override
			public Object f(Object a, Object b, Object c) {
	            String address = (String)a;
	            int socketNum = intValueOf(b);
	            int bytes = intValueOf(c);
	            Socket socket = getSocket(address, socketNum);
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
	            return createInteger(value);
	        }	        
	    }, null);

		env.bind("socketPutToken", new Procedure() {

			public void call(int n, Executor executor) {
				assert n == 4;
				
	            String address = (String)executor.getValue(0);
	            int socketNum = intValueOf(executor.getValue(1));
	            int bytes = intValueOf(executor.getValue(2));
	            int value = intValueOf(executor.getValue(3));
	            Socket socket = getSocket(address, socketNum);
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
	            executor.pop(n);
	        }
	    }, null);

		env.bind("print", new Procedure() {

			public void call(int n, Executor executor) {
				assert n == 1;

				System.out.print(executor.getValue(0));
		        executor.pop(n);
		    }
		}, null);

		env.bind("println", new Procedure() {

			public void call(int n, Executor executor) {			
				assert n == 1;

				System.out.println(executor.getValue(0));
		        executor.pop(n);
		    }
		}, null);

		env.bind("sin", new FunctionOf1() {
			
			@Override
			public Object f(Object a) {
				return Double.valueOf(java.lang.Math.sin(doubleValueOf(a)));
		    }
		}, null);

		env.bind("cos", new FunctionOf1() {

			@Override
			public Object f(Object a) {
				return Double.valueOf(java.lang.Math.cos(doubleValueOf(a)));
		    }
		}, null);

		env.bind("int", new Function() {

			public void apply(int n, Evaluator evaluator) {
				assert n == 1;
				
				evaluator.replaceWithResult(n, intValueOf(evaluator.getValue(0)))
		    }
		}, null);

		env.bind("SOP", new FunctionOf1() {

			public Object f(Object a) {
				System.out.println(a);
				return a;
			}
		}, null);

		env.bind("Integers", new FunctionOf2() {

			public Object f(Object a, Object b) {
				long aa = longValueOf(a);
				long bb = longValueOf(b);
				List res = (bb < aa) ? Collections.EMPTY_LIST : new IntegerList(theIntegerFactory, aa, bb);
			}
		    }, null);

		env.bind("toInt", new FunctionOf1() {

			@Override
			public Object f(Object a) {
				return BigInteger.valueOf(intValueOf(a));
		    }
		}, null);

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
	}
	
	private static BigInteger  createInteger(int n) {
		return BigInteger.valueOf(n);
	}
	
	private static double  doubleValueOf(Object a) {
		return ((Number)a).doubleValue();
	}

    private static int intValueOf (Object o)
    {
        if (o instanceof String)
            return Integer.parseInt((String)o);
        
        return ((Number)o).intValue();
    }
    
    private static int longValueOf (Object o)
    {
        return ((Number)o).longValue();
    }
    

    private static Socket getSocket (String ipAddr, int socketNum)
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
    private static Map allocatedSockets = new HashMap();


}




/*



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
//                      FIXME: behave like the div operator for testing
//                        return new Double(((double)va) / ((double)vb));
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
*/


