<!--
    Folds XDF Attributes

    author: JWJ
-->

<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.1"
    xmlns:exsl="http://exslt.org/common"
    xmlns:set="http://exslt.org/sets"
    xmlns:cal="java:net.sf.opendf.xslt.cal.CalmlEvaluator"  
    extension-element-prefixes="exsl set">
    
    <xsl:output method="xml"/>
    
    <xsl:param name="foldingConfig-uri" select="'net/sf/opendf/transforms/xdfAttributeFoldingConfig.xml'"/>
    <xsl:variable name="config" select="document($foldingConfig-uri)/AttributeFolding"/>
    
    <xsl:template match="Connection">
        <xsl:variable name="c" select="."/>
        <Connection
            src = "{@src}"
            src-port = "{@src-port}"
            dst = "{@dst}"
            dst-port = "{@dst-port}">
            
            <xsl:for-each select="$config/ConnectionAttribute[@attrKind='Value']">
                <xsl:variable name="name" select="@attrName"/>
                <xsl:variable name="attrs" select="$c/Attribute[@kind='Value'][@name=$name]"/>
                
                <xsl:choose>
                    <xsl:when test="count($attrs) >= 1">
                        <Attribute name="{$name}" kind="Value">
                            <Expr kind="Application">
                                <xsl:copy-of select="cal:parseExpression(FoldingFunction/@value)"/>
                                <Args>
                                    <Expr kind="List">
                                        <xsl:for-each select="$attrs">
                                            <xsl:copy-of select="./Expr"/>
                                        </xsl:for-each>
                                    </Expr>
                                </Args>
                            </Expr>
                        </Attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="DefaultValue">
                            <Attribute name="{$name}" kind="Value">
                                <xsl:copy-of select="cal:parseExpression(DefaultValue/@value)"/>
                            </Attribute>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            
            <xsl:if test="$config/ConnectionAttributeDefault[@attrKind='Value']">
                <xsl:for-each select="$c/Attribute[@kind='Value']">
                    <xsl:variable name="a" select="."/>
                    <xsl:variable name="attrs" select="$c/Attribute[@kind='Value'][@name=$a/@name]"/>
                    <xsl:if test="every $ca in $config/ConnectionAttribute[@attrKind='Value'] satisfies $ca/@attrName ne $a/@name">
                        <xsl:if test="not($a/preceding-sibling::Attribute[@kind='Value'][@name=$a/@name])">
                            <Attribute name="{$a/@name}" kind="Value">
                                <Expr kind="Application">
                                    <xsl:copy-of select="cal:parseExpression($config/ConnectionAttributeDefault/FoldingFunction/@value)"/>
                                    <Args>
                                        <Expr kind="List">
                                            <xsl:for-each select="$attrs">
                                                <xsl:copy-of select="./Expr"/>
                                            </xsl:for-each>
                                        </Expr>
                                    </Args>
                                </Expr>
                            </Attribute>                            
                        </xsl:if>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>
            
        </Connection>
    </xsl:template>
    
    <xsl:template match="Instance">
        <xsl:variable name="n" select="."/>
        <Instance>
            <xsl:for-each select="@*">
                <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
            </xsl:for-each>
            
            <xsl:apply-templates select="*[not(self::Attribute)]"/>
            
            <xsl:for-each select="$config/InstanceAttribute[@attrKind='Value']">
                <xsl:variable name="name" select="@attrName"/>
                <xsl:variable name="attrs" select="$n/Attribute[@kind='Value'][@name=$name]"/>
                
                <xsl:choose>
                    <xsl:when test="count($attrs) >= 1">
                        <Attribute name="{$name}" kind="Value">
                            <Expr kind="Application">
                                <xsl:copy-of select="cal:parseExpression(FoldingFunction/@value)"/>
                                <Args>
                                    <Expr kind="List">
                                        <xsl:for-each select="$attrs">
                                            <xsl:copy-of select="./Expr"/>
                                        </xsl:for-each>
                                    </Expr>
                                </Args>
                            </Expr>
                        </Attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:if test="DefaultValue">
                            <Attribute name="{$name}" kind="Value">
                                <xsl:copy-of select="cal:parseExpression(DefaultValue/@value)"/>
                            </Attribute>
                        </xsl:if>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            
            <xsl:if test="$config/InstanceAttributeDefault[@attrKind='Value']">
                <xsl:for-each select="$n/Attribute[@kind='Value']">
                    <xsl:variable name="a" select="."/>
                    <xsl:variable name="attrs" select="$n/Attribute[@kind='Value'][@name=$a/@name]"/>
                    <xsl:if test="every $ca in $config/InstanceAttribute[@attrKind='Value'] satisfies $ca/@attrName ne $a/@name">
                        <xsl:if test="not($a/preceding-sibling::Attribute[@kind='Value'][@name=$a/@name])">
                            <Attribute name="{$a/@name}" kind="Value">
                                <Expr kind="Application">
                                    <xsl:copy-of select="cal:parseExpression($config/InstanceAttributeDefault/FoldingFunction/@value)"/>
                                    <Args>
                                        <Expr kind="List">
                                            <xsl:for-each select="$attrs">
                                                <xsl:copy-of select="./Expr"/>
                                            </xsl:for-each>
                                        </Expr>
                                    </Args>
                                </Expr>
                            </Attribute>                            
                        </xsl:if>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>
            
        </Instance>
    </xsl:template>
        
    <xsl:template match="Attribute">
        <xsl:if test="parent::XDF">
            <xsl:copy>
                <xsl:for-each select="@*">
                    <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
                </xsl:for-each>
                
                <xsl:apply-templates select="node() | text()"/>
            </xsl:copy>
        </xsl:if>
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
