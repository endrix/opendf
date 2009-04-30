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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This environment can be used to log all accesses to variables, which it delegates to its parent
 * environment. It provides constructor parameters to configure the kinds of accesses it will create a log
 * entry for. By default, it only logs variable assignments.
 * <p>
 * Its two nested classes, AccessLog and AccessRecord provide a structured view onto the access
 * history of any variable.
 *
 * @see AccessLog
 * @see AccessRecord
 *
 *  @author J�rn W. Janneck <janneck@eecs.berkeley.edu>
 */

public class AccessLoggingEnvironment implements Environment {

    //
    //  implement: Environment
    //

    public void set(Object variable, Object value) {

        parent.set(variable, value);

        if (logAssignments) {
            addRecord(variable, new AccessRecord(variable, event++, logtypeAssignment, value, null));
        }
    }

    public void bind(Object variable, Object value) {

        parent.bind(variable, value);

        if (logBinding) {
            addRecord(variable, new AccessRecord(variable, event++, logtypeBinding, value, null));
        }
    }

    public void freezeLocal() {
        parent.freezeLocal();
    }

    public Object get(Object variable) {
        Object value = parent.get(variable);

        if (logReferences) {
            addRecord(variable, new AccessRecord(variable, event++, logtypeReference, value, null));
        }

        return value;
    }

    public boolean isLocalVar(Object variable) {
        return parent.isLocalVar(variable);
    }

    public Map localBindings() {
        return parent.localBindings();
    }

    public Set localVars() {
        return parent.localVars();
    }

    public Environment newFrame() {
        return parent.newFrame(this);
    }

    public Environment newFrame(Environment env) {
        return parent.newFrame(env);
    }

    public void set(Object variable, Object[] location, Object value) {

        parent.set(variable, location, value);

        if (logMutations) {
            addRecord(variable, new AccessRecord(variable, event++, logtypeMutation, value, location));
        }
    }

    //
    //  AccessLoggingEnvironment
    //

    /**
     * Clear all logs.
     */

    public void  clearLogs() {
        logs = new HashMap();
        recs = new ArrayList();
    }

    /**
     * Return the set of all variables for which logging records were created since the last time
     * the logs were cleared.
     *
     * @return Set of all variables for which there is a record.
     */
    public Set   loggedVars() {
        return logs.keySet();
    }

    /**
     * Return the log for the specified variable.
     *
     * @param var The variable.
     * @return  The log for the variable.
     */
    public AccessLog  getLog(Object var) {
        return (AccessLog)logs.get(var);
    }

    /**
     * Return the list of all records created since the last time the loags were cleared.
     * The records are sorted in the order in which they were created, i.e. the order in which the
     * accesses happened.
     *
     * @return List of all records.
     */
    public List   getRecords() {
        return recs;
    }


    //
    //  ctor
    //

    public AccessLoggingEnvironment(Environment parent) {
        this(parent, true, false, false, false);
    }

    public AccessLoggingEnvironment(Environment parent,
                             boolean logAssignments, boolean logBinding,
                             boolean logMutations, boolean logReferences) {
        this.parent = parent;
        this.logAssignments = logAssignments;
        this.logBinding = logBinding;
        this.logMutations = logMutations;
        this.logReferences = logReferences;
    }



    //
    //  private
    //

    private void addRecord(Object variable, AccessRecord rec) {
        AccessLog log = (AccessLog)logs.get(variable);
        if (log == null) {
            log = new AccessLog(variable);
            logs.put(variable, log);
        }
        log.add(rec);
        recs.add(rec);
    }

    private Environment parent;

    private Map         logs = new HashMap();
    private List        recs = new ArrayList();

    private long        event = 0;

    private boolean     logAssignments;
    private boolean     logBinding;
    private boolean     logMutations;
    private boolean     logReferences;

    //
    //  nested  classes
    //

    /**
     * An AccessLog contains all access records for a variable.
     */
    public static class AccessLog {

        /**
         * Add record to the log for the variable.
         *
         * @param rec The record.
         */
        public void  add(AccessRecord rec) {
            history.add(rec);
        }

        /**
         * Return the variable that this log is for.
         *
         * @return The variable.
         */
        public Object  getVariable() {
            return variable;
        }

        /**
         * Return all records for the variable this log is for, in the order
         * in which they were created.
         *
         * @return The records for this variable.
         */
        public List   getHistory() {
            return history;
        }

        /**
         * Return the last record in this log, or null if this log is empty.
         *
         * @return The last record in this log.
         */
        public AccessRecord getLastRecord() {
            if (history.size() == 0)
                return null;
            else
                return (AccessRecord)history.get(history.size() - 1);
        }

        public AccessLog (Object variable) {
            this.variable = variable;
            history = new ArrayList();
        }

        private Object   variable;
        private List     history;
    }

    /**
     * An AccessRecord maintains the information for one variable access.
     */
    public static class AccessRecord {

        /**
         * The variable that this access referred to.
         */
        public Object     name;

        /**
         * The internal event number, which is used to order access records relative to one logging environment.
         */
        public long       eventNumber;

        /**
         * The kind of access.
         *
         * @see AccessLoggingEnvironment.logtypeAssignment
         * @see AccessLoggingEnvironment.logtypeBinding
         * @see AccessLoggingEnvironment.logtypeMutation
         * @see AccessLoggingEnvironment.logtypeReference
         */
        public int        type;

        /**
         * The value involved in the access, i.e. either the value that was assigned, or the
         * value that resulted from a  reference.
         */
        public Object     value;

        /**
         * If applicable, the location that was involved in this access. If none, this is null.
         */
        public Object []  location;

        AccessRecord(Object name, long eventNumber, int type, Object value, Object [] location) {
            this.name = name;
            this.eventNumber = eventNumber;
            this.type = type;
            this.value = value;
            this.location = location;
        }
    }

    public static final int  logtypeAssignment = 1;
    public static final int  logtypeBinding = 2;
    public static final int  logtypeMutation = 3;
    public static final int  logtypeReference = 4;


}
