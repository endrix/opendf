<?xml version="1.0" encoding="UTF-8"?>
<!--*
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
*-->

<!DOCTYPE stylesheet SYSTEM "entities.dtd">

<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    
    xmlns:bs2x="urn:mpeg:mpeg21:2003:01-DIA-BSDL2x-NS"
    
    xmlns:math="http://exslt.org/math"
    
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    
    xmlns:bs0="urn:mpeg:mpeg21:2003:01-DIA-BSDL0-NS" 
    
    xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS" 
    
    xmlns:rvc="urn:mpeg:2006:01-RVC-NS"
    
    version="2.0">
    
    <xsl:template name="int2bit">
        <xsl:param name="value" as="xsd:decimal"/>
        <xsl:param name="position" as="xsd:integer"/>
        <xsl:param name="position2" as="xsd:integer">3</xsl:param>
        
        <xsl:if test="$position2 > 0">
            <xsl:call-template name="int2bit">
                <xsl:with-param name="value" select="floor($value div 2)"/>
                <xsl:with-param name="position" select="$position"/>
                <xsl:with-param name="position2" select="$position2 - 1"/>
            </xsl:call-template>
            <xsl:text>&lt;Op name="and"/&gt;&nl;</xsl:text>
        </xsl:if>     
        
        <xsl:if test="$value mod 2 = 0">
            <xsl:text>&lt;Expr kind="UnaryOp"&gt;&nl;</xsl:text>
            <xsl:text>&lt;Op name="not"/&gt;&nl;</xsl:text>
        </xsl:if>
        
        <xsl:text>&lt;Expr kind="Indexer"&gt;&nl;</xsl:text>
        <xsl:text>&lt;Expr kind="Var" name="b"/&gt;&nl;</xsl:text>
        <xsl:text>&lt;Args&gt;&nl;</xsl:text>
        <xsl:text>&lt;Expr kind="Literal" literal-kind="Integer" value="</xsl:text>
        <xsl:value-of select="$position+$position2"/>
        <xsl:text>"/&gt;&nl;</xsl:text>
        <xsl:text>&lt;/Args&gt;&nl;</xsl:text>
        <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>
        
        <xsl:if test="$value mod 2 = 0">
            <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>
        </xsl:if>
        
    </xsl:template>

    <xsl:template name="hex2bit">
        
        <xsl:param name="value" as="xsd:string"/>
        
        <xsl:param name="position" as="xsd:integer">0</xsl:param>
       
        
        <!-- isolate last hex digit (and convert it to upper case) -->
        
        <xsl:variable name="hex-digit" select="translate(substring($value,1,1),'abcdef','ABCDEF')"/>
        <!-- check that hex digit is valid -->
        
        <xsl:if test="contains('0123456789ABCDEF',$hex-digit) and (string-length($hex-digit) &gt; 0)">

            <!-- OK so far -->
            
            <xsl:variable name="remainder" select="substring($value,2)"/>
            
            <xsl:variable name="this-digit-value" select="string-length(substring-before('0123456789ABCDEF',$hex-digit))"/>
        
            <xsl:call-template name="int2bit">
                <xsl:with-param name="value" select="$this-digit-value"/>
                <xsl:with-param name="position" select="$position"/>
            </xsl:call-template>
            
            <!-- determine whether this is the end of the hex string -->
            
            <xsl:if test="string-length($remainder) &gt; 0">          
                <!-- recurse to self for next digit -->
                
                <xsl:text>&lt;Op name="and"/&gt;&nl;</xsl:text>
                <xsl:call-template name="hex2bit">
                    <xsl:with-param name="value" select="$remainder"/>
                    <xsl:with-param name="position" select="$position + 4"/>
                </xsl:call-template>
            </xsl:if>
                    
        </xsl:if>
        
    </xsl:template>
    
    
    <xsl:template name="input">
        <xsl:param name="length" required="no"/>
        <xsl:param name="name">b</xsl:param>
        <xsl:param name="port">bitstream</xsl:param>

        <xsl:text>&lt;Input kind="Elements" port="</xsl:text>
        <xsl:value-of select="$port"/>
        <xsl:text>"&gt;&nl;</xsl:text>
        <xsl:text>&lt;Decl kind="Input" name="</xsl:text>
        <xsl:value-of select="$name"/>
        <xsl:text>"/&gt;&nl;</xsl:text>
        
        <xsl:if test="string-length($length) &gt; 0">
            <xsl:text>&lt;Repeat&gt;&nl;</xsl:text>
            
            <xsl:text>&lt;Expr kind="Var" name="</xsl:text>
            <xsl:value-of select="$length"/>
            <xsl:text>"/&gt;&nl;</xsl:text>
            
            <xsl:text>&lt;/Repeat&gt;&nl;</xsl:text>
        </xsl:if>
        <xsl:text>&lt;/Input&gt;&nl;</xsl:text>
    </xsl:template>
    
    <xsl:template name="output">
        <xsl:param name="length" required="no"/>
        <xsl:param name="port" required="yes"/>
        
        <xsl:text>&lt;Output port="</xsl:text>
        <xsl:value-of select="$port"/>
        <xsl:text>"&gt;&nl;</xsl:text>
        <xsl:text>&lt;Expr kind="Var" name="b"/&gt;&nl;</xsl:text> 
        <xsl:if test="string-length($length) &gt; 0">
            <xsl:text>&lt;Repeat&gt;&nl;</xsl:text>
            
            <xsl:text>&lt;Expr kind="Var" name="</xsl:text>
            <xsl:value-of select="$length"/>
            <xsl:text>"/&gt;&nl;</xsl:text>
            
            <xsl:text>&lt;/Repeat&gt;&nl;</xsl:text>
        </xsl:if>
        <xsl:text>&lt;/Output&gt;&nl;</xsl:text>
        
    </xsl:template>
    
    <xsl:template name="vlcguard">
        <xsl:param name="value" required="yes"/>
        
        <xsl:text>&lt;Expr kind="BinOpSeq"&gt;&nl;</xsl:text>
        <xsl:text>&lt;Expr kind="var" name="f"/&gt;&nl;</xsl:text>
        <xsl:text>&lt;Op name="="/&gt;&nl;</xsl:text>
        <xsl:text>&lt;Expr kind="Literal" literal-kind="Integer" value="</xsl:text>
        <xsl:value-of select="$value"/>
        <xsl:text>"/&gt;&nl;</xsl:text>
        <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>
    </xsl:template>

    <xsl:template match="xsd:element" priority="50" mode="actions">
        
        <xsl:variable name="typename">
            <xsl:if test="not(@type='vlc')">
                <xsl:variable name="typenamer">
                    <xsl:apply-templates select="@type | *" mode="actionlength"/>
                </xsl:variable>
                <xsl:value-of select="concat(rvc:constant($typenamer),&lengthSuffix;)"/>
            </xsl:if>
        </xsl:variable>
       
 <!--      <xsl:if test="not(*)"> -->
        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&readActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="inputs">
                <xsl:call-template name="input">
                    <xsl:with-param name="length" select="$typename"/>
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="outputs">
                <xsl:if test="@rvc:port">
                    <xsl:call-template name="output">
                        <xsl:with-param name="port" select="@rvc:port"/>
                        <xsl:with-param name="length" select="$typename"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="do">
                <xsl:if test="@bs0:variable">
                    
                    <xsl:text>&lt;Stmt kind="Call"&gt;&nl;</xsl:text>
                    <xsl:text>&lt;Expr kind="Var" name="bool2int"/&gt;&nl;</xsl:text>
                    <xsl:text>&lt;Args&gt;&nl;</xsl:text>
                    <xsl:text>&lt;Expr kind="Var" name="b"/&gt;&nl;</xsl:text>
                    <xsl:text>&lt;Expr kind="Var" name="</xsl:text>
                    <xsl:value-of select="$typename"/>
                    <xsl:text>"/&gt;&nl;</xsl:text>
                    <xsl:text>&lt;/Args&gt;&nl;</xsl:text>
                    <xsl:text>&lt;/Stmt&gt;&nl;</xsl:text>
                    
                    <xsl:text>&lt;Stmt kind="Assign" name="</xsl:text>
                    <xsl:value-of select="@name"/>
                    <xsl:text>"&gt;&nl;</xsl:text>
                    <xsl:text>&lt;Expr kind="Var" name="output"/&gt;&nl;</xsl:text>
                    <xsl:text>&lt;/Stmt&gt;&nl;</xsl:text>
                    
                </xsl:if>
                <xsl:if test="xsd:annotation/xsd:appinfo/bs2x:variable">
                    <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="actionexpr"/>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
