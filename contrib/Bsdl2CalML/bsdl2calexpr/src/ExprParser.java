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
import org.w3c.dom.Element;
import org.apache.xerces.dom.ElementImpl;
import org.apache.xerces.dom.CoreDocumentImpl;


/**
 *
 * @author sakeller
 */
public class ExprParser implements Symbols {
    
    private static CoreDocumentImpl ownerDoc;
    
    public static Element parseExpression(String expr) throws Exception {
                
        Scanner scan = new Scanner(expr);
        
        ownerDoc = new CoreDocumentImpl();
        
        symbol= scan.nextSymbol();
        
        Element result = new ElementImpl(ownerDoc,"Expr");
        parseStart(scan, result);
        
        result.setAttribute("kind", "BinOpSeq");
        return result;
    }
    
    static int symbol;
    
    private static void parseOp(Scanner scan, Element result) throws Exception {
        Element op = new ElementImpl(ownerDoc,"Op");
        
        if(symbol==AND){
            op.setAttribute("name", "and");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return ;
        }
        if(symbol==OR){
            op.setAttribute("name", "or");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return ;
        }
        if(symbol==EQU){
            op.setAttribute("name", "=");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==DIFF){
            op.setAttribute("name", "!=");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==MOR){
            op.setAttribute("name", "&gt;");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==MORE){
            op.setAttribute("name", "&gt;=");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==LES){
            op.setAttribute("name", "&lt;");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==LESE){
            op.setAttribute("name", "&lt;=");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==PLUS){
            op.setAttribute("name", "+");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==MINUS){
            op.setAttribute("name", "-");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==MUL){
            op.setAttribute("name", "*");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        if(symbol==DIV){
            op.setAttribute("name", "/");
            result.appendChild(op);
            symbol= scan.nextSymbol();
            parseStart(scan, result);
            return;
        }
        return;
    }
    
    private static void parseStart(Scanner scan, Element result) throws Exception {
        if(symbol==IF){
            Element ifop = new ElementImpl(ownerDoc,"Expr");
            ifop.setAttribute("kind", "If");
            Element cond = new ElementImpl(ownerDoc,"Expr");
            cond.setAttribute("kind", "BinOpSeq");
            
            symbol= scan.nextSymbol();
            parseStart(scan, cond);
            ifop.appendChild(cond);
            
            if(symbol!=THEN)
                throw new Exception("Missing \"then\" after the function name");
            symbol= scan.nextSymbol();
            
            Element ok = new ElementImpl(ownerDoc,"Expr");
            ok.setAttribute("kind", "BinOpSeq");
            parseStart(scan, ok);
            ifop.appendChild(ok);
            
            if(symbol!=ELSE)
                throw new Exception("Missing \"else\" after the function name");
            symbol= scan.nextSymbol();
            
            Element nok = new ElementImpl(ownerDoc,"Expr");
            nok.setAttribute("kind", "BinOpSeq");
            parseStart(scan, nok);
            ifop.appendChild(nok);
            
            result.appendChild(ifop);
            
            if(symbol!=END)
                throw new Exception("Missing \"end\" after the function name");
            symbol= scan.nextSymbol();
            return;
        }
        
        if(symbol==NOT){
            Element unary = new ElementImpl(ownerDoc,"Expr");
            unary.setAttribute("kind", "UnaryOp");
            Element notop = new ElementImpl(ownerDoc,"Op");
            notop.setAttribute("name", "not");

            symbol= scan.nextSymbol();
            unary.appendChild(notop);
            parsePar(scan, unary);
            
            result.appendChild(unary);
            return;
        }
        if(symbol==MINUS){
            Element unary = new ElementImpl(ownerDoc,"Expr");
            unary.setAttribute("kind", "UnaryOp");
            Element minop = new ElementImpl(ownerDoc,"Op");
            minop.setAttribute("name", "-");
            
            symbol= scan.nextSymbol();
            unary.appendChild(minop);
            parsePar(scan, unary);
            
            result.appendChild(unary);
            return;
        }
        parsePar(scan, result);
        return;
    }
    
    private static void parsePar(Scanner scan, Element result) throws Exception {
        if(symbol==LPAR){
            Element par = new ElementImpl(ownerDoc,"Expr");
            par.setAttribute("kind", "BinOpSeq");
            
            symbol= scan.nextSymbol();
            while(symbol!=RPAR){
                parseStart(scan, par);
            }
            
            result.appendChild(par);
            
            symbol= scan.nextSymbol();
            parseOp(scan, result);
        } else
            parseEntity(scan, result);
        return ;
    }
    
    private static void parseEntity(Scanner scan, Element result) throws Exception {
        if(symbol==INT){
            Element lit = new ElementImpl(ownerDoc,"Expr");
            lit.setAttribute("kind", "Literal");
            lit.setAttribute("literal-kind", "Integer");
            lit.setAttribute("value", String.valueOf(scan.getValue()));
            symbol= scan.nextSymbol();
            result.appendChild(lit);
            parseOp(scan, result);
            return;
        }
        if(symbol==VAR){
            Element var = new ElementImpl(ownerDoc,"Expr");
            var.setAttribute("kind", "Var");
            var.setAttribute("name", scan.getName());
            symbol= scan.nextSymbol();
            result.appendChild(var);
            parseOp(scan, result);
            return;
        }
        if(symbol==FUNC){
            Element app = new ElementImpl(ownerDoc,"Expr");
            app.setAttribute("kind", "Application");
            
            Element var = new ElementImpl(ownerDoc,"Expr");
            var.setAttribute("kind", "Var");
            var.setAttribute("name", scan.getName());
            app.appendChild(var);
            
            Element args = new ElementImpl(ownerDoc,"Args");

            symbol= scan.nextSymbol();
            if(symbol!=LPAR)
                throw new Exception("Missing \"(\" after the function name");
            do{
                symbol= scan.nextSymbol();
                parseStart(scan, args);
            }while (symbol == VIR);
            
            if(symbol!=RPAR)
                throw new Exception("Missing \")\" after the function name");
            
            app.appendChild(args);
            result.appendChild(app);
            symbol= scan.nextSymbol();
            parseOp(scan, result);
            return;
        }
        return;
    }
}