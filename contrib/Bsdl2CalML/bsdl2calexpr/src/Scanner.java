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

public class Scanner implements Symbols{
    
    /** Scanned code */
    String code;
    /** Position in code*/
    private int compt;
    
    /** Name of function/variable */
    private String name;
    /** Int value */
    private int value;
    
    /**
     * Crée un nouveau scanner
     * @param code Code à scanner
     */
    public Scanner(String code) {
        this.code = code;
        compt=0;
    }
    
    /** Skip spaces */
    private void skip(){
        while ((compt<code.length())&&((code.charAt(compt)==' ')||(code.charAt(compt)=='\n')||(code.charAt(compt)=='\r')||(code.charAt(compt)=='\t'))) {
            compt++;
        }
    }
    
    private boolean isChar(char check){
        return ((check>='A')&&(check<='Z'))||((check>='a')&&(check<='z'))||(check=='_')||(check==':')||(check>='0')&&(check<='9');
    }
    
    /**
     * Number reprensentign next symbol
     * @return Next symbol number
     * @throws Exception if the symbol is incorrect
     */
    public int nextSymbol()throws Exception{
        if(compt==code.length())
            return EOL;
        skip();
        // If this is the end
        if(compt==code.length())
            return EOL;
        char temp = code.charAt(compt);
        // Cases of 1 letter symbols
        switch(temp) {
            case '(':{
                compt++;
                return LPAR;
            }
            case ')':{
                compt++;
                return RPAR;
            }
            case '=':{
                compt++;
                return EQU;
            }
            case '+':{
                compt++;
                return PLUS;
            }
            case '-':{
                compt++;
                return MINUS;
            }
            case '*':{
                compt++;
                return MUL;
            }
            case '/':{
                compt++;
                return DIV;
            }
            case ',':{
                compt++;
                return VIR;
            }
            case '!':{
                compt++;
                if(code.charAt(compt++)!='='){
                    throw new Exception("ERROR: invalid symbol");
                }
                return DIFF;
            }
            case '<':{
                compt++;
                if(code.charAt(compt)!='='){
                    compt++;
                    return LES;
                } else {
                    return LESE;
                }
            }
            case '>':{
                compt++;
                if(code.charAt(compt)!='='){
                    compt++;
                    return MOR;
                } else {
                    return MORE;
                }
            }
            case '$':{
                compt++;
                temp = code.charAt(compt);
                name="";
                while (isChar(temp)&&(compt<code.length())){
                    if(temp==':') {
                        name="";
                    } else {
                        name+=temp;
                    }
                    compt++;
                    if(compt<code.length())
                        temp = code.charAt(compt);
                }
                return VAR;
            }
        }
        // If it's an int constant
        if((temp>='0')&&(temp<='9')){
            value= 0;
            while((temp>='0')&&(temp<='9')&&(compt<code.length())) {
                value=10*value+temp-'0';
                compt++;
                if(compt<code.length())
                    temp = code.charAt(compt);
            }
            return INT;
        }
        
        // In case of restricted names
        if(code.startsWith("and",compt)) {
            compt += 3;
            return AND;
        }
        if(code.startsWith("not",compt)) {
            compt += 3;
            return NOT;
        }
        if(code.startsWith("or",compt)) {
            compt += 2;
            return OR;
        }
        
        if(((temp>='a')&&(temp<='z'))||((temp>='A')&&(temp<='Z'))) {
            name= "";
            while (isChar(temp)&&(compt<code.length())){
                if(temp==':') {
                    name="";
                } else {
                    name+=temp;
                }
                compt++;
                if(compt<code.length())
                    temp = code.charAt(compt);
            }
            return FUNC;
        }
        
        throw new Exception("Erreur symbole non-valide\n\nSymbole inconnu : \""+code);
    }
    
    /**
     * Return the name of a function/variable
     * @return name of a function/variable
     */
    public String getName(){
        return name;
    }
    
    /** Return the constant value
     * @return constant value
     */
    public int getValue(){
        return value;
    }
    
}
