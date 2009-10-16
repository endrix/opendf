
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:math="http://exslt.org/math"
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
    version="2.0">
    
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx, 2006</xd:copyright>
    </xd:doc>
    
    <xsl:template match="/">
        <xsl:variable name="emptyEnv"><Env/></xsl:variable>
        <xsl:copy>
            
            <xsl:apply-templates>
                <xsl:with-param name="env" select="$emptyEnv/Env"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    
    <xsl:template match="XDF">
        <xsl:param name="env"/>
        
        <xsl:variable name="env2">
            <xsl:call-template name="processDecls">
                <xsl:with-param name="env" select="$env"/>
                <xsl:with-param name="decls" select="Decl[@kind='Var']"/>
            </xsl:call-template>
        </xsl:variable>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates>
                <xsl:with-param name="env" select="$env2/Env"/>
            </xsl:apply-templates>
            
        </xsl:copy>
    </xsl:template>
    
    
    <xsl:template match="Instance">
        <xsl:param name="env"/>
                
        <xsl:variable name="newEnv">
            <xsl:call-template name="processParameters">
                <xsl:with-param name="params" select="Parameter"/>
                <xsl:with-param name="env" select="$env"/>
            </xsl:call-template>        
        </xsl:variable>        

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates>
                <xsl:with-param name="env" select="$newEnv/Env"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="Actor">
        <xsl:param name="env"/>
        
        <xsl:variable name="a" select="."/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates>
                <xsl:with-param name="env" select="$env"/>
            </xsl:apply-templates>
            
            <xsl:for-each select="../Parameter">
                <xsl:variable name="p" select="."/>
                <xsl:variable name="e" select="$env/Decl[@name=$p/@name][last()]/Expr"/>
                <Parameter name="{$p/@name}">
                    <xsl:copy-of select="$e"/>
                    <xsl:choose>
                        <xsl:when test="$a/Decl[@kind='Parameter'][@name=$p/@name]/Type">
                            <xsl:copy-of select="$a/Decl[@kind='Parameter'][@name=$p/@name]/Type"/>
                        </xsl:when>
                        <xsl:when test="$e/Note[@kind='exprType']/Type">
                            <xsl:copy-of select="$e/Note[@kind='exprType']/Type"/>                            
                        </xsl:when>
                        <xsl:otherwise>
                            <ERROR>Cannot find type associated with eithe parameter declaration or the actual parameter value.</ERROR>
                        </xsl:otherwise>
                    </xsl:choose>
                </Parameter>
            </xsl:for-each>
        </xsl:copy>        
    </xsl:template>
    
    <xsl:template match="Expr">
        <xsl:param name="env"/>

        <xsl:copy-of select="cal:evaluateExpr(., $env)"/>
    </xsl:template>
    
    <xd:doc>
        Eat Decls. The resulting document is free of declarations, except for the 
        parameter definitions of atomic actors, which are explicitly constructed.
    </xd:doc>
    <xsl:template match="Decl"></xsl:template>
    

    <xd:doc>
        Eat Parameters. The resulting document is free of parameters, except for the 
        parameter definitions of atomic actors, which are explicitly constructed.
    </xd:doc>
    <xsl:template match="Parameter"></xsl:template>
    
    
    <xsl:template match="*">
        <xsl:param name="env"/>
                
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}">
                    <xsl:value-of select="."/>
                </xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates select="* | text()">
                <xsl:with-param name="env" select="$env"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    
    <xsl:template name="processParameters">
        <xsl:param name="params"/>
        <xsl:param name="env"/>
        
        <Env>
        <!--     <xsl:copy-of select="$env/Env/*"/>  -->
            <xsl:for-each select="$params">
                <xsl:variable name="p" select="."/>
                <Decl name="{$p/@name}">
                    <xsl:variable name="constant" select="cal:evaluateExpr( $p/Expr, $env )"></xsl:variable>
                    <xsl:copy-of select="$constant"/>
                    <xsl:choose>
                        <xsl:when test="$constant/Note[@kind='exprType']">
                            <xsl:copy-of select="$constant/Note[@kind='exprType']/Type"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <ERROR source="xdfPropagateConstants">Cannot find type.</ERROR>
                        </xsl:otherwise>
                    </xsl:choose>
                </Decl>                
            </xsl:for-each>
        </Env>
    </xsl:template>

    <xsl:template name="processDecls">
        <xsl:param name="decls"/>
        <xsl:param name="env"/>
        
        <xsl:choose>
            <xsl:when test="$decls">
                <xsl:variable name="d1" select="$decls[1]"/>
                <xsl:variable name="drst" select="$decls[position() > 1]"/>
                <xsl:variable name="newenv">
                    <Env>
                        <xsl:copy-of select="$env/*"/>
                        <Decl name="{$d1/@name}">
                            <xsl:variable name="constant" select="cal:evaluateExpr( $d1/Expr, $env )"></xsl:variable>
                            <xsl:copy-of select="$constant"/>
                            <xsl:choose>
                                <xsl:when test="$constant/Note[@kind='exprType']">
                                    <xsl:copy-of select="$constant/Note[@kind='exprType']/Type"/>
                                </xsl:when>
                                <xsl:when test="$d1/Type">
                                    <xsl:copy-of select="$d1/Type"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <ERROR source="xdfPropagateConstants">Cannot find type.</ERROR>
                                </xsl:otherwise>
                             </xsl:choose>
                        </Decl>
                    </Env>
                </xsl:variable>
                <xsl:call-template name="processDecls">
                    <xsl:with-param name="decls" select="$drst"/>
                    <xsl:with-param name="env" select="$newenv/Env"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$env"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
</xsl:stylesheet>