<!--       </xsl:if> -->
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type='vlc']" mode="actions" priority="30">
        
        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&notFinishedActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="inputs">
                <xsl:call-template name="input">
                    <xsl:with-param name="name">f</xsl:with-param>
                    <xsl:with-param name="port" select="concat(@rvc:port,'&vlcActionSuffix;')"/>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:call-template name="vlcguard">
                    <xsl:with-param name="value">0</xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
        </xsl:call-template>

        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&finishActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="inputs">
                <xsl:call-template name="input">
                    <xsl:with-param name="name">f</xsl:with-param>
                    <xsl:with-param name="port" select="concat(@rvc:port,'&vlcActionSuffix;')"/>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:call-template name="vlcguard">
                    <xsl:with-param name="value">1</xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@bs2:nOccurs] | xsd:group[@bs2:nOccurs]" priority="20" mode="actions">

        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&testActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:text>&lt;Expr kind="BinOpSeq"&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;Expr kind="Var" name="</xsl:text>
                <xsl:value-of select="@name"/>
                <xsl:text>&countSuffix;</xsl:text>
                <xsl:text>"/&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;Op name="&gt;"/&gt;&nl;</xsl:text>
                <xsl:text>&lt;Expr kind="Literal" literal-kind="Integer" value="</xsl:text>
                <xsl:value-of select="@bs2:nOccurs"/>
                <xsl:text>"/&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>

            </xsl:with-param>
            
            <xsl:with-param name="do">
                
                <xsl:text>&lt;Stmt kind="Assign" name="</xsl:text>
                <xsl:value-of select="@name"/>
                <xsl:text>&countSuffix;</xsl:text>
                <xsl:text>"&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;Expr kind="BinOpSeq"&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;Expr kind="Var" name="</xsl:text>
                <xsl:value-of select="@name"/>
                <xsl:text>&countSuffix;</xsl:text>
                <xsl:text>"/&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;Op name="-"/&gt;&nl;</xsl:text>
                <xsl:text>&lt;Expr kind="Literal" literal-kind="Integer" value="1"/&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>
                
                <xsl:text>&lt;/Stmt&gt;&nl;</xsl:text>
                
            </xsl:with-param>
            
        </xsl:call-template>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="*[@bs2:if]" priority="15" mode="actions">
        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&validActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="guard">
                <xsl:text>&lt;BoolExpr expr="</xsl:text>
                <xsl:value-of select="@bs2:if"/>
                <xsl:text>"/&gt;&nl;</xsl:text>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
        
    </xsl:template>
        
    <xsl:template match="*[@bs2:ifNext]" priority="15" mode="actions">
        
        <xsl:variable name="typename">
            <xsl:apply-templates select="." mode="followinglength"/>
        </xsl:variable>
        
        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&validActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="inputs">
                <xsl:call-template name="input">
                    <xsl:with-param name="length" select="$typename"/>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="outputs">
                <xsl:if test="@rvc:port"> <!-- Ca c'est faux, faut prendre celui du childnext -->
                    <xsl:call-template name="output">
                        <xsl:with-param name="port" select="@rvc:port"/> <!-- IDEM -->
                        <xsl:with-param name="length" select="$typename"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                
                <xsl:text>&lt;Expr kind="BinOpSeq"&gt;&nl;</xsl:text>
                
                <xsl:call-template name="hex2bit">
                    <xsl:with-param name="value" select="@bs2:ifNext"/>
                </xsl:call-template>
                
                <xsl:text>&lt;/Expr&gt;&nl;</xsl:text>
                
            </xsl:with-param>
            
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="xsd:choice" priority="5" mode="actions">
        <xsl:apply-templates mode="actions" select="*"/>
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:group | xsd:complexType |  xsd:all | xsd:element[child::element()]" priority="5" mode="actions">
        <xsl:apply-templates mode="actions" select="*"/>
    </xsl:template>
    
    <xsl:template match="*" mode="actions" priority="-1000"/>
    
       <xsl:template  match="xsd:simpleType" mode="actionlength">
        <xsl:value-of select="@name"/>
    </xsl:template>

   <xsl:template  match="@type[namespace-uri-from-QName(resolve-QName(.,..))=&xsdNS;]" mode="actionlength">
        <xsl:value-of select="."/>
   </xsl:template>
    
    <xsl:template match="*" mode="actionlength" priority="-1000"/>
    
    
    <xsl:template match="xsd:group" mode="followinglength"  priority="5">
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:apply-templates select="$group/*[1]" mode="#current"/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="followinglength"  priority="5">
        
        <xsl:variable name="tmp">
            <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="#current"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="tmp">
                <xsl:value-of select="$tmp"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:next-match/>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <xsl:template match="xsd:element[@ref]" mode="followinglength" priority="5">
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>
        
        <xsl:apply-templates select="$eltType" mode="#current"/>
        
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:all | xsd:complexType | xsd:element[child::element()]" mode="followinglength" priority="2">
        <xsl:apply-templates mode="#current" select="*[1]"/>
    </xsl:template>
    
    <xsl:template match="xsd:element" mode="followinglength" priority="0">
        <xsl:if test="not(@type='vlc')">
            <xsl:variable name="typenamer">
                <xsl:apply-templates select="@type | *" mode="actionlength"/>
            </xsl:variable>
            <xsl:value-of select="concat(rvc:constant($typenamer),&lengthSuffix;)"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="*" mode="followinglength" priority="-1000"/>
    
    <xsl:template match="bs2x:variable" mode="actionexpr">
        <xsl:text>&lt;Stmt kind="Assign" name="</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>"&gt;&nl;</xsl:text>
        
        <xsl:text>&lt;IntExpr expr="</xsl:text>
        <xsl:value-of select="@value"/>
        <xsl:text>"/&gt;&nl;</xsl:text>
        
        <xsl:text>&lt;/Stmt&gt;&nl;</xsl:text>
    </xsl:template>
    
</xsl:stylesheet>
