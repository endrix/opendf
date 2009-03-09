<!--
  AnnotateConstantIndexers.xslt
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
  extension-element-prefixes="xsl" 
  version="2.0">
  <xsl:output method="xml"/>

  <xsl:key name="read-access"  match="Expr[@kind='Var']" use="@name"/>
  <xsl:key name="write-access" match="Stmt[@kind='Assign']" use="@name"/>
  
  <xsl:template match="Decl[parent::Input[Repeat] or (not(parent::Input) and Type/@name='list')]">
    
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
       
      <xsl:apply-templates/>

      <!-- the list is not scalarizable if it's initializer is not a simple list -->
      <xsl:if test="not( Expr/descendant-or-self::Expr[ (@kind!='List' and @kind!='Literal') or Generator] )">
        
        <!-- how many dimensions of indexing -->
        <xsl:variable name="dims" select="if (parent::Input) then 1 else count( Type//Entry[@kind='Type'] )"/>
        
        <!-- for an input token to be scalarizable all peers must be scalarizable too -->
        <xsl:variable name="decls" select="if (parent::Input) then ../Decl else ."/>
        
        <!-- look for disqualifying accesses -->
        <xsl:variable name="disqual">
          <xsl:for-each select="$decls">
            <xsl:variable name="id" select="@id"/>
            <xsl:variable name="r" select="key('read-access',@name)[Note[@kind='var-used'][@decl-id=$id]]"/>
            <xsl:variable name="w" select="key('write-access',@name)[Note[@kind='var-used'][@mode='write'][@decl-id=$id]]"/>
            
            <!-- read access must be through an indexer with constant indices -->
            <xsl:copy-of select="$r/parent::Expr[@kind!='Indexer' or Args/Expr[@kind!='Literal']
                  or count( Args/Expr ) != $dims ]"/>
            
            <!-- write access must be indexed and with constant indices -->
            <xsl:copy-of select="$w[ Args/Expr[@kind!='Literal'] or count( Args/Expr ) != $dims ]"/>
          </xsl:for-each>
        </xsl:variable>
       
        <xsl:if test="not( $disqual/* )">
          <Note kind="Scalarizable"/>
        </xsl:if>
        
      </xsl:if>
      
    </xsl:copy>
    
  </xsl:template>
    
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