<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0">
   <xsl:output method="text" indent="yes"/>
   <xsl:strip-space elements="*"/>

   <xsl:template match="XDF">

     <xsl:variable name="topName"><xsl:call-template name="createName"><xsl:with-param name="name" select="@id"/></xsl:call-template></xsl:variable>

     <!-- preamble -->
     <xsl:text>digraph structs {
     node [shape=record];
     </xsl:text>
     
     <!-- create boxes for input ports -->
     <xsl:text>struct_</xsl:text><xsl:value-of select="$topName"/><xsl:text>_in [label="</xsl:text>
     <xsl:call-template name="createPorts">
       <xsl:with-param name="ports">
         <xsl:copy-of  select="./Port[@kind='Input']"/>
       </xsl:with-param>
     </xsl:call-template>
     <xsl:text>"];
     </xsl:text>

     <!-- create node boxes -->
     <xsl:apply-templates select="Instance"/>
     
     <!-- create boxes for output ports -->
     <xsl:text>struct_</xsl:text><xsl:value-of select="$topName"/><xsl:text>_out [label="</xsl:text>
     <xsl:call-template name="createPorts">
       <xsl:with-param name="ports">
         <xsl:copy-of select="./Port[@kind='Output']"/>
       </xsl:with-param>
     </xsl:call-template>
     <xsl:text>"];
     </xsl:text>

     <!-- Create connections -->
     <xsl:apply-templates select="Connection"/>

     <!-- postamble -->
     <xsl:text>}
     </xsl:text>
     
   </xsl:template>

   <xsl:template match="Instance">
     <xsl:variable name="this" select="."/>
     <xsl:variable name="actorName"><xsl:call-template name="createName"><xsl:with-param name="name" select="@id"/></xsl:call-template></xsl:variable>
     <xsl:variable name="actorHierarchy"><xsl:call-template name="createHierarchy"></xsl:call-template></xsl:variable>
     <!-- xsl:variable name="qualifiedName" select="concat($actorHierarchy, ' ', Class/@name)"/ -->
     <xsl:text>struct_</xsl:text><xsl:value-of select="$actorName"/><xsl:text> [label="{{</xsl:text>
     
     <xsl:variable name="inputs">
       <xsl:copy-of select="Actor/Port[@kind='Input']"/>
       <xsl:for-each select="../Connection[ @dst = $this/@id ]">
         <Port name="{@dst-port}"/>
       </xsl:for-each>
     </xsl:variable>
     
     <xsl:variable name="outputs">
       <xsl:copy-of select="Actor/Port[@kind='Output']"/>
       <xsl:for-each select="../Connection[ @src = $this/@id ]">
         <Port name="{@src-port}"/>
       </xsl:for-each>
     </xsl:variable>
     
     <xsl:call-template name="createPorts">
       <xsl:with-param name="ports">
         <!-- uniquify the input port list -->
         <xsl:for-each select="$inputs/Port">
           <xsl:variable name="pos" select="position()"/>
           <xsl:variable name="name" select="@name"/>
           <xsl:if test="not( $inputs/Port[ position() &lt; $pos ][ @name = $name ] )">
             <xsl:copy-of select="."/>
           </xsl:if>
         </xsl:for-each>
       </xsl:with-param>
     </xsl:call-template>
     
     <xsl:text>}|</xsl:text><xsl:value-of select="$actorHierarchy"/>
     <xsl:text>|</xsl:text><xsl:value-of select="Class/@name"/>
     <xsl:text>| {</xsl:text>
     
     <xsl:call-template name="createPorts">
       <xsl:with-param name="ports">
         <!-- uniquify the output port list -->
         <xsl:for-each select="$outputs/Port">
           <xsl:variable name="pos" select="position()"/>
           <xsl:variable name="name" select="@name"/>
           <xsl:if test="not( $outputs/Port[ position() &lt; $pos ][ @name = $name ] )">
             <xsl:copy-of select="."/>
           </xsl:if>
         </xsl:for-each>
       </xsl:with-param>
     </xsl:call-template>
     
     <xsl:text>}}"];
     </xsl:text>
   </xsl:template>

   <xsl:template match="Connection">
     <xsl:variable name="src">
       <xsl:choose>
         <xsl:when test="@src=''">
           <xsl:variable name="xdfName"><xsl:call-template name="createName"><xsl:with-param name="name" select="ancestor::XDF/@id"/></xsl:call-template></xsl:variable>
           <xsl:value-of select="concat(normalize-space($xdfName),'_in')"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:call-template name="createName"><xsl:with-param name="name" select="@src"/></xsl:call-template>
         </xsl:otherwise>
       </xsl:choose>
     </xsl:variable>
     <xsl:variable name="dst">
       <xsl:choose>
         <xsl:when test="@dst=''">
           <xsl:variable name="xdfName"><xsl:call-template name="createName"><xsl:with-param name="name" select="ancestor::XDF/@id"/></xsl:call-template></xsl:variable>
           <xsl:value-of select="concat(normalize-space($xdfName),'_out')"/>
         </xsl:when>
         <xsl:otherwise>
           <xsl:call-template name="createName"><xsl:with-param name="name" select="@dst"/></xsl:call-template>
         </xsl:otherwise>
       </xsl:choose>
     </xsl:variable>
     <xsl:text></xsl:text><xsl:value-of select="concat('struct_',$src)"/><xsl:text>:</xsl:text><xsl:value-of select="@src-port"/><xsl:text> -&gt; </xsl:text><xsl:value-of select="concat('struct_',$dst)"/><xsl:text>:</xsl:text><xsl:value-of select="@dst-port"/>
     <xsl:if test="./Attribute[@name='bufferSize']">
       <xsl:text>[label="</xsl:text>
       <xsl:variable name="value">
         <xsl:choose>
           <xsl:when test="./Attribute[@name='bufferSize']/@value">
             <xsl:value-of select="./Attribute[@name='bufferSize']/@value"/>
           </xsl:when>
           <xsl:otherwise>
             <xsl:value-of select="./Attribute[@name='bufferSize']/Expr[@kind='Literal']/@value"/>
           </xsl:otherwise>
         </xsl:choose>
       </xsl:variable>
       <xsl:value-of select="normalize-space($value)"/>
       <xsl:text>"]</xsl:text>
     </xsl:if>
     <xsl:text>;
     </xsl:text>
   
   </xsl:template>

   
   <xsl:template name="createPorts">
     <xsl:param name="ports" select="_empty_list_"/>
     <xsl:for-each select="$ports/Port">
       <xsl:variable name="this" select="."/>
       <xsl:variable name="name">
         <xsl:call-template name="createName">
           <xsl:with-param name="name" select="@id"/>
         </xsl:call-template>
       </xsl:variable>
       <xsl:text>&lt;</xsl:text><xsl:value-of select="normalize-space($this/@name)"/><xsl:text>&gt; </xsl:text><xsl:value-of select="$this/@name"/>
       <xsl:if test="position() != last()">
         <xsl:text>|</xsl:text>
       </xsl:if>
     </xsl:for-each>
   </xsl:template>

   <xsl:template name="createName">
     <xsl:param name="name" select="_undefined_"/>
     <xsl:value-of select="replace($name,'\$','_')"/>
   </xsl:template>

   <xsl:template name="createHierarchy">
     <xsl:for-each select="Note[@kind='instanceHierarchy']/Note[@kind='hierElement']">
       <xsl:value-of select="@value"/><xsl:text>/</xsl:text>
     </xsl:for-each>
     <xsl:value-of select="./Note[@kind='UID']/@value"/>
   </xsl:template>
   
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
   
</xsl:stylesheet>