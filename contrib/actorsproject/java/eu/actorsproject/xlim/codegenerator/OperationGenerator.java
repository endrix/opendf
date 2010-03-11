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

package eu.actorsproject.xlim.codegenerator;

import eu.actorsproject.xlim.XlimOperation;
import eu.actorsproject.xlim.XlimSource;

public interface OperationGenerator {
	/**
	 * @return true if this handler supports generateExpression() in addition to generateStatement()
	 * 
	 * The effect of returning "false" is that the generateStatement() method will be used (rather than
	 * generateExpression()) and a temporary variable will be allocated for each of the output ports.  
	 */
	
	public boolean hasGenerateExpression(XlimOperation op);
	
	/**
	 * @return true if it's possible and preferable to re-evaluate the root of the expression tree
	 * 
	 *  The effect of returning "true" is that generateExpression() will be used multiple times 
	 *  rather than introducing a temporary variable (implies hasGenerateExpression). 
	 */
	public boolean reEvaluate(XlimOperation op);
	
	/**
	 * @param op       the operation at the root of the tree (guaranteed to be supported by the handler)
	 * @param gen      operation generator, which provides the context of the expression tree
	 * Generates C code for an expression tree 
	 */
	public void generateExpression(XlimOperation op, ExpressionTreeGenerator gen);
	
	/**
	 * @param op       the operation at the root of the tree (guaranteed to be supported by the handler)
	 * @param gen      operation generator, which provides the context of the expression tree
	 * 
	 * Generates a C statement, which corresponds to the expression tree
	 * default implementation is to generate an assignment to a single output port
	 * thus needs to be overridden for operations with zero or more than one output port
	 */
	public void generateStatement(XlimOperation op, ExpressionTreeGenerator gen);
	
	
	/**
	 * @param source  Source of copy operation
	 * @param dest    Destination of copy operation
	 * @param gen     operation generator, which provides the context of the copy
	 * 
	 * Generates a copy operation dest := source
	 */
	public void generateCopy(XlimSource source, XlimSource dest, ExpressionTreeGenerator gen);
}
