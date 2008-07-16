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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sakeller
 */
public class ExprParser implements Symbols {
    
    public static String parseExpression(String expr) throws Exception {
        Scanner scan = new Scanner(expr);
        
        symbol= scan.nextSymbol();
        
        String temp = parseStart(scan);
        
        if(binOp){
            return "<Expr kind=\"BinOpSeq\">\n"+temp+"</Expr>\n";
        }
        return temp;
    }
    
    static int symbol;
    
    static boolean binOp;
    
    private static String parseOp(Scanner scan) throws Exception {
        String temp;
        if(symbol==AND){
            temp = "<Op name=\"and\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==OR){
            temp = "<Op name=\"or\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==EQU){
            temp = "<Op name=\"=\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==DIFF){
            temp = "<Op name=\"!=\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==MOR){
            temp = "<Op name=\">\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==MORE){
            temp = "<Op name=\">=\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==LES){
            temp = "<Op name=\"<\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==LESE){
            temp = "<Op name=\"<=\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==PLUS){
            temp = "<Op name=\"+\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==MINUS){
            temp = "<Op name=\"-\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==MUL){
            temp = "<Op name=\"*\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        if(symbol==DIV){
            temp = "<Op name=\"/\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseStart(scan);
            return temp;
        }
        return "";
    }
    
    private static String parseStart(Scanner scan) throws Exception {
        String temp;
        
        if(symbol==NOT){
            symbol= scan.nextSymbol();
            temp = "<Expr kind= \"UnaryOp\">\n";
            temp += "<Op name=\"not\"/>\n";
            temp += parsePar(scan);
            temp += "</Expr>\n";
        }
        if(symbol==MINUS){
            symbol= scan.nextSymbol();
            temp = "<Expr kind= \"UnaryOp\">\n";
            temp += "<Op name=\"-\"/>\n";
            temp += parsePar(scan);
            temp += "</Expr>\n";
        }
        return parsePar(scan);
    }
    
    private static String parsePar(Scanner scan) throws Exception {
        String temp;
        if(symbol==LPAR){
            temp = "<Expr kind=\"BinOpSeq\">\n";
            symbol= scan.nextSymbol();
            while(symbol!=RPAR){
                temp += parseStart(scan);
            }
            temp += "</Expr>\n";
        } else
            temp = parseEntity(scan);
        return temp;
    }
    
    private static String parseEntity(Scanner scan) throws Exception {
        String temp;
        binOp = true;
        if(symbol==INT){
            temp = "<Expr kind=\"Literal\" literal-kind=\"Integer\" value=\""+ scan.getValue() +"\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseOp(scan);
            return temp;
        }
        if(symbol==VAR){
            temp = "<Expr kind=\"Var\" name=\""+ scan.getName() +"\"/>\n";
            symbol= scan.nextSymbol();
            temp += parseOp(scan);
            return temp;
        }
        if(symbol==FUNC){
            temp = "<Expr kind=\"Application\">\n";
            temp += "<Expr kind=\"Var\" name=\""+ scan.getName() +"\"/>\n";
            temp += "<Args>\n";
            symbol= scan.nextSymbol();
            if(symbol!=LPAR)
                throw new Exception("Missing \"(\" after the function name");
            do{
                symbol= scan.nextSymbol();
                temp += parseStart(scan);
            }while (symbol == VIR);
            
            if(symbol!=RPAR)
                throw new Exception("Missing \")\" after the function name");
            
            temp += "</Args>\n";
            temp += "</Expr>\n";
            symbol= scan.nextSymbol();
            return temp;
        }
        binOp = false;
        return "";
    }
    
}