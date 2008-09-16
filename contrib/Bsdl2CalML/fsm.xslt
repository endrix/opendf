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
    
    xmlns:math="http://exslt.org/math"
    
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    
    xmlns:bs0="urn:mpeg:mpeg21:2003:01-DIA-BSDL0-NS" 
    
    xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS" 
    
    xmlns:rvc="urn:mpeg:2006:01-RVC-NS"
    
    version="2.0">
    
    <xsl:template match="xsd:element | xsd:group | xsd:sequence |  xsd:all" priority="5" mode="fsm">
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName(.)"/>
                <xsl:text>&existsStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:apply-templates select="*[1]" mode="nextname">
                    <xsl:with-param name="stack" select="."  tunnel="yes"/>
                </xsl:apply-templates>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:text>&skipAction;</xsl:text>
                    </xsl:with-param>
               </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:apply-templates mode="fsmoutput" select="*">
            <xsl:with-param name="next" tunnel="yes">exit</xsl:with-param>
            <xsl:with-param name="stack" select="." tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element | xsd:group | xsd:sequence |  xsd:all" priority="60" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        <xsl:next-match>
            <xsl:with-param name="stack" select="$newStack" tunnel="yes"/> 
            <xsl:with-param name="stacko" select="$stack" tunnel="yes"/> 
            <xsl:with-param name="nextc" tunnel="yes">
                <xsl:apply-templates select="." mode="fsmnext">
                <xsl:with-param name="stack" select="$newStack" tunnel="yes"/> 
                <xsl:with-param name="stacko" select="$stack" tunnel="yes"/> 
                </xsl:apply-templates>
            </xsl:with-param> 
        </xsl:next-match>
    </xsl:template>
    
    <xsl:template match="xsd:element" priority="50" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
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
                <xsl:apply-templates select="." mode="fsmunion">
                    <xsl:with-param name="union" select="$union"/>
                </xsl:apply-templates>
            </xsl:when>
            <xsl:otherwise>
       
                <xsl:call-template name="transition">
                    <xsl:with-param name="from">
                        <xsl:value-of select="rvc:itemName($stack)"/>
                        <xsl:text>&existsStateSuffix;</xsl:text>
                    </xsl:with-param>
                    <xsl:with-param name="to">
                        <xsl:choose>
                            <xsl:when test="@type='vlc'">
                                <xsl:value-of select="rvc:itemName($stack)"/>
                                <xsl:text>&resultStateSuffix;</xsl:text>
                            </xsl:when>
                            <xsl:when test="@bs2:nOccurs">
                                <xsl:value-of select="rvc:itemName($stack)"/>
                                <xsl:text>&nOccStateSuffix;</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:apply-templates select="." mode="fsmnext">
                                    <xsl:with-param name="stack" select="$stacko" tunnel="yes"/> 
                                </xsl:apply-templates>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                    <xsl:with-param name="action">
                        <xsl:call-template name="qid">
                            <xsl:with-param name="name">
                                <xsl:value-of select="@name"/>
                            </xsl:with-param>
                            <xsl:with-param name="suffix">
                                <xsl:text>&readActionSuffix;</xsl:text>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="xsd:element[@type='vlc']" mode="fsmoutput" priority="30">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&resultStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&existsStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&notFinishedActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>

        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&resultStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:choose>
                    <xsl:when test="@bs2:nOccurs">
                        <xsl:value-of select="rvc:itemName($stack)"/>
                        <xsl:text>&nOccStateSuffix;</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="." mode="fsmnext">
                            <xsl:with-param name="stack" select="$stacko" tunnel="yes"/> 
                        </xsl:apply-templates>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&finishActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="xsd:element[@bs2:nOccurs]" priority="20" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&nOccStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&existsStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="action">
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
        </xsl:call-template>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="xsd:group[@bs2:nOccurs]" priority="20" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>

        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&nOccStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:apply-templates select="." mode="nextchild"/>
            </xsl:with-param>
            <xsl:with-param name="action">
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
        </xsl:call-template>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="xsd:element[@bs2:nOccurs] | xsd:group[@bs2:nOccurs]" priority="19" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&nOccStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:apply-templates select="." mode="fsmnext">
                    <xsl:with-param name="stack" select="$stacko" tunnel="yes"/>
                </xsl:apply-templates>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&skipActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match>
            <xsl:with-param name="nextc" tunnel="yes">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&nOccStateSuffix;</xsl:text>
            </xsl:with-param> 
        </xsl:next-match>
        
    </xsl:template>
    
    <xsl:template match="*[@bs2:ifNext]" priority="15" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
        <xsl:if test="not(parent::xsd:choice)">
            
            <xsl:call-template name="transition">
                <xsl:with-param name="from">
                    <xsl:value-of select="rvc:itemName($stack)"/>
                    <xsl:text>&checkStateSuffix;</xsl:text>
                </xsl:with-param>
                <xsl:with-param name="to">
                    <xsl:apply-templates select="." mode="followingchild">
                        <xsl:with-param name="stack" tunnel="yes" select="$stacko"/>
                    </xsl:apply-templates>
                </xsl:with-param>
                <xsl:with-param name="action">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:with-param>
                        <xsl:with-param name="suffix">
                            <xsl:text>&validActionSuffix;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
            
            <xsl:call-template name="transition">
                <xsl:with-param name="from">
                    <xsl:value-of select="rvc:itemName($stack)"/>
                    <xsl:text>&checkStateSuffix;</xsl:text>
                </xsl:with-param>
                <xsl:with-param name="to">
                    <xsl:apply-templates select="." mode="fsmnext">
                        <xsl:with-param name="stack" select="$stacko" tunnel="yes"/>
                    </xsl:apply-templates>
                </xsl:with-param>
                <xsl:with-param name="action">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:text>&skipAction;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="*[@bs2:if]" priority="15" mode="fsmoutput">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
        <xsl:if test="not(parent::xsd:choice)">
           
            <xsl:call-template name="transition">
                <xsl:with-param name="from">
                    <xsl:value-of select="rvc:itemName($stack)"/>
                    <xsl:text>&checkStateSuffix;</xsl:text>
                </xsl:with-param>
                <xsl:with-param name="to">
                    <xsl:value-of select="rvc:itemName($stack)"/>
                    <xsl:choose>  
                        <xsl:when test="not(@bs2:nOccurs)">
                            <xsl:text>&existsStateSuffix;</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>&nOccStateSuffix;</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
                <xsl:with-param name="action">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:with-param>
                        <xsl:with-param name="suffix">
                            <xsl:text>&validActionSuffix;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
            
            <xsl:call-template name="transition">
                <xsl:with-param name="from">
                    <xsl:value-of select="rvc:itemName($stack)"/>
                    <xsl:text>&checkStateSuffix;</xsl:text>
                </xsl:with-param>
                <xsl:with-param name="to">
                    <xsl:apply-templates select="." mode="fsmnext">
                        <xsl:with-param name="stack" select="$stacko"  tunnel="yes"/>
                    </xsl:apply-templates>
                </xsl:with-param>
                <xsl:with-param name="action">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:text>&skipAction;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        
        <xsl:next-match/>
        
    </xsl:template>
    
    <xsl:template match="xsd:group" mode="fsmoutput">
        <xsl:param name="nextc" required="yes" tunnel="yes"/>
        
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:apply-templates select="$group/*" mode="#current">
            <xsl:with-param name="next" select="$nextc"  tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="fsmoutput" priority="2">
        <xsl:param name="nextc" required="yes" tunnel="yes"/>
        <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="#current">
            <xsl:with-param name="next" select="$nextc"  tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element[@ref]" mode="fsmoutput" priority="2">
        <xsl:param name="nextc" required="yes" tunnel="yes"/>
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>
        
        <xsl:apply-templates select="$eltType" mode="#current">
            <xsl:with-param name="next" select="$nextc"  tunnel="yes"/>
        </xsl:apply-templates>
                
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:all | xsd:complexType" mode="fsmoutput">
        <xsl:apply-templates mode="#current">
            <xsl:with-param name="next" tunnel="yes">
                <xsl:apply-templates select="." mode="fsmnext"/>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element[child::element()]" mode="fsmoutput">
        <xsl:param name="nextc" required="yes" tunnel="yes"/>
        <xsl:apply-templates mode="#current">
            <xsl:with-param name="next" select="$nextc"  tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="*" mode="fsmoutput" priority="-1000"/> 
    
    <xsl:template match="xsd:choice" priority="3" mode="fsmoutput">
        <xsl:variable name="nextT">
            <xsl:apply-templates select="." mode="fsmnext"/>
        </xsl:variable>

        <xsl:apply-templates select="*" mode="fsmchoice">
            <xsl:with-param name="start">
                <xsl:apply-templates select="." mode="nextname"/>
            </xsl:with-param>
        </xsl:apply-templates>
        <xsl:apply-templates select="*" mode="fsmoutput">
            <xsl:with-param name="next" select="$nextT"  tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element" mode="fsmunion">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        <xsl:param name="union" required="yes" as="xsd:integer"/>
        
        <xsl:if test="$union &gt; 1">
            <xsl:apply-templates select="." mode="fsmunion">
                <xsl:with-param name="union" select="$union - 1"/>
            </xsl:apply-templates>
        </xsl:if>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="rvc:itemName($stack)"/>
                <xsl:text>&existsStateSuffix;</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:choose>
                    <xsl:when test="@type='vlc'">
                        <xsl:value-of select="rvc:itemName($stack)"/>
                        <xsl:text>&resultStateSuffix;</xsl:text>
                    </xsl:when>
                    <xsl:when test="@bs2:nOccurs">
                        <xsl:value-of select="rvc:itemName($stack)"/>
                        <xsl:text>&nOccStateSuffix;</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="." mode="fsmnext">
                            <xsl:with-param name="stack" select="$stacko" tunnel="yes"/> 
                        </xsl:apply-templates>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
            <xsl:with-param name="action">
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
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template match="*" mode="fsmunion" priority="-1000"/> 
    
    <xsl:template match="*" mode="fsmnext">
        <xsl:param name="next" required="yes" tunnel="yes"/>
        
        <xsl:choose>
            <xsl:when test="parent::xsd:choice">
                <xsl:value-of select="$next"/>
            </xsl:when>
            <xsl:when test="following-sibling::*[1]">
                <xsl:apply-templates select="following-sibling::*[1]" mode="nextname"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$next"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="xsd:group" mode="fsmchoice" priority="10">
        <xsl:param name="start" required="yes"/>
        <xsl:param name="stack" required="yes" tunnel="yes"/>

        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="$start"/>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:apply-templates select="$group/*[1]" mode="nextname">
                    <xsl:with-param name="stack" select="$newStack"  tunnel="yes"/>
                </xsl:apply-templates>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&validActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template match="*" mode="fsmchoice" priority="0">
        <xsl:param name="start" required="yes"/>
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:call-template name="transition">
            <xsl:with-param name="from">
                <xsl:value-of select="$start"/>
            </xsl:with-param>
            <xsl:with-param name="to">
                <xsl:value-of select="rvc:itemName($newStack)"/>
                <xsl:choose>
                    <xsl:when test="not(@bs2:nOccurs)">
                        <xsl:text>&existsStateSuffix;</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>&nOccStateSuffix;</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
            <xsl:with-param name="action">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="@name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&validActionSuffix;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template match="xsd:element | xsd:group | xsd:sequence |  xsd:all" priority="5" mode="firstname">
        <xsl:apply-templates select="." mode="nextname">
            <xsl:with-param name="stack" select="."  tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:group |  xsd:complexType |  xsd:all" priority="5" mode="nextname">
        <xsl:param name="stack" required="yes"  tunnel="yes"/>
        
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:apply-templates mode="#current" select="*[1]">
            <xsl:with-param name="stack" select="$newStack"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element" mode="nextname nextchild" priority="5">
        <xsl:param name="stack" required="yes"  tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:value-of select="rvc:itemName($newStack)"/>
        <xsl:text>&existsStateSuffix;</xsl:text>
    </xsl:template>
    
    <xsl:template match="xsd:choice" mode="nextname nextchild" priority="30">
        <xsl:apply-templates select="*[1]" mode="nextname"/>
        <xsl:text>&chooseStateSuffix;</xsl:text>
    </xsl:template>
    
    <xsl:template match="*[@bs2:if] | *[@bs2:ifNext]" mode="nextname" priority="20">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:value-of select="rvc:itemName($newStack)"/>
        <xsl:if test="not(parent::xsd:choice)">
            <xsl:text>&checkStateSuffix;</xsl:text>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="*[@bs2:nOccurs]" mode="nextname" priority="10">
        <xsl:param name="stack" required="yes"  tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:value-of select="rvc:itemName($newStack)"/>
        <xsl:text>&nOccStateSuffix;</xsl:text>
    </xsl:template>
    
    <xsl:template match="xsd:complexType" mode="nextname nextchild" priority="7">
        <xsl:apply-templates select="*[1]" mode="nextname"/>
    </xsl:template>
    
    <xsl:template match="xsd:group" mode="nextname nextchild" priority="7">
        <xsl:param name="stack" required="yes" tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        <xsl:apply-templates select="$group/*[1]" mode="nextname">
            <xsl:with-param name="stack" select="$newStack" tunnel="yes"/> 
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element[@ref]" mode="nextname nextchild" priority="7">
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>
        <xsl:apply-templates select="$eltType" mode="nextname"/>
    </xsl:template>
    
    <xsl:template match="*" mode="nextname nextchild" priority="-1000"/>
    
    <xsl:template match="xsd:element | xsd:group | xsd:sequence |  xsd:all" priority="10" mode="followingchild">
        <xsl:param name="stack" required="yes"  tunnel="yes"/>
        <xsl:variable name="newStack" select="if ($stack[1] is .) then $stack else (.,$stack)"/>
        
        <xsl:next-match>
            <xsl:with-param name="stack" select="$newStack"  tunnel="yes"/> 
            <xsl:with-param name="stacko" select="$stack"  tunnel="yes"/> 
        </xsl:next-match>
    </xsl:template>
    
    <xsl:template match="xsd:group" mode="followingchild"  priority="5">
        <xsl:variable name="group" select="key('groups',resolve-QName(@ref,.))"/>
        
        <xsl:apply-templates select="$group/*[1]" mode="#current">
            <xsl:with-param name="next"  tunnel="yes">
                <xsl:apply-templates select="." mode="fsmnext"/>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element[@type]" mode="followingchild"  priority="5">
        
        <xsl:variable name="tmp">
            <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="#current">
                <xsl:with-param name="next"  tunnel="yes">
                    <xsl:apply-templates select="." mode="fsmnext"/>
                </xsl:with-param>
            </xsl:apply-templates>
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
    
    <xsl:template match="xsd:element[@ref]" mode="followingchild" priority="5">
        <xsl:variable name="referencedElt" select="key('globalElements',resolve-QName(@ref,.))"/>
        <xsl:variable name="eltType"
            select="if ($referencedElt/@type)
            then key('types',resolve-QName($referencedElt/@type,.))
            else $referencedElt/*[1]"/>

        <xsl:apply-templates select="$eltType" mode="#current">
            <xsl:with-param name="next"  tunnel="yes">
                <xsl:apply-templates select="." mode="fsmnext"/>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:all | xsd:complexType | xsd:element[child::element()]" mode="followingchild" priority="2">
        <xsl:apply-templates mode="#current" select="*[1]">
            <xsl:with-param name="next"  tunnel="yes">
                <xsl:apply-templates select="." mode="fsmnext"/>
            </xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:element" mode="followingchild" priority="0">
        <xsl:param name="stacko" required="yes" tunnel="yes"/>
        
        <xsl:apply-templates select="." mode="fsmnext">
            <xsl:with-param name="stack" select="$stacko"  tunnel="yes"/> 
        </xsl:apply-templates>

    </xsl:template>

</xsl:stylesheet>
