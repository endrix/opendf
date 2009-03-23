<!--
	TrueSource.xslt
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
  version="1.1"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  >
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Augment the Notes[@kind='var-used'] to indicate true source of a value.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on SSANotes</li>
      </ul>
      Add an attribute to the var-usage with mode 'read' that indicates the 'true source' of the
      value. This is the ID of the Expr/phi-block/Actor state variable/port read that produces
      the value.
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: TrueSource.xslt 1126 2005-12-29 17:28:13Z stephenn $</xd:cvsId>
  </xd:doc>

  <xd:doc>Augment read notes to point to the true source.</xd:doc>
  <xsl:template match="Note[ @kind='var-used' and @mode='read' ]">
    
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
 
      <xsl:variable name="true-source">
        <xsl:choose>
          <!-- If this note is in an actor, then it must be an external (no true source known)-->
          <xsl:when test="../Actor"/>
          
          <xsl:otherwise>
            <xsl:apply-templates select="../.." mode="true-source">
              <xsl:with-param name="decl-id" select="@decl-id"/>
              <xsl:with-param name="modifier-id" select="@preceding-sibling-modifier"/>
            </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:if test="string-length(string($true-source)) > 0">
        <xsl:attribute name="true-source"><xsl:value-of select="$true-source"/></xsl:attribute>
      </xsl:if>
               
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template> 

  <xd:doc>Augment write notes which have a last-child-modifier attribute to point to the true source,
      which is useful for implementing PHI.</xd:doc>
  <xsl:template match="Note[ @kind='var-used' and @mode='write' ]">

    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>

      <xsl:if test="@last-child-modifier">      
        <xsl:attribute name="true-source">
          <xsl:apply-templates select=".." mode="true-source">
            <xsl:with-param name="decl-id" select="@decl-id"/>
            <xsl:with-param name="modifier-id" select="@last-child-modifier"/>
          </xsl:apply-templates>
        </xsl:attribute>
      </xsl:if>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template> 
  
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template> 

  <xsl:template match="*" mode="true-source">
    <xsl:param name="decl-id"/>
    <xsl:param name="modifier-id"/>
            
    <xsl:choose>

      <!-- In the Actor context this can only be a reference to an actor state variable -->        
      <xsl:when test="self::Actor">
        <xsl:value-of select="$decl-id"/>
      </xsl:when>

      <!-- When there is no modifier and the current context is a While that writes to
           this variable, take the value from the PHI block instead of popping up
           to the parent context -->
      <xsl:when test="self::Stmt[@kind='While'] and string-length(string($modifier-id)) = 0
                      and Note[@kind='var-used'][@mode='write'][@decl-id=$decl-id]">
        <xsl:value-of select="concat( @id, '$PHI$', $decl-id )"/>
      </xsl:when>
        
      <!-- When there is no modifier in the current context, look to the parent context -->
      <xsl:when test="string-length(string($modifier-id)) = 0">
        <xsl:apply-templates select=".." mode="true-source">
          <xsl:with-param name="decl-id" select="$decl-id"/>
          <xsl:with-param name="modifier-id" select="Note[@kind='var-used']
            [@mode='read'][@decl-id=$decl-id]/@preceding-sibling-modifier"/>
        </xsl:apply-templates>
      </xsl:when>

      <!-- Backtrack to the local modifier -->
           
      <!-- Variable Decl is a true source -->
      <xsl:when test="Decl[@kind='Variable'][ @id = $modifier-id ]">
        <xsl:value-of select="$modifier-id"/>
      </xsl:when>

      <!-- Decl inside an Input is a true source -->
      <xsl:when test="Decl[ @id = $modifier-id ][ parent::Input ]">
        <xsl:value-of select="$modifier-id"/>
      </xsl:when>
          
      <!-- Flow control block PHI function is a true source -->
      <xsl:when test="Stmt[ @kind='If' or @kind='While' ][ @id=$modifier-id ]">
        <xsl:value-of select="concat( $modifier-id, '$PHI$', $decl-id )"/>
      </xsl:when>

      <!-- Assign is a true source -->
      <xsl:when test="Stmt[ @kind='Assign' ][ @id=$modifier-id ]">
        <xsl:value-of select="$modifier-id"/>
      </xsl:when>

      <!-- Expr is a true source -->
      <xsl:when test="Expr[@id=$modifier-id]">
        <xsl:value-of select="$modifier-id"/>
      </xsl:when>
                    
      <!-- Otherwise, step into the modifier -->
      <xsl:otherwise>
        <!-- For debug trap missing modifier -->
        <xsl:if test="string-length( string( *[@id=$modifier-id]/Note[@kind='var-used']
          [@mode='write'][@decl-id=$decl-id]/@last-child-modifier ) ) = 0">
          <xsl:message terminate="yes">
            Fatal flaw in var-used notes: missing last-child-modifier
          </xsl:message>
        </xsl:if>           
            
        <xsl:apply-templates select="*[@id=$modifier-id]" mode="true-source">
          <xsl:with-param name="decl-id" select="$decl-id"/>
          <xsl:with-param name="modifier-id" select="*[@id=$modifier-id]/Note[@kind='var-used']
          [@mode='write'][@decl-id=$decl-id]/@last-child-modifier"/>
        </xsl:apply-templates>

      </xsl:otherwise>
      
    </xsl:choose>
  </xsl:template>
   
</xsl:stylesheet>
