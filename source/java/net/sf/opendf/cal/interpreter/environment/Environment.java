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

package net.sf.opendf.cal.interpreter.environment;

import java.util.Map;
import java.util.Set;

/**
 * An environment is a structure that assigns ("binds") values to identifiers. It is typically cascaded, i.e. it
 * consists of a list of "local" environments, each mapping a number of variables to values, where earlier bindings
 * (in that list) "shadow" later ones.
 * <p>
 * In addition, the environment provides support for special computational treatment of variables, such as
 * lazy evaluation, where variable values will not be determined until their first use.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 */

public interface Environment {

    /**
     * Get the current value of the specified variable. This method respects
     * {@link VariableContainer variable containers}, i.e. if a variable is bound to such a
     * container, call its {@link VariableContainer#value() value()} method.
     *
     * @throws net.sf.opendf.cal.interpreter.InterpreterException If variable cannot be found in this environment.
     * @param variable The variable.
     * @return Its value.
     * @see VariableContainer
     */
    public Object       get(Object variable);

    /**
     * Set the value of the specified variable to the specified object. This method respects
     * {@link StateVariableContainer state variable containers}, i.e. if a variable is bound to such a
     * container, call its {@link StateVariableContainer#setValue(java.lang.Object) setValue(Object)} method.
     *
     * @throws net.sf.opendf.cal.interpreter.InterpreterException If variable cannot be found in this environment.
     * @param variable The variable.
     * @param value The new value.
     * @see StateVariableContainer
     */
    public void         set(Object variable, Object value);
    public void         set(Object variable, Object [] location, Object value);

    /**
     * Introduce a new variable by the name <tt>variable</tt> into the local frame, binding it to the
     * specified <tt>value</tt>.
     * <p>
     * The <tt>value</tt> object may also be a {@link VariableContainer variable container} or a
     * {@link StateVariableContainer state variable container}. In this case, subsequent accesses to this variable
     * will be delegated to that container. (A variable container that is not also a state variable container may get
     * overwritten by the next {@link #set(java.lang.Object, java.lang.Object) set(String, Object)} call.
     *
     * @param variable The new variable name.
     * @param value The initial value.
     * @throws net.sf.opendf.cal.interpreter.InterpreterException If there already exists a variable by that name in the local frame.
     */
    public void         bind(Object variable, Object value);

    /**
     * Return the set of local variables, which are the variables defined in the current frame.
     *
     * @return The set of local variables.
     */
    public Set          localVars();

    /**
     * Determines whether the variables is a local variable. Is equivalent to<br>
     * <tt>localVars().contains(variable)</tt>
     * <p>
     * The reason for this method is to allow environments to provide this test without
     * having to explicitly construct a Set object.
     *
     * @param variable The variable to test.
     * @return  True is the variable is bound inside the local frame.
     */

    public boolean      isLocalVar(Object variable);

    /**
     * Computes a map of all local bindings.
     *
     * @return The map of all local bindings.
     */

    public Map          localBindings();
    /**
     * Create a new frame, whose parent frame is this one.
     *
     * @return The new frame.
     */
    public Environment  newFrame();

    /**
     * Create a new frame, whose parent frame is the specified one.
     *
     * @param  parent The parent of the new frame.
     * @return The new frame.
     */
    public Environment  newFrame(Environment parent);

    /**
     * Freeze all local variables.
     *
     * @see VariableContainer#freeze
     */
    public void         freezeLocal();


    /**
     * A variable container is a wrapper for the value of a variable. It may be used by the interpreter to control
     * and log read access to a variable, or to implement deferred (lazy) evaluation on the variable.
     * <p>
     * If a variable is bound to a variable container, the {@link #get get(...)}  methods of the environment will
     * not return the container, but use value produced by the corresponding {@link #value value(...)} methods instead.
     * <p>
     * Setting a variable bound to a variable container will overwrite the container, unless it is a
     * <i>state variable container</i>, a subinterface of this one.
     * <p>
     * In order for this container to behave consistently with the general semantics of assignment and variable
     * lookup, it must yield the same value on each invocation of the {@link #value value(...)} (for the same parameters),
     * during the period where such consistency is desired.
     *
     * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
     * @see Environment.StateVariableContainer
     */
    interface VariableContainer {

        /**
         * Produce the current value contained in this object.
         *
         * @return The current value.
         */
        Object  value();

        Object  value(Object [] location);

        /**
         * Freeze the current value. This means that the container needs to take action <em>as if</em>  the
         * current value would be needed, without necessarily producing it. The effect of this method should be
         * equivalent to calling a {@link #value value(...)} method and then discarding the result. However, the container
         * has more flexibility on this call, because it does not actually have to produce the result.
         * <p>
         * This method is usually used in implementing lazy evaluation involving state variables that may change.
         * In this case, we might want to "freeze" the value in the current environment, before proceeding to change it.
         * However, this may be achieved in various ways. One is to just compute the value and store it. Another would
         * be to identify the free variables that may change and store <em>their values</em> instead, so that these
         * may be used in later evaluation.
         */
        void    freeze();
    }

    /**
     * A state variable container encapsulates the value of a variable that is expected to be assigned to, and
     * whose assignments need to be intercepted. In addition to the functionality of {@link Environment.VariableContainer VariableContainer},
     * assignments to a binding of an instance of this class do <em>not</em> overwrite the container. Instead,
     * the corresponding {@link #setValue setValue(...)} method is used.
     * <p>
     * In order for this container to behave consistently with the general semantics of assignment and variable
     * lookup, during the period where such consistency is desired, this container must yield the same value on
     * each invocation of the {@link #value value(...)} (for the same parameters) until a call to a {@link #setValue setValue(...)}
     * method changes that value. Of course, a key application of this interface is to allow external software to
     * manipulate the state, e.g. by rolling back to a previous state.
     *
     * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
     */

    interface StateVariableContainer extends VariableContainer {
        void    setValue(Object value);
        void    setValue(Object [] location, Object value);
    }
    
}
