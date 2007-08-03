
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"

    xmlns:loading="java:net.sf.caltrop.util.Loading"  
    xmlns:xnlext="java:net.sf.caltrop.xslt.nl.Elaborating"  
    xmlns:cal="java:net.sf.caltrop.xslt.cal.CalmlEvaluator"  
    
    version="2.0">
    
    <xsl:output method="xml"/>
    <xsl:include href="net/sf/caltrop/transforms/CopyRequiredDecls.xslt"/>
    
    <xsl:param name="actorParameters"><Parameters/></xsl:param>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2007</xd:copyright>
    </xd:doc>
    
    <xsl:template match="/">
        <xsl:variable name="model">
            <xsl:call-template name="inlineParameters">
                <xsl:with-param name="model" select="XDF | Network | Actor"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="XDF">
                <xsl:apply-templates select="$model/*"/>
            </xsl:when>
            <xsl:when test="Network">
                <xsl:apply-templates select="xnlext:elaborate($model)"/> 
            </xsl:when>
            <xsl:when test="Actor">
                <xsl:copy-of select="$model/Actor"/>
            </xsl:when>
            <xsl:otherwise>
                <ERROR source="XNL Elaborator:">Undefined top-level construct.</ERROR>
            </xsl:otherwise>
        </xsl:choose>            
    </xsl:template>
    
    <xsl:template match="Instance">
        <xsl:variable name="thePackage">
            <xsl:for-each select="Class/QID/ID">
                <xsl:value-of select="concat(@id, '.')"/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="className" select="concat($thePackage, Class/@name)"/>
        <xsl:variable name="source" select="loading:loadActorSource($className)"/>
        
        <xsl:variable name="env">
            <Env>
                <xsl:copy-of select="../Decl[@kind='Variable']"/>
            </Env>
        </xsl:variable> 

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
                        
            <xsl:apply-templates select="Class | Note | Attribute"/>
            
            <xsl:choose>
                <xsl:when test="$source/XDF">
                    <Note kind="sourceInfo" className="{$className}" type="XDF"/>
                    <xsl:copy-of select="Parameter"/>
                    <xsl:variable name="instantiatedSource">
                        <xsl:call-template name="instantiateXDF">
                            <xsl:with-param name="n" select="$source/XDF"/>
                            <xsl:with-param name="env" select="$env"/>
                            <xsl:with-param name="pars" select="Parameter"/>
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:apply-templates select="$instantiatedSource/XDF"/>
                </xsl:when>
                <xsl:when test="$source/Network">
                    <Note kind="sourceInfo" className="{$className}" type="XNL"/>
                    <xsl:copy-of select="Parameter"/>
                    <xsl:variable name="instantiatedSource">
                        <xsl:call-template name="instantiateXNL">
                            <xsl:with-param name="n" select="$source/Network"/>
                            <xsl:with-param name="env" select="$env"/>
                            <xsl:with-param name="pars" select="Parameter"/>
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:apply-templates select="xnlext:elaborate($instantiatedSource)"/> 
                </xsl:when>
                <xsl:when test="$source/Actor">
                    <Note kind="sourceInfo" className="{$className}" type="Actor"/>
                    <xsl:copy-of select="Parameter"/>
                    <Actor/>
<!--                    <Actor name="{$source/Actor/@name}">
                        <xsl:copy-of select="Port | Decl[@kind='Parameter'] | Directive"/>
                    </Actor> -->
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="Parameter"/>
                    <Actor/>
                    <ERROR source="XNL Elaborator:">Cannot find definition for actor class <xsl:value-of select="$className"/>.
                    <xsl:copy-of select="$source"/></ERROR>
                </xsl:otherwise>
            </xsl:choose>            
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="Parameter"></xsl:template> <!-- swallow parameters -->
    
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

    <xsl:template name="instantiateXDF">
        <xsl:param name="n"/>
        <xsl:param name="env"/>
        <xsl:param name="pars"/>
        
        <XDF>
            <xsl:for-each select="$n/Decl[@kind='Param']">
                <xsl:variable name="nm" select="@name"/>
                
                <Decl kind="Variable" name="{$nm}">
                    <xsl:copy-of select="Type"/>
                    <Expr kind="Let">
                      <!--<xsl:copy-of select="$env/Env/Decl"/> -->
                      <xsl:call-template name="copyAllRequiredDecls">
                        <xsl:with-param name="expr" select="$pars[@name=$nm]/Expr"/>
                        <xsl:with-param name="env" select="$env"/>
                      </xsl:call-template>

                        <xsl:copy-of select="$pars[@name=$nm]/Expr"/>
                    </Expr>
                </Decl> 
            </xsl:for-each>
            
            <xsl:for-each select="$n/*">
                <xsl:if test="not (self::Decl[@kind='Param'])">
                    <xsl:copy-of select="."/>
                </xsl:if>
            </xsl:for-each>
        </XDF>
    </xsl:template>
    
    <xsl:template name="instantiateXNL">
        <xsl:param name="n"/>
        <xsl:param name="env"/>
        <xsl:param name="pars"/>
        
        <Network>
            <xsl:for-each select="$n/Decl[@kind='Parameter']">
                <xsl:variable name="nm" select="@name"/>
                
                <Decl kind="Variable" name="{$nm}">
                    <xsl:copy-of select="Type"/>
                    <Expr kind="Let">
                      <!-- <xsl:copy-of select="$env/Env/Decl"/>-->
                      <xsl:call-template name="copyAllRequiredDecls">
                        <xsl:with-param name="expr" select="$pars[@name=$nm]/Expr"/>
                        <xsl:with-param name="env" select="$env"/>
                      </xsl:call-template>
                        <xsl:copy-of select="$pars[@name=$nm]/Expr"/>
                    </Expr>
                </Decl> 
            </xsl:for-each>
            
            <xsl:for-each select="$n/*">
                <xsl:if test="not (self::Decl[@kind='Parameter'])">
                    <xsl:copy-of select="."/>
                </xsl:if>
            </xsl:for-each>
        </Network>
    </xsl:template>
    
    <xsl:template name="inlineParameters">
        <xsl:param name="model"/>
        
        <xsl:element name="{name($model)}">
            <xsl:for-each select="$model/@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:for-each select="$model/*">
                <xsl:choose>
                    <xsl:when test="Decl[@kind='Parameter'] | Decl[@kind='Param']">
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            
            <xsl:for-each select="$actorParameters/Parameters/Parameter">
                <xsl:variable name="v" select="@name"/>
                <Decl kind="Variable" name="{@name}">
                    <xsl:copy-of select="cal:parseExpression(@value)"/>
                    <xsl:copy-of select="Decl[@kind='Param'][@name=$v]/Type"/>
                    <xsl:copy-of select="Decl[@kind='Parameter'][@name=$v]/Type"/>
                </Decl>
            </xsl:for-each>
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>



