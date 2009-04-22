
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    version="2.0">
    
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        Copy XDF documents that are referred to by this document hierarchically. 
        It includes also the headers of atomic actors, i.e. their port and parameter 
        declrations.
        <p/>
        The result is a nested XDF document whose only outside references are 
        to atomic actors.
        <p/>
        FIXME: Currently, the network/actor lookup mechanism assumes that all files are residing in the same directory.
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006</xd:copyright>
    </xd:doc>
    
    <xsl:template match="Instance">
      <!-- Resolve the package from the QID tags and generate a qualified name for the cal/xdf -->
        <xsl:variable name="thePackage">
          <xsl:for-each select="Class/QID/ID">
            <xsl:value-of select="concat(@id,'/')"/>
          </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="calmlname" select="concat($thePackage,Class/@name, '.calml')"/>
        <xsl:variable name="xdfname" select="concat($thePackage,Class/@name, '.xdf')"/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>

            <!-- If there is already an Actor tag, omit it in preference of the one we are going to add below -->
            <xsl:apply-templates select="*[not( self::Actor )] | text()"/>

            <xsl:choose>

		<xsl:when test="./XDF">
		  <!-- Do nothing as the XDF is already inlined. -->
		</xsl:when>

                <xsl:when test="doc-available($xdfname) and doc($xdfname)/XDF and not(./XDF)">
                  <!-- The sourceLocation note is used late in the HW codegen process, by xdfInstantiateActor
                       to tie together the prm (Instance element with only params, decls, and ports) with
                       the 'clean' calml in order to create an 'instantiated' calml.  Setting the Note here
                       ensures that the backend codegeneration uses exactly the same calml as was discovered
                       here. -->
                  <Note kind="sourceLocation" value="{$xdfname}"/>
                  <xsl:apply-templates select="doc($xdfname)/XDF"/>
                </xsl:when>
                <xsl:when test="doc-available($calmlname) and doc($calmlname)/Actor">
                  <!-- See note above for sourceLocation reason. -->
                  <Note kind="sourceLocation" value="{$calmlname}"/>
                  <xsl:variable name="A" select="doc($calmlname)/Actor"/>
                  <Actor name="{$A/@name}">
                    <xsl:if test="./Actor"> <!-- Pick up the attributes of any existing Actor element, such as id -->
                      <xsl:for-each select="./Actor/@*">
                        <xsl:attribute name="{name()}">
                          <xsl:value-of select="."/>
                        </xsl:attribute>
                      </xsl:for-each>
                    </xsl:if>
                    <xsl:copy-of select="$A/Port"/>
                    <xsl:copy-of select="$A/Decl[@kind='Parameter']"/>
                    <xsl:copy-of select="Directive"/>
                  </Actor>
                </xsl:when>
                <xsl:otherwise>
                  <ERROR source="xdfInline">Cannot find definition for actor class <xsl:value-of select="Class/@name"/>.</ERROR>
                  <xsl:message terminate="yes">
                    <ERROR source="xdfInline">Cannot find definition for actor class <xsl:value-of select="Class/@name"/>.</ERROR>
                  </xsl:message>
                </xsl:otherwise>
            </xsl:choose>

            
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates select="* | text()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>



