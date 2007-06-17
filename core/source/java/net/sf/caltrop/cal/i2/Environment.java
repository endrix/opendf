package net.sf.caltrop.cal.i2;


public interface Environment {
	
	Object  getByName(Object var);
	
	/**
	 * Get the value of the specified variable. Variable objects are compared using the
	 * "equals()" method.
	 * 
	 * If this environment supports positional lookup, this method may return the position
	 * of the variable.
	 * 
	 * @param var The variable name.
	 * @param s The stack to deposit the value and type onto.
	 * @return If non-negative, the "frame position" of the variable; if NOPOS, the 
	 *         variable cannot be referred to by position; if CONSTANT, the variable 
	 *         value can never change.
	 */
	
	long	lookupByName(Object var, ObjectSink s);
	
	/**
	 * 
	 * @param frame The frame number relative to the current frame; the frame number of 
	 *              this frame is 0.
	 * @param varPos The frame position of the variable.
	 * @param s The stack to deposit the value and type onto.
	 */
	
	void 	lookupByPosition(int frame, int varPos, ObjectSink s);
	
	
	long    setByName(Object var, Object value);
	
	void    setByPosition(int frame, int varPos, Object value);

	Object  getVariableName(int frame, int varPos);

	Type    getVariableType(int frame, int varPos);
	
	
    /**
     * Freeze all variables.
     *
     * @see VariableContainer#freeze
     */
    public void         freezeAll();

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
     * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
     * @see Environment.StateVariableContainer
     */
    interface VariableContainer {

        /**
         * Produce the current value contained in this object.
         *
         * @return The current value.
         */
        Object value();

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
     * @author Jörn W. Janneck <janneck@eecs.berkeley.edu>
     */

    interface StateVariableContainer extends VariableContainer {
        void    setValue(Object value);
    }
    


	final public long NOPOS = -1;
	final public long CONSTANT = -2;
}
