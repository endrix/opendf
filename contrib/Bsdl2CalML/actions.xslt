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
    
    xmlns:cal="java:ExprParser" 
    
    xmlns:unique="java:Unique" 
    
    version="2.0">
    
    <xsl:template name="modDotText">
        <xsl:param name="value" />
        <xsl:param name="typename" tunnel="yes"/>
        <xsl:param name="isvlc" as="xsd:boolean" tunnel="yes">false</xsl:param>
        <xsl:choose>
            <xsl:when test="contains($value,'bs1x:numBits(./text())')">
                <xsl:value-of select="substring-before($value,'bs1x:numBits(./text())')" />
                <xsl:text>$typename</xsl:text>
            </xsl:when>
            <xsl:when test="contains($value,'./text()')">
                <xsl:value-of select="substring-before($value,'./text()')" />
                <xsl:choose>
                    <xsl:when test="$isvlc">
                        <xsl:text>$data</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>$output</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
                
                <xsl:call-template name="modDotText">
                    <xsl:with-param name="value" select="substring-after($value,'./text()')" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
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
            <Op name="and"/>
        </xsl:if>     
        
        <xsl:choose>
            <xsl:when test="$value mod 2 = 0">
                <Expr kind="UnaryOp">
                    <Op name="not"/>
                    
                    <Expr kind="Indexer">
                        <Expr kind="Var" name="b"/>
                        <Args>
                            <Expr kind="Literal" literal-kind="Integer">
                                <xsl:attribute name="value" select="$position+$position2"/>
                            </Expr>
                        </Args>
                    </Expr>
                </Expr>
            </xsl:when>
            <xsl:otherwise>
                <Expr kind="Indexer">
                    <Expr kind="Var" name="b"/>
                    <Args>
                        <Expr kind="Literal" literal-kind="Integer">
                            <xsl:attribute name="value" select="$position+$position2"/>
                        </Expr>
                    </Args>
                </Expr>
            </xsl:otherwise>
        </xsl:choose>
        
        
    </xsl:template>
    
    <xsl:template name="hex2bit">
        
        <xsl:param name="value" as="xsd:string"/>
        
        <xsl:param name="smalllength" as="xsd:integer">0</xsl:param> 
        
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
                <xsl:with-param name="position2" select="($smalllength + 3)  mod 4"></xsl:with-param>
            </xsl:call-template>
            
            <!-- determine whether this is the end of the hex string -->
            
            <xsl:if test="string-length($remainder) &gt; 0">          
                <!-- recurse to self for next digit -->
                
                <Op name="and"/>
                <xsl:call-template name="hex2bit">
                    <xsl:with-param name="value" select="$remainder"/>
                    <xsl:with-param name="position" select="$position + ($smalllength + 3) mod 4 + 1"/>
                </xsl:call-template>
            </xsl:if>
            
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template name="bit2bit">
        
        <xsl:param name="value" as="xsd:string"/>
        
        <xsl:variable name="remainder" select="substring($value,2)"/>
        
        <xsl:if test="string-length($remainder) &gt; 0"> 
            <xsl:call-template name="bit2bitc">
                <xsl:with-param name="value" select="$remainder"/>
                <xsl:with-param name="position" select="0"/>
            </xsl:call-template>
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template name="bit2bitc">
        
        <xsl:param name="value" as="xsd:string"/>
        <xsl:param name="position" as="xsd:integer">0</xsl:param>
        
        <xsl:variable name="remainder" select="substring($value,2)"/>
        
        <xsl:choose>
            <xsl:when test="substring($value,1,1) = '0'">
                <Expr kind="UnaryOp">
                    <Op name="not"/>
                    <Expr kind="Indexer">
                        <Expr kind="Var" name="b"/>
                        <Args>
                            <Expr kind="Literal" literal-kind="Integer">
                                <xsl:attribute name="value" select="$position"/>
                            </Expr> 
                        </Args>
                    </Expr>
                </Expr>
            </xsl:when>
            <xsl:otherwise>
                <Expr kind="Indexer">
                    <Expr kind="Var" name="b"/>
                    <Args>
                        <Expr kind="Literal" literal-kind="Integer">
                            <xsl:attribute name="value" select="$position"/>
                        </Expr> 
                    </Args>
                </Expr>
            </xsl:otherwise>
        </xsl:choose>
        
        
        <xsl:if test="string-length($remainder) &gt; 0">          
            <!-- recurse to self for next bool -->
            <Op name="and"/>
            <xsl:call-template name="bit2bitc">
                <xsl:with-param name="value" select="$remainder"/>
                <xsl:with-param name="position" select="$position + 1"/>
            </xsl:call-template>
        </xsl:if>
        
    </xsl:template>
    
    
    <xsl:template name="input">
        <xsl:param name="length" required="no" select="'nolength'"/>
        <xsl:param name="name">b</xsl:param>
        <xsl:param name="port">bitstream</xsl:param>
        
        <Input kind="Elements">
            <xsl:attribute name="port" select="$port"/>
            <Decl kind="Input">
                <xsl:attribute name="name" select="$name"/>
            </Decl> 
            
            <xsl:if test="$length = 'nolength'">
                <Repeat>  
                    <xsl:copy-of select="$length" copy-namespaces="no"/>
                </Repeat>
            </xsl:if>
        </Input>
    </xsl:template>
    
    <xsl:template name="output">
        <xsl:param name="port" required="yes"/>
        
        <Output>
            <xsl:attribute name="port" select="$port"/>
            <Expr kind="Var" name="output"/> 
        </Output>
        
    </xsl:template>
    
    <xsl:template name="outputvlc">
        <xsl:param name="port" required="yes"/>
        
        <Output>
            <xsl:attribute name="port" select="$port"/>
            <Expr kind="Var" name="b"/> 
        </Output>
        
    </xsl:template>
    
    <xsl:template name="vlcguard">
        <xsl:param name="value" required="yes"/>
        
        <Expr kind="BinOpSeq">
            <Expr kind="Var" name="f"/>
            <Op name="="/>
            <Expr kind="Literal" literal-kind="Integer">
                <xsl:attribute name="value" select="$value"/>
            </Expr>
        </Expr>
    </xsl:template>
    
    <xsl:template match="xsd:element" priority="50" mode="actions">
        
        <xsl:if test="not(@type= 'bs1:align8' or @type= 'bs1:align16' or @type= 'bs1:align32')">
            
            <xsl:variable name="union" as="xsd:integer">
                <xsl:variable name="u">
                    <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="unioncount"/>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="string-length($u) &gt; 0">
                        <xsl:value-of select="$u"/>
                    </xsl:when>
                    <xsl:otherwise>0</xsl:otherwise>
                </xsl:choose>
                
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="$union &gt; 0">
                    <xsl:apply-templates select="." mode="actionsunion">
                        <xsl:with-param name="union" select="$union"/>
                    </xsl:apply-templates>
                </xsl:when>
                <xsl:otherwise>
                    
                    <xsl:variable name="typename">
                        <xsl:if test="not(@type='vlc')">
                            <xsl:variable name="notfix">
                                <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="notFixLength"/>
                            </xsl:variable>
                            <xsl:choose>
                                <xsl:when test="string-length($notfix) &gt; 0">
                                    <xsl:copy-of select="cal:parseExpression($notfix)" copy-namespaces="no"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Expr kind="Var">
                                        <xsl:attribute name="name">
                                            <xsl:variable name="typenamer">
                                                <xsl:apply-templates select="@type | *" mode="actionlength"/>
                                            </xsl:variable>
                                            <xsl:value-of select="concat(rvc:constant($typenamer),&lengthSuffix;)"/>
                                        </xsl:attribute>
                                    </Expr> 
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:variable>
                    
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
                                <xsl:with-param name="length">
                                    <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                </xsl:with-param>
                            </xsl:call-template>
                        </xsl:with-param>
                        <xsl:with-param name="outputs">
                            <xsl:if test="@rvc:port">
                                <xsl:choose>
                                    <xsl:when test="@type='vlc'">
                                        <xsl:call-template name="outputvlc">
                                            <xsl:with-param name="port" select="@rvc:port"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="output">
                                            <xsl:with-param name="port" select="@rvc:port"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:if>
                        </xsl:with-param>
                        <xsl:with-param name="do">
                            <xsl:choose>
                                <xsl:when test="not(@type='vlc')">
                                    <xsl:variable name="test">
                                        <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="iftext"/>
                                        <xsl:text>false</xsl:text>
                                    </xsl:variable>
                                    <xsl:if test="@bs0:variable or $test or @rvc:port">
                                        <Stmt kind="Call">
                                            <Expr kind="Var" name="bool2int"/>
                                            <Args>
                                                <Expr kind="Var" name="b"/>
                                                <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                            </Args>
                                        </Stmt>
                                    </xsl:if>
                                    <xsl:if test="@bs0:variable">
                                        <Stmt kind="Assign">
                                            <xsl:attribute name="name" select="@name"/>
                                            <Expr kind="Var" name="output"/>
                                        </Stmt>  
                                    </xsl:if>
                                    <Stmt kind="Assign" name="&bitNumber;">
                                        <Expr kind="BinOpSeq">
                                            <Expr kind="Var" name="&bitNumber;"/>
                                            <Op name="+"/> 
                                            <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                        </Expr>  
                                    </Stmt>  
                                    <xsl:if test="xsd:annotation/xsd:appinfo/bs2x:variable">
                                        <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="actionexpr">
                                            <xsl:with-param name="typename" select="$typename" tunnel="yes" />
                                        </xsl:apply-templates>
                                    </xsl:if>
                                </xsl:when>
                                <xsl:otherwise>
                                    <Stmt kind="Assign" name="&bitNumber;">
                                        <Expr kind="BinOpSeq">
                                            <Expr kind="Var" name="&bitNumber;"/>
                                            <Op name="+"/>  
                                            <Expr kind="Literal" literal-kind="Integer" value="1"/>
                                        </Expr>  
                                    </Stmt>  
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:with-param>
                        <xsl:with-param name="guard">
                            <xsl:if test="@fixed">
                                <xsl:call-template name="guards">
                                    <xsl:with-param name="guard">
                                        
                                        <Expr kind="BinOpSeq">
                                            <xsl:call-template name="hex2bit">
                                                <xsl:with-param name="value" select="@fixed"/>
                                                <xsl:with-param name="smalllength">
                                                    <xsl:apply-templates select="." mode="followingsize"/>
                                                </xsl:with-param>
                                            </xsl:call-template> 
                                        </Expr>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:if>
                            <xsl:variable name="startCode">
                                <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="startCode"/>
                            </xsl:variable>
                            <xsl:if test="string-length($startCode) &gt; 0">
                                <xsl:call-template name="guards">
                                    <xsl:with-param name="guard">
                                        
                                        <Expr kind="BinOpSeq">
                                            <xsl:call-template name="bit2bit">
                                                <xsl:with-param name="value" select="$startCode"/>
                                            </xsl:call-template> 
                                        </Expr>
                                    </xsl:with-param>
                                </xsl:call-template>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        
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
                <xsl:call-template name="guards">
                    <xsl:with-param name="guard">
                        <xsl:call-template name="vlcguard">
                            <xsl:with-param name="value">0</xsl:with-param>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
        </xsl:call-template>
        
        <xsl:variable name="test">
            <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="iftext"/>
            <xsl:text>false</xsl:text>
        </xsl:variable>
        
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
                <xsl:if test="@bs0:variable or $test">
                    <xsl:call-template name="input">
                        <xsl:with-param name="name">data</xsl:with-param>
                        <xsl:with-param name="port" select="concat(@rvc:port,'&vlcDataPortSuffix;')"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:call-template name="guards">
                    <xsl:with-param name="guard">
                        <xsl:call-template name="vlcguard">
                            <xsl:with-param name="value">1</xsl:with-param>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="do">
                <xsl:if test="xsd:annotation/xsd:appinfo/bs2x:variable">
                    <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="actionexpr">
                        <xsl:with-param name="isvlc" tunnel="yes">true</xsl:with-param>
                    </xsl:apply-templates>
                </xsl:if>
                <xsl:if test="@bs0:variable">
                    <Stmt kind="Assign"> 
                        <xsl:attribute name="name" select="@name"/>
                        <Expr kind="Var" name="data"/>
                    </Stmt>
                </xsl:if>
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
                        <xsl:text>&testActionSuffix;_</xsl:text>
                        <xsl:number count ="xsd:element[@bs2:nOccurs] | xsd:group[@bs2:nOccurs]"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:call-template name="guards">
                    <xsl:with-param name="guard">
                        <Expr kind="BinOpSeq">
                            <Expr kind="Var"> 
                                <xsl:attribute name="name">
                                    <xsl:value-of select="@name"/>
                                    <xsl:text>&countSuffix;</xsl:text>
                                </xsl:attribute>
                            </Expr>
                            <Op name="&lt;"/>
                            <Expr kind="Literal" literal-kind="Integer">
                                <xsl:attribute name="value" select="@bs2:nOccurs"/>
                            </Expr>
                        </Expr>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="do">
                
                <Stmt kind="Assign">
                    <xsl:attribute name="name">
                        <xsl:value-of select="@name"/>
                        <xsl:text>&countSuffix;</xsl:text>
                    </xsl:attribute>
                    
                    <Expr kind="BinOpSeq">
                        
                        <Expr kind="Var">
                            <xsl:attribute name="name">
                                <xsl:value-of select="@name"/>
                                <xsl:text>&countSuffix;</xsl:text>
                            </xsl:attribute>
                        </Expr>
                        
                        <Op name="+"/>
                        <Expr kind="Literal" literal-kind="Integer" value="1"/>
                        
                    </Expr>
                    
                </Stmt>
                
            </xsl:with-param>
            
        </xsl:call-template>
        
        <xsl:variable name="names">
            <xsl:value-of select="@name"/>
            <xsl:text>&skipActionSuffix;</xsl:text>
        </xsl:variable>
        <xsl:if test="unique:iffirst($names)">
            <xsl:call-template name="action">
                <xsl:with-param name="name">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:with-param>
                        <xsl:with-param name="suffix">
                            <xsl:text>&skipActionSuffix;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
                
                <xsl:with-param name="do">
                    
                    <Stmt kind="Assign">
                        <xsl:attribute name="name">
                            <xsl:value-of select="@name"/>
                            <xsl:text>&countSuffix;</xsl:text>
                        </xsl:attribute>
                        <Expr kind="Literal" literal-kind="Integer" value="0"/>
                    </Stmt>
                    
                </xsl:with-param>
                
            </xsl:call-template>
        </xsl:if>
        
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
                        <xsl:number count ="xsd:element[@bs2:if] | xsd:group[@bs2:if]"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="guard">
                <xsl:call-template name="guards">
                    <xsl:with-param name="guard">
                        <xsl:copy-of select="cal:parseExpression(@bs2:if)" copy-namespaces="no"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="*[@bs2:ifNext]" priority="15" mode="actions">
        
        <xsl:variable name="typename">
            <Expr kind="Var">
                <xsl:attribute name="name">
                    <xsl:apply-templates select="." mode="followinglength"/>
                </xsl:attribute>
            </Expr> 
        </xsl:variable>
        
        <xsl:variable name="output">
            <xsl:apply-templates select="." mode="followingoutput"/>
        </xsl:variable>
        
        <xsl:call-template name="action">
            <xsl:with-param name="name">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&validNextActionSuffix;</xsl:text>
                        <xsl:number count ="xsd:element[@bs2:ifNext] | xsd:group[@bs2:ifNext]"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="inputs">
                <xsl:call-template name="input">
                    <xsl:with-param name="length">
                        <xsl:copy-of select="$typename" copy-namespaces="no"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="outputs">
                <xsl:if test="$output">
                    <xsl:call-template name="output">
                        <xsl:with-param name="port" select="$output"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:with-param>
            
            <xsl:with-param name="guard">
                <xsl:call-template name="guards">
                    <xsl:with-param name="guard">
                        <Expr kind="BinOpSeq">
                            
                            <xsl:call-template name="hex2bit">
                                <xsl:with-param name="value" select="@bs2:ifNext"/>
                                <xsl:with-param name="smalllength">
                                    <xsl:apply-templates select="." mode="followingsize"/>
                                </xsl:with-param>
                            </xsl:call-template>
                            
                        </Expr>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            
            <xsl:with-param name="do">
                <xsl:choose>
                    <xsl:when test="not(@type='vlc')">
                        <xsl:if test="$output">
                            <Stmt kind="Call">
                                <Expr kind="Var" name="bool2int"/>
                                <Args>
                                    <Expr kind="Var" name="b"/>
                                    <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                </Args>
                            </Stmt>
                        </xsl:if>
                        <Stmt kind="Assign" name="&bitNumber;">
                            <Expr kind="BinOpSeq">
                                <Expr kind="Var" name="&bitNumber;"/>
                                <Op name="+"/> 
                                <xsl:copy-of select="$typename" copy-namespaces="no"/>
                            </Expr>  
                        </Stmt> 
                        <xsl:if test="xsd:annotation/xsd:appinfo/bs2x:variable">
                            <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="actionexpr">
                                <xsl:with-param name="typename" select="$typename" tunnel="yes" />
                            </xsl:apply-templates>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <Stmt kind="Assign" name="&bitNumber;">
                            <Expr kind="BinOpSeq">
                                <Expr kind="Var" name="&bitNumber;"/>
                                <Op name="+"/>  
                                <Expr kind="Literal" literal-kind="Integer" value="1"/>
                            </Expr>  
                        </Stmt>  
                    </xsl:otherwise>
                </xsl:choose>
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
    
    <xsl:template match="xsd:element" mode="actionsunion">
        <xsl:param name="union" required="yes"/>
        
        <xsl:if test="not(@type= 'bs1:align8' or @type= 'bs1:align16' or @type= 'bs1:align32')">
            
            <xsl:if test="$union &gt; 1">
                <xsl:apply-templates select="." mode="actionsunion">
                    <xsl:with-param name="union" select="$union - 1"/>
                </xsl:apply-templates>
            </xsl:if>
            
            <xsl:variable name="typename">
                <xsl:variable name="typenamer">
                    <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="uniontype">
                        <xsl:with-param name="union" select="$union"/>
                    </xsl:apply-templates>
                </xsl:variable>
                <Expr kind="Var">
                    <xsl:attribute name="name" select="concat(rvc:constant($typenamer),&lengthSuffix;)"/>
                </Expr> 
            </xsl:variable>
            
            <xsl:variable name="guardExpr">
                <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="unioncond">
                    <xsl:with-param name="union" select="$union"/>
                </xsl:apply-templates>
            </xsl:variable>
            
            <xsl:call-template name="action">
                <xsl:with-param name="name">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:with-param>
                        <xsl:with-param name="suffix">
                            <xsl:text>&readActionSuffix;</xsl:text>
                            <xsl:value-of select="$union"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
                <xsl:with-param name="inputs">
                    <xsl:call-template name="input">
                        <xsl:with-param name="length">
                            <xsl:copy-of select="$typename" copy-namespaces="no"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
                <xsl:with-param name="outputs">
                    <xsl:if test="@rvc:port">
                        <xsl:choose>
                            <xsl:when test="@type='vlc'">
                                <xsl:call-template name="outputvlc">
                                    <xsl:with-param name="port" select="@rvc:port"/>
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:call-template name="output">
                                    <xsl:with-param name="port" select="@rvc:port"/>
                                </xsl:call-template>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:if>
                </xsl:with-param>
                <xsl:with-param name="do">
                    
                    <xsl:choose>
                        <xsl:when test="not(@type='vlc')">
                            <xsl:variable name="test">
                                <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="iftext"/>
                                <xsl:text>false</xsl:text>
                            </xsl:variable>
                            
                            <xsl:if test="@bs0:variable or $test or @rvc:port">
                                <Stmt kind="Call">
                                    <Expr kind="Var" name="bool2int"/>
                                    <Args>
                                        <Expr kind="Var" name="b"/>
                                        <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                    </Args>
                                </Stmt>
                            </xsl:if>
                            <xsl:if test="@bs0:variable">
                                <Stmt kind="Assign">
                                    <xsl:attribute name="name" select="@name"/>
                                    <Expr kind="Var" name="output"/>
                                </Stmt>  
                            </xsl:if>
                            <Stmt kind="Assign" name="&bitNumber;">
                                <Expr kind="BinOpSeq">
                                    <Expr kind="Var" name="&bitNumber;"/>
                                    <Op name="+"/> 
                                    <xsl:copy-of select="$typename" copy-namespaces="no"/>
                                </Expr>  
                            </Stmt>  
                            <xsl:if test="xsd:annotation/xsd:appinfo/bs2x:variable">
                                <xsl:apply-templates select="xsd:annotation/xsd:appinfo/bs2x:variable" mode="actionexpr"/>
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <Stmt kind="Assign" name="&bitNumber;">
                                <Expr kind="BinOpSeq">
                                    <Expr kind="Var" name="&bitNumber;"/>
                                    <Op name="+"/>  
                                    <Expr kind="Literal" literal-kind="Integer" value="1"/>
                                </Expr>  
                            </Stmt>  
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
                <xsl:with-param name="guard">
                    <xsl:if test="not($guardExpr = 'true()')">
                        <xsl:call-template name="guards">
                            <xsl:with-param name="guard"> 
                                <xsl:copy-of select="cal:parseExpression($guardExpr)"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:with-param> 
            </xsl:call-template>
        </xsl:if>
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template name="gettype">
        <xsl:param name="type" required="yes" as="xsd:string"/>
        <xsl:param name="union" required="yes" as="xsd:integer"/>
        <xsl:choose>
            <xsl:when test="not( contains($type,' '))">
                <xsl:value-of select="$type"/>
            </xsl:when>
            <xsl:when test="$union=0">
                <xsl:value-of select="substring-before($type,' ')"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="gettype">
                    <xsl:with-param name="type" select="substring-after($type,' ')"/>
                    <xsl:with-param name="union" select="$union - 1"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="xsd:simpleType" mode="unioncount">
        <xsl:value-of select="count(xsd:union/xsd:annotation/xsd:appinfo/bs2:ifUnion)"/>
    </xsl:template>
    
    <xsl:template match="*" mode="unioncount" priority="-1000"/>
    
    <xsl:template match="xsd:simpleType" mode="uniontype">
        <xsl:param name="union" required="yes" as="xsd:integer"/>
        <xsl:call-template name="gettype">
            <xsl:with-param name="type">
                <xsl:value-of select="xsd:union/@memberTypes"/>
            </xsl:with-param>
            <xsl:with-param name="union" select="$union - 1"/>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template match="*" mode="uniontype" priority="-1000"/>
    
    <xsl:template match="xsd:simpleType" mode="unioncond">
        <xsl:param name="union" required="yes" as="xsd:integer"/>
        <xsl:value-of select="xsd:union/xsd:annotation/xsd:appinfo/bs2:ifUnion[$union]/@value"/>
    </xsl:template>
    
    <xsl:template match="*" mode="unioncond" priority="-1000"/>
    
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
    
    <xsl:template match="xsd:element[@type]" mode="followinglength" priority="10">
        <xsl:if test="not(@type='vlc')">
            <xsl:variable name="typenamer">
                <xsl:apply-templates select="@type | *" mode="actionlength"/>
            </xsl:variable>
            <xsl:value-of select="concat(rvc:constant($typenamer),&lengthSuffix;)"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="*" mode="followinglength" priority="-1000"/>
    
    <xsl:template match="xsd:group" mode="followingsize"  priority="5">
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:apply-templates select="$group/*[1]" mode="#current"/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="followingsize"  priority="5">
        
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
    
    <xsl:template match="xsd:element[@ref]" mode="followingsize" priority="5">
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>
        
        <xsl:apply-templates select="$eltType" mode="#current"/>
        
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:all | xsd:complexType | xsd:element[child::element()]" mode="followingsize" priority="2">
        <xsl:apply-templates mode="#current" select="*[1]"/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="followingsize" priority="3">
        <xsl:if test="not(@type='vlc')">
            <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="calculateLength"/>
        </xsl:if>
    </xsl:template>
    
    <!--  <xsl:template match="xsd:element" mode="followingsize" priority="0">
        <xsl:if test="not(@type='vlc')">
        <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="calculateLength"/>
        </xsl:if>
        </xsl:template>-->
    
    <xsl:template match="*" mode="followingsize" priority="-1000"/>
    
    <xsl:template match="xsd:group" mode="followingoutput"  priority="5">
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:apply-templates select="$group/*[1]" mode="#current"/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="followingoutput"  priority="5">
        
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
    
    <xsl:template match="xsd:element[@ref]" mode="followingoutput" priority="5">
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>
        
        <xsl:apply-templates select="$eltType" mode="#current"/>
        
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:all | xsd:complexType | xsd:element[child::element()]" mode="followingoutput" priority="2">
        <xsl:apply-templates mode="#current" select="*[1]"/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@rvc:port]" mode="followingoutput" priority="10">
        <xsl:value-of select="@rvc:port" />
    </xsl:template>
    
    <xsl:template match="*" mode="followingoutput" priority="-1000"/>
    
    <xsl:template match="bs2x:variable" mode="actionexpr">
        <Stmt kind="Assign"> 
            <xsl:attribute name="name" select="@name"/>
            
            <xsl:variable name="modval">
                <xsl:call-template name="modDotText">
                    <xsl:with-param name="value" select="@value" />
                </xsl:call-template>
            </xsl:variable>
            
            <xsl:copy-of select="cal:parseExpression($modval)"/>
        </Stmt>
    </xsl:template>
    
    <xsl:template match="bs2x:variable" mode="iftext">
        <xsl:choose>
            <xsl:when test="contains(@value,'./text()')">true or </xsl:when>
            <xsl:otherwise>false or </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
</xsl:stylesheet>
