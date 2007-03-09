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


package net.sf.caltrop.cal.interpreter;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface provides the key abstraction of the context an
 * interpreter (whether for expressions, statements, or other
 * entities) runs in. This includes the creation of instances of basic
 * datatypes and some of the basic operations on them. Instances of
 * this interface are injected into the interpreters at instantiation
 * time.
 *
 * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
 * @see ExprEvaluator
 * @see StmtEvaluator
 * @see ptolemy.caltrop.ddi.util.DataflowActorInterpreter
 */

public interface Context extends net.sf.caltrop.cal.interpreter.environment.DataStructureManipulator {

    //////////////////////////////////////////////////////////
    ////    Simple Data Objects
    //////////////////////////////////////////////////////////

    /**
     * Create a null object.
     *
     * @return An object representing a null entity. Can be null.
     */

    Object   createNull();

    boolean  isNull(Object o);

    /**
     * Create a Boolean object.
     *
     * @param b The Boolean value.
     * @return An object representing the boolean value.
     */

    Object   createBoolean(boolean b);

    boolean  isBoolean(Object o);
    
    /**
     * Return the Boolean value represented by a Boolean object. The latter is guaranteed to be returned by a
     * {@link #createBoolean(boolean) createBoolean(...)} call.
     *
     * @param b A Boolean object.
     * @return The corresponding Boolean value.
     */
    boolean  booleanValue(Object b);

    /**
     * Create a character object.
     *
     * @param c The character value.
     * @return An object representing the character value.
     */

    Object   createCharacter(char c);

    boolean  isCharacter(Object o);

    char     charValue(Object o);

    /**
     * Create an integer object.
     *
     * @param s A string representation of the integer object.
     * @return A corresponding integer object.
     */

    Object   createInteger(String s);

    Object   createInteger(int n);
    
    Object   createInteger(BigInteger n);
    
    Object   createInteger(BigInteger n, int nBits, boolean signed);

    boolean  isInteger(Object o);
    
/*    int 	 integerSize();
    
    public static final  int  IS_INT = 1;
    public static final  int  IS_LONG = 2;
    public static final  int  IS_BIG = 99;
*/
    int      intValue(Object o);
/*    
    long     longValue(Object o);
    
    BigInteger bigIntValue(Object o);
*/
    BigInteger  asBigInteger(Object o);
    
    int getIntegerLength(Object o);
    
    int getMinimalIntegerLength(Object o);
    
    boolean isSignedInteger(Object o);
    
    Object   createReal(String s);
    
    Object   createReal(double v);

    boolean  isReal(Object o);

    double   realValue(Object o);
    
    Object   createString(String s);

    boolean  isString(Object o);

    String   stringValue(Object o);

    //////////////////////////////////////////////////////////
    ////    Collections
    //////////////////////////////////////////////////////////

    Object   createList(List a);

    boolean  isList(Object o);

    List     getList(Object o);

    Object   createSet(Set s);

    boolean  isSet(Object o);

    Set      getSet(Object o);

    Object   createMap(Map m);

    boolean  isMap(Object o);

    Map         getMap(Object a);

    Object      applyMap(Object map, Object arg);

    boolean     isCollection(Object o);

    Collection  getCollection(Object o);

    //////////////////////////////////////////////////////////
    ////    Functional and procedural closures
    //////////////////////////////////////////////////////////

    /**
     * Create a function object based on the Function parameter. (Typically, this is either a wrapper for the
     * Function object, or just the object itself.)
     *
     * @param f The function.
     * @return An object representing the function.
     */
    Object   createFunction(Function f);

    boolean  isFunction(Object a);

    /**
     * Apply a function object to an array of parameters. The <tt>function</tt> parameter is guaranteed to be an object
     * that was returned from a {@link #createFunction(net.sf.caltrop.cal.interpreter.Function) createFunction(...)} call.
     *
     * @param function The function object.
     * @param args The arguments.
     * @return The value resulting from applying the function to the arguments.
     * @see Function
     */
    Object   applyFunction(Object function, Object [] args); // TODO: perhaps need to optimize array creation


    /**
     * Create a procedure object based on the Procedure parameter. (Typically, this is either a wrapper for the
     * Procedure object, or just the object itself.)
     *
     * @param p The procedure.
     * @return An object representing the procedure.
     * @see Procedure
     */
    Object   createProcedure(Procedure p);

    boolean  isProcedure(Object o);
    /**
     * Call a procedure object on an array of parameters. The <tt>procedure</tt> parameter is guaranteed to be an object
     * that was returned from a {@link #createProcedure(net.sf.caltrop.cal.interpreter.Procedure) createProcedure(...)} call.
     *
     * @param procedure The function object.
     * @param args The arguments.
     */
    void   callProcedure(Object procedure, Object [] args); // TODO: perhaps need to optimize array creation


    //////////////////////////////////////////////////////////
    ////    Class
    //////////////////////////////////////////////////////////

    /**
     * Create and return the dataobject representing the specified class object.
     *
     * @param c The Class object.
     * @return The corresponding data object.
     */
    Object  createClass(Class c);

    boolean  isClass(Object o);

    /**
     * Return the Class corresponding to this object.
     */
    Class    getJavaClass(Object o);

    Class    getJavaClassOfObject(Object o);



    //////////////////////////////////////////////////////////
    ////    Misc
    //////////////////////////////////////////////////////////


    /**
     * Return the Java Object corresponding to this object.
     */
    Object  toJavaObject(Object o);

    /**
     * Return the internal representation of a Java Object.
     */
    Object  fromJavaObject(Object o);

    Object  selectField(Object composite, String fieldName);
    
    void    modifyField(Object composite, String fieldName, Object value);
    
    //////////////////////////////////////////////////////////
    ////    Misc
    //////////////////////////////////////////////////////////
    
    Object  cond(Object c, Thunk v1, Thunk v2);
    
    public static interface Thunk {
    	Object value();
    }
    
}
