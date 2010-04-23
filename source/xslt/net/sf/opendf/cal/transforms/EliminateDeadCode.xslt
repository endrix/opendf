<!--
    EliminateDeadCode
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


    assumes: EvaluateConstantExpressions

    author: JWJ
    
-->

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  extension-element-prefixes="xsl"
  version="1.1">
    <xsl:output method="xml"/>
    
    
    <xsl:template match="Expr[@kind='If'][Expr[1][@kind='Literal'][@literal-kind='Boolean']]">
        <xsl:choose>
            <xsl:when test="Expr[1]/@value = 'true' or Expr[1]/@value = 1">
                <xsl:copy-of select="Expr[2]"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="Expr[3]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="Stmt[@kind='If'][Expr[@kind='Literal'][@literal-kind='Boolean']]">
        <xsl:choose>
            <xsl:when test="Expr/@value = 'true' or Expr/@value = 1">
                <xsl:copy-of select="Stmt[1]"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- This may not exist, in which case the substitution code is, 
                     appropriately, empty. -->
                <xsl:copy-of select="Stmt[2]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template match="Stmt[@kind='While'][Expr[@kind='Literal'][@literal-kind='Boolean']]">
        <xsl:choose>
            <xsl:when test="Expr/@value = 'true' or Expr/@value = 1">
                <xsl:copy-of select="."/>  <!-- Infinite loop: Houston, we got a problem. -->
            </xsl:when>
            <xsl:otherwise>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>
    
    
</xsl:stylesheet>


