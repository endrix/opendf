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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
    xmlns:rvc="urn:mpeg:2006:01-RVC-NS" 
    version="2.0">
  <xsl:include href="functions.xslt"/>
  
    <xsl:template match="/" mode="#all">
        <xsl:variable name="targetNamespace" select="/xsd:schema/@targetNamespace"/>
        <xsl:apply-templates mode="#current">
            <xsl:with-param name="prefix" select="rvc:findPrefix($targetNamespace,.,.)" tunnel="yes"/>
            <xsl:with-param name="namespace" select="$targetNamespace" tunnel="yes"/>            
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:include" mode="#all">
        <xsl:variable name="targetNamespace" select="/xsd:schema/@targetNamespace"/>
        <xsl:variable name="includedDoc" select="document(@schemaLocation)/xsd:schema/*"/>
        <xsl:apply-templates select="$includedDoc" mode="#current">
            <xsl:with-param name="prefix" select="rvc:findPrefix($targetNamespace,.,$includedDoc)" tunnel="yes"/>
            <xsl:with-param name="namespace" select="$targetNamespace" tunnel="yes"/>            
        </xsl:apply-templates>
    </xsl:template>
    
     
    <xsl:template match="xsd:import" mode="#all">
        <xsl:variable name="importedNamespace" select="@namespace"/>
        <xsl:variable name="importedDoc" select="document(@schemaLocation)/xsd:schema/*"/>
        <xsl:apply-templates select="$importedDoc" mode="import">
            <xsl:with-param name="prefix" select="rvc:findPrefix($importedNamespace,.,$importedDoc)" tunnel="yes"/>
            <xsl:with-param name="namespace" select="$importedNamespace" tunnel="yes"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="xsd:schema/*/@name" mode="#all">
        <xsl:param name="prefix" required="yes" tunnel="yes"/>
        <xsl:param name="namespace" required="yes" tunnel="yes"/>
        <xsl:namespace name="{$prefix}" select="$namespace"/>
        <xsl:attribute name="name" select="concat($prefix,':',.)"/>
    </xsl:template>
    

        <xsl:template match="*|@*" mode="#all">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
         </xsl:copy>
    </xsl:template>

</xsl:stylesheet>