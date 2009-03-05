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
<xsl:stylesheet  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
     xmlns:saxon="http://saxon.sf.net/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:bs2x="urn:mpeg:mpeg21:2003:01-DIA-BSDL2x-NS"
    xmlns:math="http://exslt.org/math"
    xmlns:rvc="urn:mpeg:2006:01-RVC-NS"
    xmlns:bs0="urn:mpeg:mpeg21:2003:01-DIA-BSDL0-NS" 
    xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS" 
    version="2.0">
    
    <xsl:include href="removeRedundantSequences.xslt"/>
    <xsl:include href="inlineComplexExtRestr.xslt"/>
    <xsl:include href="addNames.xslt"/>
    <!--<xsl:include href="includeImport.xslt"/>-->
    <xsl:variable name="compositeDocument"
        select="saxon:transform(
        saxon:compile-stylesheet(doc('includeImport.xslt')),
        /.)"/>
    
    
    <xsl:template match="/">
        <xsl:apply-templates select="$compositeDocument/*"/>
    </xsl:template>
    
    <xsl:template match="xsd:sequence[@bs2:nOccurs] | xsd:sequence[@bs2:if] | xsd:sequence[@bs2:ifNext]" mode="#all" priority="10">
        <xsd:group>
            <xsl:attribute name="ref">sequence<xsl:number count ="xsd:sequence[@bs2:nOccurs] | xsd:sequence[@bs2:if] | xsd:sequence[@bs2:ifNext]" level="any"/></xsl:attribute>
            <xsl:attribute name="name">sequence<xsl:number count ="xsd:sequence[@bs2:nOccurs] | xsd:sequence[@bs2:if] | xsd:sequence[@bs2:ifNext]" level="any"/></xsl:attribute>
            <xsl:apply-templates select="@*" mode="#current"/> 
        </xsd:group>
    </xsl:template>
    
    <xsl:template match="xsd:group[@ref]" mode="#all" priority="10">
        <xsd:group>
            <xsl:attribute name="name" select="@ref"/>
            <xsl:apply-templates select="@*" mode="#current"/> 
        </xsd:group>
    </xsl:template>
    
    <xsl:template match="xsd:sequence[@bs2:nOccurs] | xsd:sequence[@bs2:if] | xsd:sequence[@bs2:ifNext]" mode="groupize" priority="20">
        <xsd:group>
            <xsl:attribute name="name">sequence<xsl:number count ="xsd:sequence[@bs2:nOccurs] | xsd:sequence[@bs2:if] | xsd:sequence[@bs2:ifNext]" level="any"/></xsl:attribute>
            <xsl:apply-templates select="*" mode="group2"/> 
        </xsd:group>
        <xsl:apply-templates select="*" mode="groupize"/> 
    </xsl:template>
    
    <xsl:template match="*|@*" mode="groupize" priority="10">
         <xsl:apply-templates select="@* | node()" mode="#current"/>
    </xsl:template>

    <xsl:template match="xsd:schema" mode="#all" priority="10">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="#current"/>
            <xsl:apply-templates select="node()" mode="groupize"/>
            <xsl:apply-templates select="node()" mode="#current"/>           
        </xsl:copy>
    </xsl:template>

    <xsl:template match="*|@*" mode="#all">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" mode="#current"/>            
        </xsl:copy>
    </xsl:template>
    
    <!-- strip any text -->
    <xsl:template match="text()" mode="#all">
    </xsl:template>
</xsl:stylesheet><!-- Stylus Studio meta-information - (c) 2004-2007. Progress Software Corporation. All rights reserved.
<metaInformation>
<scenarios ><scenario default="yes" name="Scenario1" userelativepaths="yes" externalpreview="no" url="temp.xml" htmlbaseurl="" outputurl="" processortype="saxon8" useresolver="yes" profilemode="0" profiledepth="" profilelength="" urlprofilexml="" commandline="" additionalpath="" additionalclasspath="" postprocessortype="none" postprocesscommandline="" postprocessadditionalpath="" postprocessgeneratedext="" validateoutput="no" validator="internal" customvalidator="" ><advancedProp name="sInitialMode" value=""/><advancedProp name="bSchemaAware" value="true"/><advancedProp name="bXsltOneIsOkay" value="true"/><advancedProp name="bXml11" value="false"/><advancedProp name="iValidation" value="0"/><advancedProp name="bExtensions" value="true"/><advancedProp name="iWhitespace" value="0"/><advancedProp name="sInitialTemplate" value=""/><advancedProp name="bTinyTree" value="true"/><advancedProp name="bUseDTD" value="false"/><advancedProp name="bWarnings" value="true"/><advancedProp name="iErrorHandling" value="fatal"/></scenario></scenarios><MapperMetaTag><MapperInfo srcSchemaPathIsRelative="yes" srcSchemaInterpretAsXML="no" destSchemaPath="" destSchemaRoot="" destSchemaPathIsRelative="yes" destSchemaInterpretAsXML="no" ><SourceSchema srcSchemaPath="temp.xml" srcSchemaRoot="schema" AssociatedInstance="" loaderFunction="document" loaderFunctionUsesURI="no"/></MapperInfo><MapperBlockPosition><template match="/"></template><template match="*|@*"></template></MapperBlockPosition><TemplateContext></TemplateContext><MapperFilter side="source"></MapperFilter></MapperMetaTag>
</metaInformation>
-->