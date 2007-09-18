<!--
	SetActorParameters.xslt
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
  xmlns:math="http://exslt.org/math"
  version="1.1">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Set Actor parameters.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddDirectives.</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: SetActorParameters.xslt 2451 2007-06-28 19:23:34Z imiller $</xd:cvsId>
  </xd:doc>

  <xd:doc>
    <xd:short>Set the value of an Actor parameter.</xd:short>
    <xd:detail>The value is determined by looking for (in this order):
    <ol>
      <li> the value assigned by a Directive, else</li>
      <li> the value of the parent Actor vertex attribute of the same name, else</li>
      <li> a default value expression</li>
    </ol>
    </xd:detail>
  </xd:doc>
  <xsl:template match="Decl[ @kind='Parameter' ]">

    <xsl:variable name="this" select="@name"/>

    <!-- Find the applicable Expr in the source document -->    
    <xsl:variable name="expr">
      <xsl:variable name="dir-attr" select="../Note[ @kind='Directive' and @name=$this ]/Expr"/>
      <xsl:variable name="graph-attr" select="../../../Note[ @kind='parameter' and @name=$this ]/Expr"/>
      <xsl:choose>
        <xsl:when test="parent::Actor and $dir-attr">
          <!-- directive -->
          <xsl:copy-of select="$dir-attr"/>
        </xsl:when>
        <xsl:when test="parent::Actor and $graph-attr">
          <!-- graph attribute -->
          <xsl:copy-of select="$graph-attr"/>
        </xsl:when>
        <xsl:otherwise>
          <!-- default value -->
          <xsl:copy-of select="Expr"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:copy-of select="*[ not( Expr ) ]"/> 
      <xsl:copy-of select="$expr"/>
    </xsl:copy>
      
  </xsl:template>
 
  <xd:doc>
    Copy unmodified elemnt.
  </xd:doc>
  <xsl:template match="*">

    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>

      <xsl:apply-templates/>
    </xsl:copy>
    
  </xsl:template>

</xsl:stylesheet>