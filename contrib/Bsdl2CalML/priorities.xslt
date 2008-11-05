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
    
    <xsl:template match="*[@bs2:if]" mode="priorities" priority="20">
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
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
            <xsl:with-param name="lesser">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:text>&skipAction;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
             </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="*[@bs2:ifNext]" mode="priorities" priority="20">
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
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
            <xsl:with-param name="lesser">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:text>&skipAction;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="*[@bs2:if]" mode="prioritieschoose" priority="20">
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
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
            <xsl:with-param name="lesser">
                <xsl:choose>
                    <xsl:when test="following-sibling::*[@name]">
                        <xsl:call-template name="qid">
                            <xsl:with-param name="name">
                                <xsl:value-of select="following-sibling::*[1]/@name"/>
                            </xsl:with-param>
                            <xsl:with-param name="suffix">
                                <xsl:text>&validActionSuffix;</xsl:text>
                                <xsl:number count ="xsd:element[@bs2:if] | xsd:group[@bs2:if]"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="qid">
                            <xsl:with-param name="name">
                                <xsl:text>&skipAction;</xsl:text>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="*[@bs2:ifNext]" mode="prioritieschoose" priority="20">
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
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
            <xsl:with-param name="lesser">
                <xsl:choose>
                    <xsl:when test="following-sibling::*[@name]">
                        <xsl:call-template name="qid">
                            <xsl:with-param name="name">
                                <xsl:value-of select="following-sibling::*[1]/@name"/>
                            </xsl:with-param>
                            <xsl:with-param name="suffix">
                                <xsl:text>&validNextActionSuffix;</xsl:text>
                                <xsl:number count ="xsd:element[@bs2:ifNext] | xsd:group[@bs2:ifNext]"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="qid">
                            <xsl:with-param name="name">
                                <xsl:text>&skipAction;</xsl:text>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="*[@bs2:nOccurs]" mode="priorities prioritieschoose" priority="10">
        
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
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
            <xsl:with-param name="lesser">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:text>&skipAction;</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template match="xsd:element" priority="20" mode="priorities">
        
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
        <xsl:if test="$union &gt; 0">
            <xsl:call-template name="unionPriority">
                <xsl:with-param name="union" select="$union"/>
                <xsl:with-param name="name" select="@name"></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        
        <xsl:variable name="startCode">
            <xsl:apply-templates select="key('types',resolve-QName(@type,.))" mode="startCode"/>
        </xsl:variable>
        <xsl:if test="@fixed or (string-length($startCode) &gt; 0)">
            <xsl:call-template name="priority">
                <xsl:with-param name="greater">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:value-of select="@name"/>
                        </xsl:with-param>
                        <xsl:with-param name="suffix">
                            <xsl:text>&readActionSuffix;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
                <xsl:with-param name="lesser">
                    <xsl:call-template name="qid">
                        <xsl:with-param name="name">
                            <xsl:text>&errorAction;</xsl:text>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        
        <xsl:next-match/>
    </xsl:template>
    
    <xsl:template name="unionPriority">
        <xsl:param name="union" as="xsd:integer"/>
        <xsl:param name="name"/>
        <xsl:if test="$union &gt; 2">
            <xsl:call-template name="unionPriority">
                <xsl:with-param name="union" select="$union - 1"/>
                <xsl:with-param name="name" select="$name"/>
            </xsl:call-template>
        </xsl:if>
        
        <xsl:call-template name="priority">
            <xsl:with-param name="greater">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="$name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&readActionSuffix;</xsl:text>
                        <xsl:value-of select="$union - 1"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="lesser">
                <xsl:call-template name="qid">
                    <xsl:with-param name="name">
                        <xsl:value-of select="$name"/>
                    </xsl:with-param>
                    <xsl:with-param name="suffix">
                        <xsl:text>&readActionSuffix;</xsl:text>
                        <xsl:value-of select="$union"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
            
    
    <xsl:template match="xsd:choice" priority="5" mode="priorities">
        <xsl:apply-templates mode="prioritieschoose" select="*"/>
    </xsl:template>
    
    <xsl:template match="xsd:sequence | xsd:group | xsd:complexType |  xsd:all | xsd:element[child::element()]" priority="5" mode="priorities prioritieschoose">
        <xsl:apply-templates mode="priorities" select="*"/>
    </xsl:template>
    
    <xsl:template match="*" mode="priorities" priority="-1000"/>

</xsl:stylesheet>
