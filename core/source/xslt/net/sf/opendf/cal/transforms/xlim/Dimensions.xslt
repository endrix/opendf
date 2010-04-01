<!--
	Dimensions.xslt
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT

-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  version="1.1">
  <xsl:output method="xml"/>

  <xsl:key name="find-decl" match="Decl" use="@id"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Annotate all indexer Stmts/Exprs with size of each dimension</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on EvaluateConstantExpressions</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: Dimensions.xslt 1088 2005-12-13 18:04:52Z davep $</xd:cvsId>
  </xd:doc>
   
  <xd:doc>
    Annotate list indexer with size of dimensions.
  </xd:doc>
  <xsl:template match="Expr[@kind='Indexer']">

    <xsl:variable name="decl-id" select="Expr/Note[@kind='varRef']/@decl-id"/>
        
    <xsl:copy>
      <!-- Preserve the existing element information -->  
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
     
      <xsl:apply-templates/>
      
      <xsl:apply-templates select="Expr[@kind='Var']/Note[@kind='exprType']/Type" mode="dimensions">
        <xsl:with-param name="index">1</xsl:with-param>
      </xsl:apply-templates>
                
    </xsl:copy>
    
  </xsl:template>

  <xd:doc>
    Annotate list assignment with dimension sizes.
  </xd:doc>
  <xsl:template match="Stmt[@kind='Assign'][ Args ]">

    <!-- <xsl:variable name="decl-id" select="Note[@kind='varMod']/@decl-id"/>  -->
    
    <xsl:variable name="decl-id">        
        <xsl:choose>
          <xsl:when test="Note[@kind='varMod']">
            <xsl:value-of select="Note[@kind='varMod']/@decl-id"/>    
          </xsl:when>
          <xsl:otherwise>
             <!-- Element-wise writes to local lists are treated as read (or better, side effects) -->  
             <xsl:value-of select="Note[@kind='varRef']/@decl-id"/>       
          </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>    
        
    <xsl:copy>
      <!-- Preserve the existing element information -->  
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
      
      
<!--   <xsl:apply-templates select="key('find-decl',$decl-id)/Expr/Note[@kind='exprType']/Type" mode="dimensions"> -->
      <xsl:apply-templates select="//Decl[@id=$decl-id]/Type" mode="dimensions"> 
      
      <xsl:with-param name="index">1</xsl:with-param>
      </xsl:apply-templates>
      
    </xsl:copy>
    
  </xsl:template>
    
  <xd:doc> Default just copies the input element to the result tree </xd:doc>
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>   
    </xsl:copy>
  </xsl:template>

  <xsl:template match="Type[ Entry[@kind='Type'] ]" mode="dimensions">
    <xsl:param name="index"/>

    <xsl:variable name="inner-dims">
      <xsl:apply-templates select="Entry[@kind='Type']/Type" mode="dimensions">
        <xsl:with-param name="index" select="$index + 1"/>
      </xsl:apply-templates>
    </xsl:variable>

    <Note kind="dimension" index="{$index}" size="{Entry[@kind='Expr'][@name='size']/Expr/@value}">
      <xsl:attribute name="multiplier">
        <xsl:choose>
          <xsl:when test="$inner-dims/Note"> 
            <xsl:value-of select="$inner-dims/Note[1]/@size * $inner-dims/Note[1]/@multiplier"/>
          </xsl:when>
          <xsl:otherwise>1</xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
    </Note>
    
    <xsl:copy-of select="$inner-dims/Note"/>
      
  </xsl:template>

  <xsl:template match="*" mode="dimensions"/>
          
</xsl:stylesheet>
