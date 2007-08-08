package net.sf.caltrop.cal.i2.platform;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import net.sf.caltrop.cal.i2.Procedure;
import net.sf.caltrop.cal.i2.configuration.DefaultUntypedConfiguration;
import net.sf.caltrop.cal.i2.environment.DynamicEnvironmentFrame;
import net.sf.caltrop.cal.i2.util.CalScriptImportHandler;
import net.sf.caltrop.cal.i2.util.ClassLoadingImportHandler;
import net.sf.caltrop.cal.i2.util.EnvironmentFactoryImportHandler;
import net.sf.caltrop.cal.i2.util.FunctionOf0;
import net.sf.caltrop.cal.i2.util.FunctionOf1;
import net.sf.caltrop.cal.i2.util.FunctionOf2;
import net.sf.caltrop.cal.i2.util.FunctionOf2Eval;
import net.sf.caltrop.cal.i2.util.FunctionOf3;
import net.sf.caltrop.cal.i2.util.FunctionOf3Eval;
import net.sf.caltrop.cal.i2.util.ImportHandler;
import net.sf.caltrop.cal.i2.util.ImportMapper;
import net.sf.caltrop.cal.i2.util.IntegerFactory;
import net.sf.caltrop.cal.i2.util.IntegerList;
import net.sf.caltrop.cal.i2.util.Platform;
import net.sf.caltrop.cal.i2.util.ProcedureOf1;
import net.sf.caltrop.cal.i2.util.ProcedureOf2;
import net.sf.caltrop.cal.i2.util.ProcedureOf2Eval;
import net.sf.caltrop.cal.i2.util.ProcedureOf4;
import net.sf.caltrop.cal.i2.util.ReplacePrefixImportMapper;

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
		return new ImportHandler [] {
				new EnvironmentFactoryImportHandler(this),
				new CalScriptImportHandler(this),
				new ClassLoadingImportHandler(this, loader)
		};
	}

	public ImportMapper []  getImportMappers() {
		return new ImportMapper [] {
				new ReplacePrefixImportMapper(new String [] {"caltrop", "lib"}, 
											  new String [] {"net", "sf", "caltrop", "cal", "lib_i2"})
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

		env.bind("socketPutToken", new ProcedureOf4() {
			public void p(Object a, Object b, Object c, Object d) {
	            String address = (String)a;
	            int socketNum = intValueOf(b);
	            int bytes = intValueOf(c);
	            int value = intValueOf(d);
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
	        }
	    }, null);

		env.bind("print", new ProcedureOf1() {
			public void p(Object a) {
				System.out.print(a);
		    }
		}, null);

		env.bind("println", new ProcedureOf1() {
			public void p(Object a) {
				System.out.println(a);
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

		env.bind("int", new FunctionOf1() {

			@Override
			public Object f(Object a) {
				if (a instanceof String) {
					return new BigInteger((String)a);
				}
				return createInteger(intValueOf(a));
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
				return res;
			}
		}, null);

		env.bind("toInt", new FunctionOf1() {

			@Override
			public Object f(Object a) {
				return BigInteger.valueOf(intValueOf(a));
		    }
		}, null);

		env.bind("$domain", new FunctionOf1() {
			@Override
			public Object f(Object a) {
				Map m = (Map)a;
				return m.keySet();
		    }
		}, null);

		env.bind("$not", new FunctionOf1() {
			@Override
			public Object f(Object a) {
				return booleanValueOf(!theConfiguration.booleanValue(a));
			}
		}, null);

		env.bind("$and", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				return booleanValueOf(theConfiguration.booleanValue(a) && theConfiguration.booleanValue(b)); 
		    }
		}, null);

		env.bind("$or", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				return booleanValueOf(theConfiguration.booleanValue(a) || theConfiguration.booleanValue(b)); 
		    }
		}, null);

		env.bind("$eq", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				return booleanValueOf(equality(a, b));
			}
		}, null);
		
		env.bind("$ne", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				return booleanValueOf(!equality(a, b));
			}
		}, null);
		
		env.bind("$lt", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger && b instanceof BigInteger) {
					return booleanValueOf(((BigInteger)a).compareTo((BigInteger)b) < 0);
				} else if (a instanceof Number && b instanceof Number) {
					return booleanValueOf(((Number)a).doubleValue() < ((Number)b).doubleValue());
				} else if (a instanceof Comparable && b instanceof Comparable) {
					return booleanValueOf(((Comparable)a).compareTo(b) < 0);
				} else {
					throw new InterpreterException("< operator: Cannot be used with these arguments. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$le", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger && b instanceof BigInteger) {
					return booleanValueOf(((BigInteger)a).compareTo((BigInteger)b) <= 0);
				} else if (a instanceof Number && b instanceof Number) {
					return booleanValueOf(((Number)a).doubleValue() <= ((Number)b).doubleValue());
				} else if (a instanceof Comparable && b instanceof Comparable) {
					return booleanValueOf(((Comparable)a).compareTo(b) <= 0);
				} else {
					throw new InterpreterException("<= operator: Cannot be used with these arguments. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$gt", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger && b instanceof BigInteger) {
					return booleanValueOf(((BigInteger)a).compareTo((BigInteger)b) > 0);
				} else if (a instanceof Number && b instanceof Number) {
					return booleanValueOf(((Number)a).doubleValue() > ((Number)b).doubleValue());
				} else if (a instanceof Comparable && b instanceof Comparable) {
					return booleanValueOf(((Comparable)a).compareTo(b) > 0);
				} else {
					throw new InterpreterException("> operator: Cannot be used with these arguments. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$ge", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger && b instanceof BigInteger) {
					return booleanValueOf(((BigInteger)a).compareTo((BigInteger)b) >= 0);
				} else if (a instanceof Number && b instanceof Number) {
					return booleanValueOf(((Number)a).doubleValue() >= ((Number)b).doubleValue());
				} else if (a instanceof Comparable && b instanceof Comparable) {
					return booleanValueOf(((Comparable)a).compareTo(b) >= 0);
				} else {
					throw new InterpreterException(">= operator: Cannot be used with these arguments. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$negate", new FunctionOf1() {
			@Override
			public Object f(Object a) {
				if (! (a instanceof Number))
					throw new InterpreterException("- operator: Argument not a number. (" + a + ")");
				Number n = (Number)a;
				if (n instanceof BigInteger)
					return ((BigInteger)n).negate();
				else if (n instanceof Integer)
					return new Integer(-n.intValue());
				else if (n instanceof Double)
					return new Double(-n.doubleValue());
				else if (n instanceof Float)
					return new Float(-n.floatValue());
				else if (n instanceof Long)
					return new Long(-n.longValue());
				else if (n instanceof Short)
					return new Integer(-n.shortValue());
				else if (n instanceof BigDecimal)
					return ((BigDecimal)n).negate();
				else
					throw new InterpreterException("- operator: Argument aot a scalar. (" + n + ")");
			}
		}, null);

		env.bind("$add", new FunctionOf2 () {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof String || b instanceof String) {
					return "" + a + b;
				} else if (a instanceof Number) {
					if (! (b instanceof Number))
						throw new InterpreterException("+ operator: Cannot add to number: " + b);

					if (a instanceof Double || a instanceof Float || b instanceof Double || b instanceof Float) {
						return new Double(((Number)a).doubleValue() + ((Number)b).doubleValue());
					}

					if (a instanceof BigInteger) {
						if (b instanceof BigInteger) {
							return ((BigInteger)a).add((BigInteger)b);
						} else if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
							return ((BigInteger)a).add(BigInteger.valueOf(((Number)b).longValue()));
						}
					}

					return BigInteger.valueOf(((Number)a).longValue()).add(BigInteger.valueOf(((Number)b).longValue()));
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
						throw new RuntimeException("+ operator: Cannot add to list: " + b);
				} else if (a instanceof Collection) {
					if (b instanceof Collection) {
						Set res = new HashSet((List)a);
						res.addAll((Collection)b);
						return res;
					} else
						throw new RuntimeException("+ operator: Cannot add to collection: " + b);
				} else if (a instanceof Map) {
					if (b instanceof Map) {
						Map res = new HashMap((Map)a);
						res.putAll((Map)b);
						return res;
					} else
						throw new RuntimeException("+ operator: Cannot add to map: " + b);
				} else {
					throw new RuntimeException("+ operator: Cannot add. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$mul", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof Number) {
					if (! (b instanceof Number))
						throw new InterpreterException("* operator: Cannot multiply with number: " + b);

					if (a instanceof Double || a instanceof Float || b instanceof Double || b instanceof Float) {
						return new Double(((Number)a).doubleValue() * ((Number)b).doubleValue());
					}

					if (a instanceof BigInteger) {
						if (b instanceof BigInteger) {
							return ((BigInteger)a).multiply((BigInteger)b);
						} else if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
							return ((BigInteger)a).multiply(BigInteger.valueOf(((Number)b).longValue()));
						}
					}

					return BigInteger.valueOf(((Number)a).longValue()).multiply(BigInteger.valueOf(((Number)b).longValue()));
				} else if (a instanceof Collection) {
					if (b instanceof Collection) {
						Set res = new HashSet((Collection)a);
						res.retainAll((Collection)b);
						return res;
					} else
						throw new InterpreterException("* operator: Cannot multiply with collection: " + b);
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
					} else
						throw new InterpreterException("* operator: Cannot multiply with map: " + b);
				} else {
					throw new InterpreterException("* operator: Cannot multiply. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$sub", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof Number) {
					if (! (b instanceof Number))
						throw new InterpreterException("- operator: Cannot subtract from number: " + b);

					if (a instanceof Double || a instanceof Float || b instanceof Double || b instanceof Float) {
						return new Double(((Number)a).doubleValue() - ((Number)b).doubleValue());
					}

					if (a instanceof BigInteger) {
						if (b instanceof BigInteger) {
							return ((BigInteger)a).subtract((BigInteger)b);
						} else if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
							return ((BigInteger)a).subtract(BigInteger.valueOf(((Number)b).longValue()));
						}
					}

					return BigInteger.valueOf(((Number)a).longValue()).subtract(BigInteger.valueOf(((Number)b).longValue()));
				} else if (a instanceof Collection) {
					if (b instanceof Collection) {
						Set res = new HashSet((Collection)a);
						res.removeAll((Collection)b);
						return res;
					} else
						throw new InterpreterException("- operator: Cannot subtract from collection: " + b);
				} else if (a instanceof Map) {
					if (b instanceof Collection) {
						Map res = new HashMap((Map)a);
						for (Iterator i = ((Collection)b).iterator(); i.hasNext(); ) {
							Object k = i.next();
							res.remove(k);
						}
						return res;
					} else
						throw new InterpreterException("- operator: Cannot subtract from map: " + b);
				} else {
					throw new InterpreterException("- operator: Cannot subtract. (" + a + ", " + b + ")");
				}
			}
		}, null);

		env.bind("$div", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof Number) {
					if (! (b instanceof Number))
						throw new InterpreterException("/ operator: Cannot divide number: " + b);

					if (a instanceof Double || a instanceof Float || b instanceof Double || b instanceof Float) {
						return new Double(((Number)a).doubleValue() / ((Number)b).doubleValue());
					}

					if (a instanceof BigInteger) {
						if (b instanceof BigInteger) {
							return ((BigInteger)a).divide((BigInteger)b);
						} else if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
							return ((BigInteger)a).divide(BigInteger.valueOf(((Number)b).longValue()));
						}
					}

					return BigInteger.valueOf(((Number)a).longValue()).divide(BigInteger.valueOf(((Number)b).longValue()));
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
						throw new InterpreterException("/ operator: Cannot constrain map: " + b);
				} else {
					throw new InterpreterException("/ operator: Cannot apply. (" + a + ", " + b + ")");
				}
		    }
		}, null);

		env.bind("$mod", new FunctionOf2() {
			@Override
			public Object f(Object a, Object b) {
				if (a instanceof BigInteger) {
					if (b instanceof BigInteger) {
						return ((BigInteger)a).remainder((BigInteger)b);
					} else if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
						return ((BigInteger)a).remainder(BigInteger.valueOf(((Number)b).longValue()));
					} else {
						throw new InterpreterException("mod operator: Cannot apply. (" + a + ", " + b + ")");						
					}
				} else if (a instanceof Short || a instanceof Integer || a instanceof Long || a instanceof Byte) {
					if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Byte) {
						return ((BigInteger)a).remainder(BigInteger.valueOf(((Number)b).longValue()));
					} else {
						throw new InterpreterException("mod operator: Cannot apply. (" + a + ", " + b + ")");						
					}
				} else {
					throw new InterpreterException("mod operator: Cannot apply. (" + a + ", " + b + ")");						
		        }
		    }
		}, null);

		env.bind("$size", new FunctionOf1() {
			@Override
		    public Object f(Object a) {
				if (a instanceof Collection) {
					return createInteger(((Collection)a).size());
				} else if (a instanceof Map) {
					return createInteger(((Map)a).size());
				} else
					throw new RuntimeException("# operator: Required map or collection. (" + a +")");
			}
		}, null);

		env.bind("$createList", new FunctionOf2Eval() {
			public Object f(Object a, Object b, Evaluator evaluator) {
				Collection c = (Collection)a;
	            Function f = (Function)b;
	            List res = new ArrayList();
	            for (Object x : c) {
	            	evaluator.push(x);
	            	f.apply(1, evaluator);
	            	Collection val = (Collection)evaluator.pop();
		            res.addAll(val);
	            }
	            return res;
			}
		}, null);

		env.bind("$createSet", new FunctionOf2Eval() {
			public Object f(Object a, Object b, Evaluator evaluator) {
				Collection c = (Collection)a;
	            Function f = (Function)b;
	            Set res = new HashSet();
	            for (Object x : c) {
	            	evaluator.push(x);
	            	f.apply(1, evaluator);
	            	Collection val = (Collection)evaluator.pop();
		            res.addAll(val);
	            }
	            return res;
			}
		}, null);

		env.bind("$createMap", new FunctionOf2Eval() {
			public Object f(Object a, Object b, Evaluator evaluator) {
				Collection c = (Collection)a;
	            Function f = (Function)b;
	            Map res = new HashMap();
	            for (Object x : c) {
	            	evaluator.push(x);
	            	f.apply(1, evaluator);
	            	Map val = (Map)evaluator.pop();
		            res.putAll(val);
	            }
	            return res;
			}
		}, null);

		env.bind("$iterate", new ProcedureOf2Eval() {
			public void p(Object a, Object b, Executor executor) {
				Collection c = (Collection)a;
				Procedure proc = (Procedure)b;
				for (Object x : c) {
					executor.push(x);
					proc.call(1, executor);
				}
			}
		}, null);

		env.bind("iterate", env.getByName("$iterate"), null);

		env.bind("accumulate", new FunctionOf3Eval() {
			public Object f(Object a, Object b, Object c, Evaluator evaluator) {
				Function f = (Function)a;
				Object v = b;

				for (Object k : (Collection)c) {
					evaluator.push(k);
					evaluator.push(v);
					f.apply(2, evaluator);
					v = evaluator.pop();
				}
				return v;
		    }
		}, null);

		env.bind("zip", new FunctionOf3Eval() {
			public Object f(Object a, Object b, Object c, Evaluator evaluator) {
				Function f = (Function)a;
				Collection c1 = (Collection)b;
				Collection c2 = (Collection)c;

				Iterator i1 = c1.iterator();
				Iterator i2 = c2.iterator();
				List res = new ArrayList();
				while (i1.hasNext() && i2.hasNext()) {
					evaluator.push(i2.next());
					evaluator.push(i1.next());
					f.apply(2, evaluator);
					Object v = evaluator.pop();
					res.add(v);
				}
				return res;
			}
		}, null);

		env.bind("selectf", new FunctionOf3Eval() {
			public Object f(Object a, Object b, Object c, Evaluator evaluator) {
				Function f = (Function)a;
				Collection s = (Collection)b;

				if (s.size() == 0)
					return c;
				else {
					Iterator i = s.iterator();
					assert i.hasNext();
					
					Object x = i.next();
					
					Collection rst = (s instanceof List) ? new ArrayList() : new HashSet();
					while (i.hasNext())
						rst.add(i.next());

					evaluator.push(rst);
					evaluator.push(x);
					
					f.apply(2, evaluator);
					return evaluator.pop();
				}
			}
		}, null);

		env.bind("selectp", new ProcedureOf2Eval() {
			@Override
			public void p(Object a, Object b, Executor executor) {
				Procedure p = (Procedure)a;
				Collection s = (Collection)b;

				if (s.size() == 0)
					return;

				Iterator i = s.iterator();
				assert i.hasNext();

				Object x = i.next();
				
				Collection rst = (s instanceof List) ? new ArrayList() : new HashSet();
				while (i.hasNext())
					rst.add(i.next());
				
				executor.push(rst);
				executor.push(x);
				p.call(2, executor);
			}
		}, null);

		env.bind("readByte", new FunctionOf1 () {
			@Override
			public Object f(Object a) {
				InputStream s = (InputStream)a;
				try {
					return createInteger(s.read());
				} catch (IOException e) {
					throw new InterpreterException("I/O exception while trying to read a byte.", e);
				}
			}
		}, null);

		env.bind("writeByte", new ProcedureOf2 () {
			@Override
			public void p(Object a, Object b) {
				OutputStream s = (OutputStream)a;
				int bb = ((Number)b).intValue();
				try {
					s.write(bb);
				} catch (IOException e) {
					throw new InterpreterException("I/O exception while trying to write a byte.", e);
				}
			}
		}, null);

		env.bind("openFile", new FunctionOf1 () {
			@Override
			public Object f(Object a) {
				String fname = (String)a;
				try {
					return new FileInputStream(fname);
				} catch (FileNotFoundException e) {
					throw new InterpreterException("I/O exception while trying to open a file at '" + fname + "'.", e);
				}
			}
		}, null);

		env.bind("createFile", new FunctionOf1 () {
			@Override
			public Object f(Object a) {
				String fname = (String)a;
				try {
					return new FileOutputStream(fname);
				} catch (FileNotFoundException e) {
					throw new InterpreterException("I/O exception while trying to create a file at '" + fname + "'.", e);
				}
			}
		}, null);

		env.bind("closeInputStream", new ProcedureOf1 () {
			@Override
			public void p(Object a) {
				try {
					((InputStream)a).close();
				} catch (IOException e) {
					throw new InterpreterException("I/O exception while trying to close an input stream.", e);
				}
			}
		}, null);

		env.bind("closeOutputStream", new ProcedureOf1 () {
			@Override
			public void p(Object a) {
				try {
					((OutputStream)a).close();
				} catch (IOException e) {
					throw new InterpreterException("I/O exception white trying to close an output stream.", e);
				}
			}
		}, null);

		env.bind("currentSystemTime", new FunctionOf0 () {
			@Override
			public Object f() {
				return createInteger(System.currentTimeMillis());
			}
		}, null);
		
        env.bind("bitor", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa | bb);
            }
        }, null);

        env.bind("bitand", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa & bb);
            }
        }, null);

        env.bind("bitxor", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa ^ bb);
            }
        }, null);
        
        env.bind("bitnot", new FunctionOf1() {
        	@Override
        	public Object f(Object a) {
                int aa = intValueOf(a);
                return createInteger(~aa);
        	}
        }, null);
        
        env.bind("rshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa >> bb);
            }
        }, null);

        env.bind("urshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa >>> bb);
            }
        }, null);

        env.bind("lshift", new FunctionOf2() {
        	@Override
        	public Object f(Object a, Object b) {
                int aa = intValueOf(a);
                int bb = intValueOf(b);
                return createInteger(aa << bb);
            }
        }, null);

		
	}
	
	private static boolean  equality(Object a, Object b) {
		if (a == null) {
			return booleanValueOf(b == null);
		} else {
			if (a instanceof BigInteger) {
				if (b instanceof BigInteger) {
					return a.equals(b);
				}
				if (b instanceof Long || b instanceof Integer || b instanceof Short || b instanceof Byte) {
					return a.equals(BigInteger.valueOf(((Number)b).longValue()));
				}
				if (b instanceof Double || b instanceof Float) {
					return ((Number)a).doubleValue() == ((Number)b).doubleValue();
				}
				return false;
			}
			
			assert ! (a instanceof BigInteger);
			
			if (b instanceof BigInteger) {
				if (a instanceof Long || a instanceof Integer || a instanceof Short || a instanceof Byte) {
					return b.equals(BigInteger.valueOf(((Number)a).longValue()));
				}
				if (a instanceof Double || a instanceof Float) {
					return ((Number)a).doubleValue() == ((Number)b).doubleValue();
				}
				
				return false;
			}
			
			assert ! (b instanceof BigInteger);
			
			if (a instanceof Long || a instanceof Integer || a instanceof Short || a instanceof Byte) {
				if (b instanceof Long || b instanceof Integer || b instanceof Short || b instanceof Byte) {
					return ((Number)a).longValue() == ((Number)b).longValue();
				}
				if (b instanceof Double || b instanceof Float) {
					return ((Number)a).doubleValue() == ((Number)b).doubleValue();
				}
				
				return false;
			}
			
			if (a instanceof Double || a instanceof Float) {
				if (b instanceof Double || b instanceof Float) {
					return ((Number)a).doubleValue() == ((Number)b).doubleValue();
				}
				
				return false;				
			}
			
			return a.equals(b);
		}
	}
	
	private static BigInteger  createInteger(long n) {
		return BigInteger.valueOf(n);
	}
	
	private static Boolean  booleanValueOf(boolean b) {
		return b ? Boolean.TRUE : Boolean.FALSE;
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
    
    private static long longValueOf (Object o)
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
