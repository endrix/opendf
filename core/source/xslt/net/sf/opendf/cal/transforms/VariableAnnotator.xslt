<!--
    VariableAnnotator

    Adds a Note element to each variable reference, of the following format:

    <Note kind="varRef" free="yes/no" assignable="yes/no" mutable="yes/no" scope-id="<id>">
        <Type ... />
    </Note>

    assumes: TagPorts, AddID

    IS reentrant.  Any prior varRef or varMod notes are stripped and new ones created.

    author: JWJ
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0"
  xmlns:exsl="http://exslt.org/common"
  xmlns:set="http://exslt.org/sets"
  extension-element-prefixes="xsl exsl set">

  <xsl:output method="xml"/>

    <xsl:template match="Actor">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="Note[@kind='varRef' or @kind='varMod']">
      <!-- Suppress any prior varRef or varMod notes and rebuild the annotation entirely -->
    </xsl:template>

    <xsl:template match="Expr[@kind='Var']|Stmt[@kind='Assign']">
        <xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
        <xsl:variable name="ref-type">
          <xsl:choose>
            <xsl:when test="name()='Expr'">varRef</xsl:when> 
            <xsl:otherwise>varMod</xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <!--
            Compute list of all lexically enclosing decls of the name $name.
            Later, if that is not empty, choose the last.
            Decls are enumerated along the ancestor axis: innermost decl is last.
            
            DBP: Fix bug that causes assignable and mutable attributes to have lists of values
            or be an empty string. Also, last "when" is never executed?

            Also, normalize the assignable/mutable values to be either 'yes' or 'no'
            TDB: are the defaults right in the event of missing attribute string?

            Also, what about Generator Decls?
        -->
        <xsl:variable name="decls">
          <xsl:for-each select="ancestor::*">

            <xsl:choose>
              <xsl:when test="Decl[@kind='Variable'][@name=$name]">
                <xsl:variable name="this-decl" select="Decl[@kind='Variable'][@name=$name]"/>
                <Note kind="{$ref-type}" free="no" scope-id="{@id}" decl-id="{$this-decl/@id}">
                  <xsl:attribute name="assignable">
                    <xsl:call-template name="normalize-yesno">
                      <xsl:with-param name="string">
                        <xsl:value-of select="$this-decl/@assignable"/>
                      </xsl:with-param>
                      <xsl:with-param name="default">yes</xsl:with-param>
                    </xsl:call-template>
                  </xsl:attribute>
                  <xsl:attribute name="mutable">
                    <xsl:call-template name="normalize-yesno">
                      <xsl:with-param name="string">
                        <xsl:value-of select="$this-decl/@mutable"/>
                      </xsl:with-param>
                      <xsl:with-param name="default">no</xsl:with-param>
                     </xsl:call-template>
                  </xsl:attribute>
                  <xsl:attribute name="actor-scope">
                    <xsl:choose>
                        <xsl:when test="self::Actor">yes</xsl:when>
                        <xsl:otherwise>no</xsl:otherwise>
                    </xsl:choose>                      
                  </xsl:attribute>    
                  <xsl:copy-of select="$this-decl/Type"/>                              
                </Note>
              </xsl:when>
              <xsl:when test="Decl[@kind='Parameter'][@name=$name]">
                <xsl:variable name="this-decl" select="Decl[@kind='Parameter'][@name=$name]"/>
                <Note kind="{$ref-type}" free="no" assignable="no" mutable="no" scope-id="{@id}"
                      decl-id="{$this-decl/@id}">
                    <xsl:copy-of select="$this-decl/Type"/>
                </Note>
              </xsl:when>
              <xsl:when test="Input/Decl[@name=$name]">
                <xsl:variable name="port"><xsl:value-of select="Input/@port"/></xsl:variable>
                <xsl:variable name="this-decl" select="Input/Decl[@name=$name]"/>
                <Note kind="{$ref-type}" free="no" assignable="no" mutable="no" scope-id="{@id}" decl-id="{$this-decl/@id}">
                  <xsl:copy-of select="ancestor::*/Port[@name=$port][1]/Type"/>
                </Note>
              </xsl:when>
              <!-- DBP: this appears to be useless
                      <xsl:when test="Input/Decl[@name=$name]">
                        <xsl:variable name="port"><xsl:value-of select="Input/@port"/></xsl:variable>
                        <Note kind="{$ref-type}" free="no" assignable="no" mutable="no" scope-id="{@id}">
                            <Type name="Seq">
                                <TypePars>
                                    <xsl:copy-of select="ancestor::*/Port[@name=$port][1]/Type"/>
                                </TypePars>
                            </Type>
                        </Note>
                    </xsl:when>
             -->
            <!-- DBP Add support for Generators -->
              <xsl:when test="Generator/Decl[@name=$name]">
                <xsl:variable name="this-decl" select="Generator/Decl[@name=$name]"/>
                <Note kind="{$ref-type}" free="no" assignable="no" mutable="no" scope-id="{$this-decl/../../@id}"
                   decl-id="{$this-decl/@id}">
                  <xsl:copy-of select="$this-decl/Type"/>
                </Note>
              </xsl:when>
            </xsl:choose>
          </xsl:for-each>
        </xsl:variable>
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="*"/>

            <!--
                Pick the first (innermost) decl if there is one. Otherwise, it is a free variable.
            -->
            <xsl:choose>
                <xsl:when test="$decls/Note">
                    <!-- FIXME: Is this a bug in Saxon? The Note elements should have been created in reverse
                        document order, because we used the ancestor:: axis. So if this is a bug, the 'correct'
                        select in the next line would be:
                            exsl:node-set($decls)/Note[1]
                            
                       DBP: changed to 2.0 and reverified with a test case on 4/3/08.
                       The nearest ancestor is last(), when it should be [1] according to docs
                    -->
                    <xsl:copy-of select="$decls/Note[last()]"/>
                </xsl:when>
                <xsl:otherwise>
                    <Note kind="varRef" free="yes" assignable="no" mutable="no"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:copy>
    </xsl:template>


    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>

            <xsl:apply-templates select="node() | text()"/>
        </xsl:copy>
    </xsl:template>

  <xsl:template name="normalize-yesno">
    <xsl:param name="string"/>
    <xsl:param name="default"/>
    
    <xsl:choose>
      <xsl:when test="string-length($string) = 0">
        <xsl:value-of select="$default"/>
      </xsl:when>
      <xsl:when test="translate($string,'YES'  ,'yes'  ) = 'yes'  ">yes</xsl:when>
      <xsl:when test="translate($string,'TRUE' ,'true' ) = 'true' ">yes</xsl:when>
      <xsl:when test="translate($string,'Y'    ,'y'    ) = 'y'    ">yes</xsl:when>
      <xsl:when test="translate($string,'T'    ,'t'    ) = 't'    ">yes</xsl:when>
      <xsl:when test="translate($string,'NO'   ,'no'   ) = 'no'   ">no</xsl:when>
      <xsl:when test="translate($string,'FALSE','false') = 'false'">no</xsl:when>
      <xsl:when test="translate($string,'N'    ,'n'    ) = 'n'    ">no</xsl:when>
      <xsl:when test="translate($string,'F'    ,'f'    ) = 'f'    ">no</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string"/>
      </xsl:otherwise>
    </xsl:choose>  
  </xsl:template>
    
</xsl:stylesheet>





