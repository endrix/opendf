/**
* Copyright(c)2008, Samuel Keller, Christophe Lucarz, Joseph Thomas-Kerr 
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the EPFL, University of Wollongong nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY  Samuel Keller, Christophe Lucarz, 
* Joseph Thomas-Kerr ``AS IS'' AND ANY 
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL  Samuel Keller, Christophe Lucarz, 
* Joseph Thomas-Kerr BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/

public interface Symbols {
    // Constantes définissant les symboles
    /** Symbol "(" */
    public static final int LPAR = 0;
    /** Symbol ")" */
    public static final int RPAR = LPAR + 1;
    /** Variable (start with $) */
    public static final int VAR = RPAR + 1;
    /** Symbol "not" */
    public static final int NOT = VAR + 1;
    /** Symbol "and" */
    public static final int AND = NOT + 1;
    /** Symbol "or" */
    public static final int OR = AND + 1;
    /** Symbol "=" */
    public static final int EQU = OR + 1;
    /** Symbol "!=" */
    public static final int DIFF = EQU + 1;
    /** Symbol ">" */
    public static final int MOR = DIFF + 1;
    /** Symbol ">=" */
    public static final int MORE = MOR + 1;
    /** Symbol "<" */
    public static final int LES = MORE + 1;
    /** Symbol "<=" */
    public static final int LESE = LES + 1;
    /** Symbol "+" */
    public static final int PLUS = LESE + 1;
    /** Symbol "-" */
    public static final int MINUS = PLUS + 1;
    /** Symbol "*" */
    public static final int MUL = MINUS + 1;
    /** Symbol "/" */
    public static final int DIV = MUL + 1;
    /** Function */
    public static final int FUNC = DIV + 1;
    /** Int Constant */
    public static final int INT = FUNC + 1;
    /** EOL */
    public static final int EOL = INT + 1;
    /** Symbol "," */
    public static final int VIR = EOL + 1;
}

