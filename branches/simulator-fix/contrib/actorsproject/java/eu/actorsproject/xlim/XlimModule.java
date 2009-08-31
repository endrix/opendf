/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package eu.actorsproject.xlim;

import eu.actorsproject.util.XmlElement;

/**
 * @author ecarvon
 *
 */
public interface XlimModule extends XmlElement {
	String getKind();
	
	/**
	 * @return module, to which this (sub-)module belongs (null for top-level modules)
	 */
	XlimModule getParentModule();
	
	
	/**
	 * @param module
	 * @return Least common ancestor of this XlimModule and module 
	 * (first module to enclose both of them). By definition every module is an ancestor
	 * of itself.
	 */
	XlimModule leastCommonAncestor(XlimModule module);
	
	/**
	 * @return task module, which encloses this module
	 */
	XlimTaskModule getTask();
	
	/**
	 * @return the test module of the least enclosing if/loop module (if any)
	 * By convention if- and loop-modules are not control dependent on themselves.
	 * Further, we assume that test-modules are control-depenent on themselves
	 * (i.e. in its enclosing if/loop), although only test-modules of loops really are.
	 */
	XlimTestModule getControlDependence();
	
	<Result,Arg> Result accept(Visitor<Result,Arg> visitor, Arg arg);
	
	interface Visitor<Result,Arg> {
		Result visitContainerModule(XlimContainerModule m, Arg arg);
		Result visitIfModule(XlimIfModule m, Arg arg);
		Result visitLoopModule(XlimLoopModule m, Arg arg);
		Result visitTaskModule(XlimTaskModule m, Arg arg);
		Result visitTestModule(XlimTestModule m, Arg arg);
	}
}
