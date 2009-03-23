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
  extension-element-prefixes="xsl xd"
  version="1.1">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Set Actor parameters.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AddDirectives.</li>
        <li>Relies on AddID.</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: SetActorParameters.xslt 2451 2007-06-28 19:23:34Z imiller $</xd:cvsId>
  </xd:doc>

  <xd:doc>
    <xd:detail>
      Eliminates Parameter elements in inlined Instances
      Converts Decl[@kind=parameter] elements in Actors to Decl[@kind=variable] based
        on Instance parameter values or directives
    </xd:detail>
  </xd:doc>

  <!--
      Suppress the instance parameter if the source has already been loaded
  -->
  <xsl:template match="Instance/Parameter">
    <xsl:choose>
      
      <xsl:when test="../Note[@kind='sourceLoaded' and @value='true']">
        <!-- Drop the Parameter.  It will be pushed into the Actor instance below -->
      </xsl:when>
      
      <xsl:otherwise>
        <!-- Preserve the existing element information -->  
        <xsl:copy>
          <xsl:for-each select="@*">
            <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
          </xsl:for-each>

          <xsl:apply-templates/>
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

<!--  <xsl:template match="Instance[Note[@kind='sourceLoaded',@value='true']]/Actor/Decl[@kind='Parameter']">-->
<xsl:template match="Instance/Actor/Decl[@kind='Parameter']">

  <xsl:variable name="paramName" select="@name"/>
  <xsl:variable name="actor" select=".."/>
  <xsl:variable name="instance" select="../.."/>

  <!-- only apply to loaded-source actors -->
  <xsl:choose>

    <xsl:when test="$instance/Note[@kind='sourceLoaded' and @value='true']">
      <!-- If an actor variable declaration shadows the parameter value,
           leave the actor parameter declaration alone.  Otherwise,
           convert the actor parameter declaration to an actor variable
           declartion with the content set from the Instance Parameter -->
      
      <xsl:choose>
        <xsl:when test="$actor/Decl[@kind='Variable' and @name=$paramName]">
          <Decl kind="Parameter" id="{@id}" name="{@name}">
            <xsl:copy-of select="Type"/>
            <xsl:copy-of select="$actor/Parameter[@name = $paramName]/Expr"/>
          </Decl>
        </xsl:when>
        
        <xsl:when test="$instance/Parameter[@name= $paramName]">
          <Decl kind="Variable" id="{@id}" name="{@name}">
            <xsl:copy-of select="Type"/>
            <xsl:copy-of select="$instance/Parameter[@name = $paramName]/Expr"/>
          </Decl>
        </xsl:when>
        
        <xsl:otherwise>
          <!-- Preserve the existing element information -->  
          <xsl:copy>
            <xsl:for-each select="@*">
              <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates/>
          </xsl:copy>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <!-- leave the element unmodified -->
      <xsl:copy>
        <xsl:for-each select="@*">
          <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
        </xsl:for-each>

        <xsl:apply-templates/>
      </xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
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