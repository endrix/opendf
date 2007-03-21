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

package net.sf.caltrop.cal.lib;

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
import net.sf.caltrop.cal.interpreter.environment.EnvironmentFactory;
import net.sf.caltrop.cal.interpreter.environment.HashEnvironment;
import net.sf.caltrop.cal.interpreter.util.DefaultContext;
import net.sf.caltrop.cal.interpreter.util.IntegerList;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform.FunctionComposition;


/**
 * 
 * @author jornj
 */
public class Basic implements EnvironmentFactory {


	public Environment createEnvironment(Environment parent, Context envContext) {

        Environment env = DefaultContext.theContext.newEnvironmentFrame(parent);
        final Context context = envContext;

        env.bind("println", context.createProcedure(new Procedure() {
                public void call(Object[] args) {
                    System.out.println(context.stringValue(args[0]));
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("SOP", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        System.out.println(args[0]);
                        return args[0];
                    } catch (Exception ex) {
                        throw new InterpreterException("Function 'SOP': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("Integers", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        int a = context.intValue(args[0]);
                        int b = context.intValue(args[1]);
                        List res = (b < a) ? Collections.EMPTY_LIST : new IntegerList(context, a, b);
                        return context.createList(res);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function 'Integers': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$not", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context.createBoolean(!context.booleanValue(args[0]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$not': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("$and", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context.createBoolean(context.booleanValue(args[0])
                                                    && context.booleanValue(args[1]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$and': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$or", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context.createBoolean(context.booleanValue(args[0])
                                                    || context.booleanValue(args[1]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$or': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$eq", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        return context.createBoolean(args[0].equals(args[1]));
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$eq': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$lt", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context.isSet(args[0]) && context.isCollection(args[1])) {
                		Set s = context.getSet(args[0]);
                		Collection c = context.getCollection(args[1]);
                		return context.createBoolean(s.containsAll(c) && !s.equals(c));
                	} else {
                		Comparable a = (Comparable)args[0];
                		Comparable b = (Comparable)args[1];
                        return context.createBoolean(a.compareTo(b) < 0);
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$lt': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$le", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context.isSet(args[0]) && context.isCollection(args[1])) {
                		Set s = context.getSet(args[0]);
                		Collection c = context.getCollection(args[1]);
                		return context.createBoolean(s.containsAll(c));
                	} else {
                		Comparable a = (Comparable)args[0];
                		Comparable b = (Comparable)args[1];
                        return context.createBoolean(a.compareTo(b) <= 0);
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$le': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$gt", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context.isSet(args[0]) && context.isCollection(args[1])) {
                		Set s = context.getSet(args[0]);
                		Collection c = context.getCollection(args[1]);
                		return context.createBoolean(c.containsAll(s) && !s.equals(c));
                	} else {
                		Comparable a = (Comparable)args[0];
                		Comparable b = (Comparable)args[1];
                        return context.createBoolean(a.compareTo(b) > 0);
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$gt': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$ge", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                	if (context.isSet(args[0]) && context.isCollection(args[1])) {
                		Set s = context.getSet(args[0]);
                		Collection c = context.getCollection(args[1]);
                		return context.createBoolean(c.containsAll(s));
                	} else {
                		Comparable a = (Comparable)args[0];
                		Comparable b = (Comparable)args[1];
                        return context.createBoolean(a.compareTo(b) >= 0);
                	}
                } catch (Exception ex) {
                    throw new InterpreterException("Function '$ge': Cannot apply.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$negate", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                    	Object a = args[0];
                    	if (context.isInteger(a)) {
                    		return context.createInteger(-context.intValue(a));
                    	} else if (context.isReal(a)){
                    		return context.createReal(-context.realValue(a));
                    	} else if (context.isBoolean(a)) {
                    		return context.createBoolean(!context.booleanValue(a));
                    	} else
                            throw new RuntimeException("Cannot negate object: " + a);
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$negate': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 1;
                }
            }));

        env.bind("$add", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Object a = args[0];
                        Object b = args[1];
                        if (context.isReal(a) || context.isReal(b)) {
                        	return context.createReal(context.realValue(a) + context.realValue(b));
                        } else if (context.isInteger(a) && context.isInteger(b)){
                        	return context.createReal(context.intValue(a) + context.intValue(b));
                        } else if (context.isString(a) && context.isString(b)) {
                        	return context.createString(context.stringValue(a) + context.stringValue(b));
                        } else if (context.isSet(a) && context.isCollection(b)) {
                        	Set s = new HashSet(context.getSet(a));
        					s.addAll(context.getCollection(b));
                        	return context.createSet(s);
                        } else if (context.isList(a) && context.isCollection(b)) {
                        	List l = new ArrayList(context.getList(a));
                        	l.addAll(context.getCollection(b));
                        	return context.createList(l);
                        } else if (context.isMap(a) && context.isMap(b)) {
                        	Map m = new HashMap(context.getMap(a));
                        	m.putAll(context.getMap(b));
                        	return context.createMap(m);
                        } else {
                            throw new RuntimeException("Cannot add to: " + a);
                        }
                    } catch (Exception ex) {
                        throw new InterpreterException("Function '$add': Cannot apply.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$mul", context.createFunction(new Function() {
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

                            long va = ((Number)a).longValue();
                            long vb = ((Number)b).longValue();
                            long res = va * vb;

                            return new Long(res);
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

        env.bind("$sub", context.createFunction(new Function() {
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

                            long va = ((Number)a).longValue();
                            long vb = ((Number)b).longValue();
                            long res = va - vb;
                            return new Long(res);
                        } else {
                            double va = ((Number)a).doubleValue();
                            double vb = ((Number)b).doubleValue();
                            return new Double(va - vb);
                        }
                    } else if (a instanceof Collection) {
                        if (b instanceof Collection) {
                            Set res = new HashSet((List)a);
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

        env.bind("$div", context.createFunction(new Function() {
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

                            long va = ((Number)a).longValue();
                            long vb = ((Number)b).longValue();
                            if (! (va % vb == 0)) {
                                return new Double(((double)va) / ((double)vb));
                            }
                            long res = va / vb;
                            return new Integer((int)res);
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

        env.bind("$mod", context.createFunction(new Function() {
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

        env.bind("$size", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Object s = args[0];
                    if (s instanceof Collection) {
                        return context.createInteger(((Collection)s).size());
                    } else if (s instanceof Map) {
                        return context.createInteger(((Map)s).size());
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

        env.bind("$createList", context.createFunction(new Function() {
            public Object apply(Object[] args) {
                try {
                    Collection c = context.getCollection(args[0]);
                    Object f = args[1];
                    Object [] argument = new Object [1];
                    List res = new ArrayList();
                    for (Iterator i = c.iterator(); i.hasNext(); ) {
                        argument[0] = i.next();
                        Object listFragment = context.applyFunction(f, argument);
                        res.addAll(context.getCollection(listFragment));
                    }
                    return context.createList(res);
                }
                catch (Exception ex) {
                    throw new InterpreterException("Cannot create list.", ex);
                }
            }

            public int arity() {
                return 2;
            }
        }));

        env.bind("$createSet", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Collection c = context.getCollection(args[0]);
                        Object f = args[1];
                        Object [] argument = new Object [1];
                        Set res = new HashSet();
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            Object setFragment = context.applyFunction(f, argument);
                            res.addAll(context.getCollection(setFragment));
                        }
                        return context.createSet(res);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot create set.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$createMap", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Collection c = context.getCollection(args[0]);
                        Object f = args[1];
                        Object [] argument = new Object [1];
                        Map res = new HashMap();
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            Object mapFragment = context.applyFunction(f, argument);
                            res.putAll(context.getMap(mapFragment));
                        }
                        return context.createMap(res);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot create map.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        env.bind("$iterate", context.createProcedure(new Procedure() {
                public void call(Object[] args) {
                    try {
                        Collection c = context.getCollection(args[0]);
                        Object proc = args[1];
                        Object [] argument = new Object [1];
                        for (Iterator i = c.iterator(); i.hasNext(); ) {
                            argument[0] = i.next();
                            context.callProcedure(proc, argument);
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

        env.bind("accumulate", context.createFunction(new Function() {
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

        env.bind("selectf", context.createFunction(new Function() {
                public Object apply(Object[] args) {
                    try {
                        Object f = args[0];
                        Collection c = context.getCollection(args[1]);

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
                            a[1] = (c instanceof List) ? context.createList((List)s) : context.createSet((Set)s);
                            return context.applyFunction(f, a);
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

        env.bind("selectp", context.createProcedure(new Procedure() {
                public void call(Object[] args) {
                    try {
                        Object p = args[0];
                        Collection c = context.getCollection(args[1]);

                        if (c.size() == 0)
                            return;

                        Iterator i = c.iterator();
                        assert i.hasNext();

                        Object [] a = new Object [2];
                        a[0] = i.next();
                        Collection s = (c instanceof List) ? (Collection)new ArrayList() : (Collection)new HashSet();
                        while (i.hasNext())
                            s.add(i.next());
                        a[1] = (c instanceof List) ? context.createList((List)s) : context.createSet((Set)s);
                        context.callProcedure(p, a);
                    }
                    catch (Exception ex) {
                        throw new InterpreterException("Cannot call selectp procedure.", ex);
                    }
                }

                public int arity() {
                    return 2;
                }
            }));

        
        return env;
	}
}
