<!--
    BuildProductSchedule
    
    Compute a single schedule FSM as the product of several FSMs.
    
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
                <xsl:copy/>
            </xsl:for-each>
            
            <xsl:apply-templates select="*[not(self::Schedule)]"/>
            
            <xsl:call-template name="multiply-schedules">
                <xsl:with-param name="head" select="Schedule[1]"/>
                <xsl:with-param name="tail" select="Schedule[position() > 1]"/>
            </xsl:call-template>
            
        </xsl:copy>        
    </xsl:template>
    
    
    <xsl:template match="*">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:copy/>
            </xsl:for-each>
            
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <xsl:template name="multiply-schedules">
        <xsl:param name="head"/>
        <xsl:param name="tail"/>
        
        <xsl:choose>
            <xsl:when test="not($head)"></xsl:when>
            <xsl:when test="not($tail)"><xsl:copy-of select="$head"/></xsl:when>
            <xsl:otherwise>
                <xsl:variable name="firstTwo">
                    <xsl:call-template name="multiply-two-schedules">
                        <xsl:with-param name="s1" select="$head"/>
                        <xsl:with-param name="s2" select="$tail[1]"/>
                    </xsl:call-template>                    
                </xsl:variable>
                <xsl:call-template name="multiply-schedules">
                    <xsl:with-param name="head" select="$firstTwo/Schedule"/>
                    <xsl:with-param name="tail" select="$tail[position() > 1]"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>    
    
    <xsl:template name="multiply-two-schedules">
        <xsl:param name="s1"/>
        <xsl:param name="s2"/>
        
        <xsl:variable name="states1dup">
            <S name="{$s1/@initial-state}"/>
            <xsl:for-each select="$s1/Transition">
                <S name="{@from}"/>
                <S name="{@to}"/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="states1">
            <xsl:copy-of select="$states1dup/S[not(@name=preceding-sibling::S/@name)]"/>
        </xsl:variable>
        <xsl:variable name="states2dup">
            <S name="{$s2/@initial-state}"/>
            <xsl:for-each select="$s2/Transition">
                <S name="{@from}"/>
                <S name="{@to}"/>
            </xsl:for-each>
        </xsl:variable>
        <xsl:variable name="states2">
            <xsl:copy-of select="$states2dup/S[not(@name=preceding-sibling::S/@name)]"/>
        </xsl:variable>
        <Schedule kind="fsm" initial-state="{$s1/@initial-state}#{$s2/@initial-state}">
            <xsl:for-each select="$states1/S">
                <xsl:variable name="s" select="."/>
                <xsl:for-each select="$s2/Transition">
                    <Transition from="{$s/@name}#{@from}" to="{$s/@name}#{@to}">
                        <xsl:copy-of select="ActionTags"/>
                    </Transition>
                </xsl:for-each>
            </xsl:for-each>
            <xsl:for-each select="$states2/S">
                <xsl:variable name="s" select="."/>
                <xsl:for-each select="$s1/Transition">
                    <Transition from="{@from}#{$s/@name}" to="{@to}#{$s/@name}">
                        <xsl:copy-of select="ActionTags"/>
                    </Transition>
                </xsl:for-each>
            </xsl:for-each>
        </Schedule>
    </xsl:template>
    
</xsl:stylesheet>
