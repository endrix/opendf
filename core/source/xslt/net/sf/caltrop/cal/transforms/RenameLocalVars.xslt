<!--
    RenameLocalVars

    Xilinx Confidential
    
    Copyright (c) 2005 Xilinx Inc.
-->

<xsl:stylesheet
    xmlns:xd="http://www.pnp-software.com/XSLTdoc"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="2.0">
    <xsl:output method="xml"/>
    
    <xd:doc type="stylesheet">
        <xd:short>Rename all local variables.</xd:short>
        <xd:detail>
            Rename the declarations and references to all variables that are neither global nor actor state variables.
            <p/>
        </xd:detail>
        <xd:author>JWJ</xd:author>
        <xd:copyright>Xilinx Inc., 2005</xd:copyright>
        <xd:cvsId>$Id: RenameLocalVars.xslt 1018 2005-11-04 20:14:06Z jornj $</xd:cvsId>
    </xd:doc>
    
    
    <xsl:template match="Actor">
        <xsl:param name="localVars" select="UNDEFINED"/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <!-- Use this actor as the scope for the contained elements. -->
            <xsl:apply-templates select="node() | text()">
                <xsl:with-param name="localVars" select="UNDEFINED"/>
            </xsl:apply-templates>
        </xsl:copy>        
    </xsl:template>
    
    <xsl:template match="Expr[@kind='Var']">
        <xsl:param name="localVars" select="UNDEFINED"/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:variable name="name" select="."/>
                <xsl:choose>
                    <xsl:when test="(name() = 'name') and ($localVars/substitution/@name = $name)">
                        <xsl:attribute name="name"><xsl:value-of select="$localVars/substitution[@name=$name]/@new-name"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()">
                <xsl:with-param name="localVars" select="$localVars"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="Decl">
        <xsl:param name="localVars" select="UNDEFINED"/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:variable name="name" select="."/>
                <xsl:choose>
                    <xsl:when test="(name() = 'name') and ($localVars/substitution/@name = $name)">
                        <xsl:attribute name="name"><xsl:value-of select="$localVars/substitution[@name=$name]/@new-name"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()">
                <xsl:with-param name="localVars" select="$localVars"/>
            </xsl:apply-templates>
        </xsl:copy>
        
    </xsl:template>
    
    <xsl:template match="Stmt[@kind='Assign']">
        <xsl:param name="localVars" select="UNDEFINED"/>
        
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:variable name="name" select="."/>
                <xsl:choose>
                    <xsl:when test="(name() = 'name') and ($localVars/substitution/@name = $name)">
                        <xsl:attribute name="name"><xsl:value-of select="$localVars/substitution[@name=$name]/@new-name"/></xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()">
                <xsl:with-param name="localVars" select="$localVars"/>
            </xsl:apply-templates>
        </xsl:copy>
        
    </xsl:template>
    
    <xsl:template match="*">
        <xsl:param name="localVars" select="UNDEFINED"/>
        
        <xsl:variable name="allLocalVars">
            <xsl:copy-of select="$localVars/substitution"/>
            <xsl:for-each select="Decl | Input/Decl | Generator/Decl">
                <xsl:variable name="name" select="@name"/>
                <xsl:if test="not($localVars/substitution/@name = $name)"> <!-- avoid doubles -->
                    <substitution name="{$name}" new-name="$local${$name}"/>
                </xsl:if>
            </xsl:for-each>
            
        </xsl:variable>        

        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            <xsl:apply-templates select="node() | text()">
                <xsl:with-param name="localVars" select="$allLocalVars"/>
            </xsl:apply-templates>
        </xsl:copy>
    </xsl:template>
       
 </xsl:stylesheet>
