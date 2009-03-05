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
<xsl:stylesheet
     xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:rvc="urn:mpeg:2006:01-RVC-NS"
    version="2.0">


    <xsl:template match="xsd:sequence | xsd:choice | xsd:all" mode="#all">
        <xsl:copy>
            <xsl:attribute name="name">
                <xsl:value-of select="local-name(.)"/>
                <xsl:number select="."/>
            </xsl:attribute>
            <xsl:apply-templates select="@* | node()" mode="#current"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@ref">
        <xsl:copy/>
        <xsl:attribute name="name" select="."/>
    </xsl:template>
    
<!--anonymous types -->
    <xsl:template match="xsd:simpleType[not(@name)] | xsd:complexType[not(@name)]" mode="#all">
        <xsl:variable name="siblingCount" select="count(../*[ends-with(name(),'Type')])"/>
        <xsl:copy>
            <xsl:attribute name="name" >
                <xsl:value-of select="rvc:namedAncestor(..)"/>
                <xsl:text>Type</xsl:text>
                <xsl:if test="$siblingCount>1">
                    <xsl:number select="."/>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates select="@* | node()" mode="#current"/>
       </xsl:copy>
    </xsl:template>    
    
    <!-- recurse up the tree until you find a named element -->
    <xsl:function name="rvc:namedAncestor" as="xsd:string">
        <xsl:param name="typeElement" as="element()"/>
        <xsl:choose>
            <xsl:when test="$typeElement/@name">
                <xsl:value-of select="$typeElement/@name"/>
            </xsl:when>
            <xsl:when test="$typeElement/..">
                <xsl:value-of select="rvc:namedAncestor($typeElement/..)"/>
            </xsl:when>
        </xsl:choose>            
    </xsl:function>
    
    <!--<xsl:template match="*|@*" mode="#all">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>-->
</xsl:stylesheet>