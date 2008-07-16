<?xml version="1.0" encoding="UTF-8"?>
<!--*
  * Copyright(c)2008, Samuel Keller, Christophe Lucarz, Joseph Thomas-Kerr 
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions are met:
  *     * Redistributions of source code must retain the above copyright
  *       notice, this list of conditions and the following disclaimer.
  *     * Redistributions in binary form must reproduce the above copyright
  *       notice, this list of conditions and the following disclaimer in the
  *       documentation and/or other materials provided with the distribution.
  *     * Neither the name of the EPFL, University of Wollongong nor the
  *       names of its contributors may be used to endorse or promote products
  *       derived from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr ``AS IS'' AND ANY 
  * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  * DISCLAIMED. IN NO EVENT SHALL  Samuel Keller, Christophe Lucarz, 
  * Joseph Thomas-Kerr BE LIABLE FOR ANY
  * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  *-->

<!DOCTYPE stylesheet SYSTEM "entities.dtd">

<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

  xmlns:rvc="urn:mpeg:2006:01-RVC-NS" 

  xmlns:math="http://exslt.org/math"

  xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
  
  xmlns:bs2="urn:mpeg:mpeg21:2003:01-DIA-BSDL2-NS"

  version="2.0">

  

  <xsl:template name="lengthConstants">

    <xsl:variable name="context" select="."/>
    <!-- create LENGTH constants for simple types -->

    <xsl:apply-templates select="/xsd:schema/xsd:simpleType" mode="length"/>

    

    <!-- create LENGTH constants for referenced XSD types -->

    <xsl:for-each select="distinct-values(/.//xsd:element/@type)">

      <xsl:variable name="typeQName" select="resolve-QName(.,$context)"/>
      <xsl:if test="namespace-uri-from-QName($typeQName)=&xsdNS;">
        <xsl:call-template name="statement">

          <xsl:with-param name="expressions">

            <xsl:call-template name="variableDeclaration">

              <xsl:with-param name="name" select="concat(rvc:constant(.),&lengthSuffix;)"/>

              <xsl:with-param name="type">int</xsl:with-param>

              <xsl:with-param name="initialValue">

                <xsl:call-template name="xsdTypeLength">
                  <xsl:with-param name="typeQName" select="$typeQName"/>
                </xsl:call-template>

              </xsl:with-param>

            </xsl:call-template>

          </xsl:with-param>

        </xsl:call-template>

      </xsl:if>
    </xsl:for-each>
    
    <xsl:call-template name="variableDeclaration">
      
      <xsl:with-param name="name">M4V_VLC_LENGTH</xsl:with-param>
      
      <xsl:with-param name="type">int</xsl:with-param>
      
      <xsl:with-param name="initialValue">1</xsl:with-param>
      
    </xsl:call-template>

  </xsl:template>

  

  <!-- create LENGTH constants for simple types -->

  <xsl:template match="xsd:simpleType" mode="length">

    <xsl:call-template name="statement">

      <xsl:with-param name="expressions">

        <xsl:call-template name="variableDeclaration">

          <xsl:with-param name="name" select="concat(rvc:constant(@name),&lengthSuffix;)"/>

          <xsl:with-param name="type">int</xsl:with-param>

          <xsl:with-param name="initialValue">

            <xsl:apply-templates mode="calculateLength"/>

          </xsl:with-param>

        </xsl:call-template>

      </xsl:with-param>

    </xsl:call-template>

  </xsl:template>

  

  <!-- only need to create Length constants for anonymous inner types. -->

  <xsl:template match="xsd:simpleType[xsd:list]" mode="length">

    <xsl:apply-templates select="xsd:list/*" mode="length"/>

  </xsl:template>

  

  <xsl:template match="xsd:simpleType[xsd:union]" mode="length">

    <xsl:apply-templates select="xsd:union/*" mode="length"/>

  </xsl:template>

  

  <!-- TODO: test this -->

  <xsl:template match="xsd:simpleType" mode="calculateLength">

    <xsl:apply-templates mode="calculateLength"/>

  </xsl:template>
  

  <xsl:template match="xsd:simpleType[xsd:list]" mode="calculateLength">

    <xsl:apply-templates select="xsd:list/*" mode="calculateLength"/>

  </xsl:template>

  

  <xsl:template match="xsd:simpleType[xsd:union]" mode="calculateLength">

    <xsl:apply-templates select="xsd:union/*" mode="calculateLength"/>

  </xsl:template>

  

  <xsl:template match="xsd:restriction" mode="calculateLength">

    <xsl:variable name="baseBitCount">

      <xsl:apply-templates select="@base" mode="calculateLength"/>

    </xsl:variable>

    <xsl:variable name="aBaseBitCount" select="if ($baseBitCount='') then 9999 else $baseBitCount"/>

    <xsl:variable name="localBitCount">

      <xsl:apply-templates select="xsd:simpleType" mode="calculateLength"/>

    </xsl:variable>

    <xsl:variable name="aLocalBitCount" select="if ($localBitCount='') then 9999 else $localBitCount"/>

    <xsl:variable name="meBitCount">

      <xsl:apply-templates select="xsd:maxExclusive" mode="calculateLength"/>

    </xsl:variable>

    <xsl:variable name="aMEBitCount" select="if ($meBitCount='') then 9999 else $meBitCount"/>
    
    <xsl:variable name="aBaseHexCount" >
      <xsl:choose>
        <xsl:when test="@base='xsd:hexBinary'">
          <xsl:apply-templates select="xsd:length" mode="calculateLength"/>
        </xsl:when>
        <xsl:otherwise>9999</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:variable name="aStartCode">
      
      <xsl:variable name="startCode">
        <xsl:apply-templates select="xsd:annotation" mode="startCode"/>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="string-length($startCode) &gt; 0">
          <xsl:value-of select="string-length($startCode)"/>
        </xsl:when>
        <xsl:otherwise>9999</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:value-of select="math:min(($aBaseBitCount,$aLocalBitCount,$aMEBitCount,$aBaseHexCount,$aBaseBitCount,$aStartCode))"/>

  </xsl:template>

  

  <xsl:template match="xsd:maxExclusive" mode="calculateLength">

    <xsl:value-of select="round(math:log(@value) div math:log(2))"/><!-- should really be ceil but .0000004 is really 0 -->

  </xsl:template>

  

  <xsl:template match="xsd:length" mode="calculateLength">

    <xsl:value-of select="@value*8"/>

  </xsl:template>

  

  <xsl:template match="@base" mode="calculateLength">

    <xsl:apply-templates select="key('types',resolve-QName(.,..))" mode="calculateLength"/><!-- FIXME - this won't work... -->

  </xsl:template>

  

  <xsl:template match="@base[namespace-uri-from-QName(resolve-QName(.,..))=&xsdNS;]" mode="calculateLength">

    <xsl:call-template name="xsdTypeLength">
      <xsl:with-param name="typeQName" select="resolve-QName(.,..)"/>
    </xsl:call-template>

  </xsl:template>

  

  <xsl:template name="xsdTypeLength">
    <xsl:param name="typeQName" />

    <xsl:choose>

      <xsl:when test="$xsdLong=$typeQName">64</xsl:when>

      <xsl:when test="$xsdInt=$typeQName">32</xsl:when>

      <xsl:when test="$xsdShort=$typeQName">16</xsl:when>

      <xsl:when test="$xsdByte=$typeQName">8</xsl:when>

      <xsl:when test="$xsdUnsignedLong=$typeQName">64</xsl:when>

      <xsl:when test="$xsdUnsignedInt=$typeQName">32</xsl:when>

      <xsl:when test="$xsdUnsignedShort=$typeQName">16</xsl:when>

      <xsl:when test="$xsdUnsignedByte=$typeQName">8</xsl:when>

    </xsl:choose>

  </xsl:template>
  
  <xsl:template match="xsd:simpleType" mode="notFixLength">
    <xsl:value-of select="xsd:restriction/xsd:annotation/xsd:appinfo/bs2:bitLength/@value"/>
  </xsl:template>
  
  <xsl:template match="*" mode="notFixLength" priority="-1000"/>
  
  
  <xsl:template match="xsd:simpleType" mode="startCode">
    <xsl:value-of select="xsd:restriction/xsd:annotation/xsd:appinfo/bs2:startCode/@value"/>
  </xsl:template>
  
  <xsl:template match="xsd:restriction" mode="startCode">
    <xsl:value-of select="xsd:annotation/xsd:appinfo/bs2:startCode/@value"/>
  </xsl:template>
  
  <xsl:template match="xsd:annotation" mode="startCode">
    <xsl:value-of select="xsd:appinfo/bs2:startCode/@value"/>
  </xsl:template>
  
  <xsl:template match="*" mode="startCode" priority="-1000"/>
  

  <xsl:variable name="xsdLong" select="resolve-QName('xsd:long',/*[1])"/>

  <xsl:variable name="xsdInt" select="resolve-QName('xsd:int',/*[1])"/>

  <xsl:variable name="xsdShort" select="resolve-QName('xsd:short',/*[1])"/>

  <xsl:variable name="xsdByte" select="resolve-QName('xsd:byte',/*[1])"/>

  <xsl:variable name="xsdUnsignedLong" select="resolve-QName('xsd:unsignedLong',/*[1])"/>

  <xsl:variable name="xsdUnsignedInt" select="resolve-QName('xsd:unsignedInt',/*[1])"/>

  <xsl:variable name="xsdUnsignedShort" select="resolve-QName('xsd:unsignedShort',/*[1])"/>

  <xsl:variable name="xsdUnsignedByte" select="resolve-QName('xsd:unsignedByte',/*[1])"/>

</xsl:stylesheet>

