// TypedPlatform.java
// Xilinx Confidential
// Copyright (c) 2005 Xilinx Inc

//2005-08-08 DBP Created from caltrop/dev/source/java/caltrop/interpreter/util/DefaultPlatform.java

package net.sf.caltrop.xslt.cal;

import java.io.FileInputStream;
import java.io.InputStream;
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
import net.sf.caltrop.cal.interpreter.util.CalScriptImportHandler;
import net.sf.caltrop.cal.interpreter.util.ClassLoadingImportHandler;
import net.sf.caltrop.cal.interpreter.util.EnvironmentFactoryImportHandler;
import net.sf.caltrop.cal.interpreter.util.ImportHandler;
import net.sf.caltrop.cal.interpreter.util.ImportMapper;
import net.sf.caltrop.cal.interpreter.util.IntegerList;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.cal.interpreter.util.ReplacePrefixImportMapper;


/**
 *  @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class TypedPlatform implements Platform {
	
	public final static Platform thePlatform = new TypedPlatform();
	public final static Context  theContext = thePlatform.context();
	public final static Environment theDefaultEnvironment = thePlatform.createGlobalEnvironment();

	
    public Context context() {
        return TypedContext.theContext;
    }

    public Environment createGlobalEnvironment() {
        return this.createGlobalEnvironment(null);
    }

	public ImportHandler[] getImportHandlers(ClassLoader loader) {
		return new ImportHandler [] {
			new EnvironmentFactoryImportHandler(this),
			new CalScriptImportHandler(this),
			new ClassLoadingImportHandler(this, loader)
		};
	}

	public ImportMapper[] getImportMappers() {
		return new ImportMapper [] {
				new ReplacePrefixImportMapper(new String [] {"caltrop", "lib"}, 
											  new String [] {"com", "xilinx", "systembuilder", "caltrop", "lib"})
		};
	}


    public Environment createGlobalEnvironment(Environment parent) {

        Environment env = TypedContext.theContext.newEnvironmentFrame(parent);
        
        env.bind("__PlatformName", this.getClass().getName());

        env.bind("print", context().createProcedure(new Procedure() {
                public void call(Object[] args) {
                    System.out.print( args[0] );
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("println", context().createProcedure(new Procedure() {
                public void call(Object[] args) {
                    System.out.println( args[0] );
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("sin", context().createFunction(new AbstractUnarySBFunction() {
            public TypedObject doValueFunction (TypedObject a) {
                return (TypedObject)context().createReal( java.lang.Math.sin( context().realValue(a) ) );
            }
        }));

        env.bind("cos", context().createFunction(new AbstractUnarySBFunction() {
            public TypedObject doValueFunction (TypedObject a) {
                return (TypedObject)context().createReal( java.lang.Math.cos( context().realValue(a) ) );
            }
        }));

        env.bind("SOP", context().createFunction(new AbstractUnarySBFunction() {
                public TypedObject doValueFunction(TypedObject a) {
                    try {
                        System.out.println(a);
                        return a;
                    } catch (Exception ex) {
                        throw new InterpreterException("Function 'SOP': Cannot apply.", ex);
                    }
                }
            }));
        
        env.bind("int", context().createFunction(new AbstractUnarySBFunction() {
    		public TypedObject doValueFunction(TypedObject a) {
                return (TypedObject)context().createInteger( context().intValue(a) );        			
    		}
        }));
        
        env.bind("Integers", context().createFunction(new AbstractBinarySBFunction() {

        		public TypedObject doValueFunction(TypedObject ta, TypedObject tb) {
                    int a = context().intValue(ta);
                    int b = context().intValue(tb);
                    List res = (b < a) ? Collections.EMPTY_LIST : new IntegerList(context(), a, b);
                    return (TypedObject)context().createList(res);	
        		}
        		
        		public Type  doTypeFunction(Type ta, Type tb) {
        			return Type.create(Type.nameList, 
        					           Collections.singletonMap(Type.tparType, Type.create(Type.nameInt)),
        					           Collections.EMPTY_MAP);
        		}
            }));

        env.bind("$domain", context().createFunction(new AbstractUnarySBFunction() {
            public TypedObject doValueFunction (TypedObject a) {
                try {
                	Map m = context().getMap(a);
                	return (TypedObject)context().createSet(m.keySet());
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$domain': Cannot apply.", ex);
                }
            }
        }));

        env.bind("toInt", context().createFunction(new AbstractUnarySBFunction() {
            public TypedObject doValueFunction (TypedObject arg) {
                try {
                    return (TypedObject)context().createInteger(BigInteger.valueOf(Math.round(context().realValue(arg))));
                } catch (Exception ex) {
                    throw new InterpreterException("Function 'toInt': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 1;
            }
        }));

        env.bind("$not", context().createFunction(new AbstractUnarySBFunction() {
        		public TypedObject doValueFunction(TypedObject a) {
                    return (TypedObject)context().createBoolean(!context().booleanValue(a));        			
        		}
            }));

        env.bind("$and", context().createFunction(new AbstractBinarySBFunction() {
	
        	    protected TypedObject    doFunction(TypedObject a, TypedObject b) {
	        		if (a.getValue() == TypedContext.UNDEFINED) {
	        			if (b.getValue() == TypedContext.UNDEFINED) {
	        				return TypedContext.boolUNDEFINED;
	        			} else {
	        				if (!context().booleanValue(b)) {
	        					return TypedContext.boolFALSE;
	        				} else {
	        					return TypedContext.boolUNDEFINED;
	        				}
	        			}
	        		} else if (b.getValue() == TypedContext.UNDEFINED) {
	        			if (!context().booleanValue(a)) {
	        				return TypedContext.boolFALSE;
	        			} else {
	        				return TypedContext.boolUNDEFINED;
	        			}
	        		} else { 
	        			return doValueFunction(a, b);
	        		}
	        	}
	        	
	        	protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	        		return (TypedObject)context().createBoolean(context().booleanValue(a) && context().booleanValue(b));
	        	}
	        	
            }));

        env.bind("$or", context().createFunction(new AbstractBinarySBFunction() {

	    	    protected TypedObject    doFunction(TypedObject a, TypedObject b) {
	        		if (a.getValue() == TypedContext.UNDEFINED) {
	        			if (b.getValue() == TypedContext.UNDEFINED) {
	        				return TypedContext.boolUNDEFINED;
	        			} else {
		        			if (context().booleanValue(b)) {
		        				return TypedContext.boolTRUE;
		        			} else {
		        				return TypedContext.boolUNDEFINED;
		        			}	        				
	        			}
	        		} else if (b.getValue() == TypedContext.UNDEFINED) {
	        			if (context().booleanValue(a)) {
	        				return TypedContext.boolTRUE;
	        			} else {
	        				return TypedContext.boolUNDEFINED;
	        			}
	        		} else { 
	        			return doValueFunction(a, b);
	        		}
	        	}
	        	
	        	protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	        		return (TypedObject)context().createBoolean(context().booleanValue(a) || context().booleanValue(b));
	        	}
	        	
	        }));

        env.bind("$eq", context().createFunction(new AbstractBinarySBFunction() {
        	
    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a) && context().isInteger(b)) {
    	    		return (TypedObject)context().createBoolean(a.getValue().equals(b.getValue()));
    	    	} else if (context().isBoolean(a) && context().isBoolean(b)) {
    	    		return (TypedObject)context().createBoolean(a.getValue().equals(b.getValue()));
    	    	} else
    	    		throw new RuntimeException("Cannot compare for equality.");
    	    }
    	    
    	    protected Type  doTypeFunction (Type a, Type b) {
    	    	return Type.typeBool;
    	    }
    	    
        }));

        env.bind("$ne", context().createFunction(new AbstractBinarySBFunction() {
        	
    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a) && context().isInteger(b)) {
    	    		return (TypedObject)context().createBoolean(!a.getValue().equals(b.getValue()));
    	    	} else if (context().isBoolean(a) && context().isBoolean(b)) {
    	    		return (TypedObject)context().createBoolean(!a.getValue().equals(b.getValue()));
    	    	} else
    	    		throw new RuntimeException("Cannot compare for non-equality.");
    	    }
    	    
    	    protected Type  doTypeFunction (Type a, Type b) {
    	    	return Type.typeBool;
    	    }
    	    
        }));

        env.bind("$lt", context().createFunction(new AbstractBinarySBFunction() {
	    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	    	    	if (context().isInteger(a) && context().isInteger(b)) {
	    	    		return (TypedObject)context().createBoolean(context().asBigInteger(a).compareTo(context().asBigInteger(b)) < 0);
	    	    	} else if (context().isReal(a) && context().isReal(b)) {
	    	    		return (TypedObject)context().createBoolean(context().realValue(a) < context().realValue(b));
	    	    	} else
	    	    		throw new RuntimeException("Cannot compare for <.");
	    	    }
	    	    
	    	    protected Type  doTypeFunction (Type a, Type b) {
	    	    	return Type.typeBool;
	    	    }
    	    
            }));

        env.bind("$le", context().createFunction(new AbstractBinarySBFunction() {
	    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	    	    	if (context().isInteger(a) && context().isInteger(b)) {
	    	    		return (TypedObject)context().createBoolean(context().asBigInteger(a).compareTo(context().asBigInteger(b)) <= 0);
	    	    	} else if (context().isReal(a) && context().isReal(b)) {
	    	    		return (TypedObject)context().createBoolean(context().realValue(a) <= context().realValue(b));
	    	    	} else
	    	    		throw new RuntimeException("Cannot compare for <=.");
	    	    }
	    	    
	    	    protected Type  doTypeFunction (Type a, Type b) {
	    	    	return Type.typeBool;
	    	    }
		    
            }));

        env.bind("$gt", context().createFunction(new AbstractBinarySBFunction() {
	    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	    	    	if (context().isInteger(a) && context().isInteger(b)) {
	    	    		return (TypedObject)context().createBoolean(context().asBigInteger(a).compareTo(context().asBigInteger(b)) > 0);
	    	    	} else if (context().isReal(a) && context().isReal(b)) {
	    	    		return (TypedObject)context().createBoolean(context().realValue(a) > context().realValue(b));
	    	    	} else
	    	    		throw new RuntimeException("Cannot compare for >.");
	    	    }
	    	    
	    	    protected Type  doTypeFunction (Type a, Type b) {
	    	    	return Type.typeBool;
	    	    }
		    
            }));


        env.bind("$ge", context().createFunction(new AbstractBinarySBFunction() {
    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a) && context().isInteger(b)) {
    	    		return (TypedObject)context().createBoolean(context().asBigInteger(a).compareTo(context().asBigInteger(b)) >= 0);
    	    	} else if (context().isReal(a) && context().isReal(b)) {
    	    		return (TypedObject)context().createBoolean(context().realValue(a) >= context().realValue(b));
    	    	} else
    	    		throw new RuntimeException("Cannot compare for >=.");
    	    }
    	    
    	    protected Type  doTypeFunction (Type a, Type b) {
    	    	return Type.typeBool;
    	    }
	    
        }));
        
        env.bind("$negate", context().createFunction(new AbstractUnarySBFunction() {

	        	protected TypedObject  doValueFunction(TypedObject a) {
	    	    	if (context().isInteger(a)) {
	    	    		return new TypedObject(a.getType(),
	    	    				              context().asBigInteger(a).negate());
	    	    	} else if (context().isReal(a)) {
	    	    		return (TypedObject)context().createReal(-context().realValue(a));
	    	    	} else
	    	    		throw new RuntimeException("Cannot negate.");
	    	    }	    	    
            }));

        env.bind("$add", context().createFunction(new AbstractBinarySBFunction() {

        	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
	    	    	if (context().isInteger(a)) {
	    	    		if (context().isInteger(b)) {
	    	    			BigInteger n = context().asBigInteger(a).add(context().asBigInteger(b));
	    	    			return (TypedObject)context().createInteger(n, n.bitLength() + 1, true);
	    	    		} else if (context().isReal(b)) {
	    	    			return (TypedObject)context().createReal(context().asBigInteger(a).doubleValue() + context().realValue(b));
	    	    		} else
	    	    			throw new RuntimeException("Cannot add.");
	    	    	} else if (context().isReal(a)) {
	    	    		if (context().isInteger(b)) {
	    	    			return (TypedObject)context().createReal(context().realValue(a) + context().asBigInteger(b).doubleValue());
	    	    		} else if (context().isReal(b)) {
	    	    			return (TypedObject)context().createReal(context().realValue(a) + context().realValue(b));
	    	    		} else
	    	    			throw new RuntimeException("Cannot add.");
	    	    	} else
	    	    		throw new RuntimeException("Cannot add. (" + a + "<" + ((a == null) ? "NULL" : a.getType()) + ">, " 
	    	    				                                   + b + "<" + ((b == null) ? "NULL" : b.getType()) + ">");
        	    }

        	    protected Type doTypeFunction(Type a, Type b) {
            		if (Type.nameInt.equals(a.getName())) {
            			if (Type.nameInt.equals(b.getName())) {
            				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
            				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
            				return Type.create(Type.nameInt, 
            						Collections.EMPTY_MAP, 
            						Collections.singletonMap(Type.vparSize, new Integer(Math.max(w1, w2) + 1)));
            			} else if (Type.nameReal.equals(b.getName())) {
            				return Type.typeReal;
            			} else {
            				throw new RuntimeException("Cannot add types.");
            			}
            		} else if (Type.nameReal.equals(a.getName())) {
            			if (Type.nameInt.equals(b.getName()) || Type.nameReal.equals(b.getName())) {
            				return Type.typeReal;
            			} else {
                			throw new RuntimeException("Cannot add types: " + a + ", " + b + ".");
            			}	
            		} else {
            			throw new RuntimeException("Cannot add types: " + a + ", " + b + ".");
            		}
        	    }
            }));

        env.bind("$mul", context().createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a)) {
    	    		if (context().isInteger(b)) {
    	    			BigInteger n = context().asBigInteger(a).multiply(context().asBigInteger(b));
    	    			return (TypedObject)context().createInteger(n, n.bitLength() + 1, true);
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().asBigInteger(a).doubleValue() * context().realValue(b));
    	    		} else
    	    			throw new RuntimeException("Cannot multiply.");
    	    	} else if (context().isReal(a)) {
    	    		if (context().isInteger(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) * context().asBigInteger(b).doubleValue());
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) * context().realValue(b));
    	    		} else
    	    			throw new RuntimeException("Cannot multiply.");
    	    	} else
    	    		throw new RuntimeException("Cannot multiply.");
    	    }

        	protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer((w1 + w2))));
        			} else if (Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
            			throw new RuntimeException("Cannot multiply types: " + a + ", " + b + ".");
        			}
        		} else if (Type.nameReal.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName()) || Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
            			throw new RuntimeException("Cannot multiply types: " + a + ", " + b + ".");
        			}	
        		} else {
        			throw new RuntimeException("Cannot multiply types: " + a + ", " + b + ".");
        		}
        	}
        }));

        env.bind("$sub", context().createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a)) {
    	    		if (context().isInteger(b)) {
    	    			BigInteger n = context().asBigInteger(a).subtract(context().asBigInteger(b));
    	    			return (TypedObject)context().createInteger(n, n.bitLength() + 1, true);
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().asBigInteger(a).doubleValue() - context().realValue(b));
    	    		} else
    	    			throw new RuntimeException("Cannot subtract.");
    	    	} else if (context().isReal(a)) {
    	    		if (context().isInteger(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) - context().asBigInteger(b).doubleValue());
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) - context().realValue(b));
    	    		} else
            			throw new RuntimeException("Cannot subtract: " + a + ", " + b + ".");
    	    	} else
        			throw new RuntimeException("Cannot subtract: " + a + ", " + b + ".");
    	    }

    	    protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer(Math.max(w1, w2) + 1)));
        			} else if (Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
        				throw new RuntimeException("Cannot add types.");
        			}
        		} else if (Type.nameReal.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName()) || Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
            			throw new RuntimeException("Cannot subtract types: " + a + ", " + b + ".");
        			}	
        		} else {
        			throw new RuntimeException("Cannot subtract types: " + a + ", " + b + ".");
        		}
    	    }
        }));

        env.bind("$div", context().createFunction(new AbstractBinarySBFunction() {

    	    protected TypedObject  doValueFunction(TypedObject a, TypedObject b) {
    	    	if (context().isInteger(a)) {
    	    		if (context().isInteger(b)) {
    	    			BigInteger n = context().asBigInteger(a).divide(context().asBigInteger(b));
    	    			return (TypedObject)context().createInteger(n, n.bitLength() + 1, true);
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().asBigInteger(a).doubleValue() / context().realValue(b));
    	    		} else
    	    			throw new RuntimeException("Cannot divide.");
    	    	} else if (context().isReal(a)) {
    	    		if (context().isInteger(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) / context().asBigInteger(b).doubleValue());
    	    		} else if (context().isReal(b)) {
    	    			return (TypedObject)context().createReal(context().realValue(a) / context().realValue(b));
    	    		} else
    	    			throw new RuntimeException("Cannot divide.");
    	    	} else
    	    		throw new RuntimeException("Cannot divide.");
    	    }

        	protected Type doTypeFunction(Type a, Type b) {
        		if (Type.nameInt.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName())) {
        				int w1 = ((Integer)a.getValueParameters().get(Type.vparSize)).intValue();
        				int w2 = ((Integer)b.getValueParameters().get(Type.vparSize)).intValue();
        				return Type.create(Type.nameInt, 
        						Collections.EMPTY_MAP, 
        						Collections.singletonMap(Type.vparSize, new Integer(w1)));
        			} else if (Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
        				throw new RuntimeException("Cannot divide types.");
        			}
        		} else if (Type.nameReal.equals(a.getName())) {
        			if (Type.nameInt.equals(b.getName()) || Type.nameReal.equals(b.getName())) {
        				return Type.typeReal;
        			} else {
            			throw new RuntimeException("Cannot divide types: " + a + ", " + b + ".");
        			}	
        		} else {
        			throw new RuntimeException("Cannot divide types: " + a + ", " + b + ".");
        		}
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

                        long va = ((Number)a).longValue();
                        long vb = ((Number)b).longValue();
                        long res = va % vb;

                        return new Long(res);
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

        // updated!!!
        env.bind("$createList", context().createFunction(new AbstractBinarySBFunction() {

        	protected TypedObject doValueFunction(TypedObject a, TypedObject b) {
                Collection c = context().getCollection(a);
                Object [] argument = new Object [1];
                List res = new ArrayList();
                for (Iterator i = c.iterator(); i.hasNext(); ) {
                    argument[0] = i.next();
                    Object listFragment = context().applyFunction(b, argument);
                    res.addAll(context().getCollection(listFragment));
                }
                return (TypedObject)context().createList(res);
        	}
        	
        	protected Type doTypeFunction(Type a, Type b) {
        		throw new RuntimeException("Cannot abstractly evaluate lists.");
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
                        throw new InterpreterException("Cannot zip.", ex);
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

        env.bind("readByte", context().createFunction(new AbstractUnarySBFunction () {
                public TypedObject doValueFunction (TypedObject a) {
                    try {
                        InputStream s = (InputStream)a.getValue();
                        return (TypedObject)context().createInteger(s.read());
                    } catch (Exception ex) {
                        throw new InterpreterException("Cannot apply function 'readByte': ", ex);
                    }
                }
            }));
        
        env.bind("openFile", context().createFunction(new AbstractUnarySBFunction () {
                public TypedObject doValueFunction (TypedObject a) {
                    String s = null;
                    try {
                        s = (String)context().stringValue(a);
                        return new TypedObject(Type.typeANY,new FileInputStream(s));
                    } catch (Exception ex) {
                        throw new InterpreterException("Cannot apply function 'openFile' to \""+s+"\": ", ex);
                    }
                }
            }));

        env.bind("currentSystemTime", context().createFunction(new Function () {
                public Object apply(Object[] args) {
                    try {
                        return new Long(System.currentTimeMillis());
                    } catch (Exception ex) {
                        throw new InterpreterException("Cannot apply function 'currentTime': ", ex);
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


