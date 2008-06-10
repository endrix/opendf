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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:rvc="urn:mpeg:2006:01-RVC-NS" xmlns:math="http://exslt.org/math" xmlns:xsd="http://www.w3.org/2001/XMLSchema" version="2.0">


  <!-- returns the unprefixed simple name -->
  <xsl:function name="rvc:simpleLocalName" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:value-of select="rvc:localName(rvc:simpleName($name))"/>
  </xsl:function>



  <!-- returns the unprefixed simple name -->
  <xsl:function name="rvc:simpleValidPrefixedName" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:param name="defaultPrefix" as="xsd:string?"/>
    <xsl:value-of select="rvc:validPrefixedName(rvc:simpleName($name),$defaultPrefix)"/>
  </xsl:function>

  <!-- returns the contents after the last period -->

  <xsl:function name="rvc:simpleName" as="xsd:string?">

    <xsl:param name="name" as="xsd:string?"/>
    <xsl:value-of>

    <xsl:analyze-string select="$name" regex="\w+\.">

      <xsl:non-matching-substring>

        <xsl:value-of select="."/>

      </xsl:non-matching-substring>

    </xsl:analyze-string>
    </xsl:value-of>

  </xsl:function>


  <!-- replace : with _ -->
  <xsl:function name="rvc:validName" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:value-of select="replace($name,':','_')"/>
  </xsl:function>
  
  <xsl:function name="rvc:validPrefixedName" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:param name="defaultPrefix" as="xsd:string"/>
    <xsl:variable name="prefixedName" select="if (contains($name,':')) then $name else concat($defaultPrefix,':',$name)"/>
    <xsl:value-of select="rvc:validName($prefixedName)"/>
  </xsl:function>
  <!-- replace . with _ -->
  <xsl:function name="rvc:underscore" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:value-of select="replace($name,'[:\.]','_')"/>
  </xsl:function>
  
  <!-- replace . with _ and uppercase -->
  <xsl:function name="rvc:constant" as="xsd:string?">
    <xsl:param name="name" as="xsd:string?"/>
    <xsl:value-of select="upper-case(rvc:underscore($name))"/>
  </xsl:function>
    

  <!-- return the name attribute, the local-name of the ref attribute, or the item text for all elements in the stack. -->

  <xsl:function name="rvc:itemName" as="xsd:string?">

    <xsl:param name="stack" as="item()*"/>

    <xsl:variable name="item" select="$stack[1]"/>

    <xsl:value-of>

      <xsl:if test="$stack[2]">

        <xsl:value-of select="rvc:itemName(subsequence($stack,2))"/>

        <xsl:text>.</xsl:text>

      </xsl:if>

      <xsl:choose>

        <xsl:when test="$item instance of xsd:string or 
          $item[text() or self::text()]">

          <xsl:value-of select="$item"/>

        </xsl:when>

        <xsl:when test="$item/@name">

          <!-- detect if the prefix is necessary for disambiguation. If  not, just use local name-->

          <xsl:choose>

            <xsl:when test="$item/following-sibling::*[rvc:localName(@name)=rvc:localName($item/@name)] or
              $item/preceding-sibling::*[rvc:localName(@name)=rvc:localName($item/@name)] ">

              <xsl:value-of select="rvc:validName($item/@name)"/>

            </xsl:when>

            <xsl:otherwise>

              <xsl:value-of select="rvc:localName($item/@name)"/>

            </xsl:otherwise>

          </xsl:choose>

        </xsl:when>

      </xsl:choose>

    </xsl:value-of>

  </xsl:function>



  <!-- retrieve a localname from a lexical qname -->

  <xsl:function name="rvc:localName" as="xsd:string?">

    <xsl:param name="cName" as="xsd:string?"/>

    <xsl:value-of select="if (contains($cName,':')) then substring-after($cName,':') else $cName"/>

  </xsl:function>



  <!-- opposite of string-join -->

  <xsl:function name="rvc:split" as="xsd:string*">

    <xsl:param name="inputString" as="xsd:string?"/>

    <xsl:param name="delimiter" as="xsd:string"/>

    <xsl:sequence

      select="if (contains($inputString,$delimiter))
      then (substring-before($inputString,$delimiter),
      rvc:split(substring-after($inputString,$delimiter),
      $delimiter))
      else $inputString"/>



  </xsl:function>



  <!-- locate a prefix for the namespace, either in context or context2.
    If none is found, create a random one. - should probably just increment, but that's difficult to do. -->

  <xsl:function name="rvc:findPrefix">

    <xsl:param name="namespace"/>

    <xsl:param name="context"/>

    <xsl:param name="context2"/>

    <xsl:variable name="prefix" select="($context//namespace::*[.=$namespace]/name()[string-length() &gt; 0])[1]"/>

    <xsl:variable name="prefix2" select="if ($prefix) then $prefix else ($context2//namespace::*[.=$namespace]/name()[string-length() &gt; 0])[1]"/>

    <xsl:value-of select="if ($prefix2) then $prefix2 else concat('a',round(math:random()*100))"/>

  </xsl:function>


  <!-- ============================================================ -->
  <!-- Function: nextReadElement                                                                                           -->
  <!-- Determines the next element that is to be read, based on whether the               -->
  <!-- current element is inside a choice, and whether this is a read-again element.  -->
  <!--                                                                                                                                               -->
  <!-- The algorithm to determine the next element is as follows:                                    -->
  <!--                                                                                                                                               -->
  <!--  if current==choiceOption, next = read child    
          else if inside a choice, 
                      if following-sibling read exists next = following-sibling read
                      else next = the read element following the choice
          else next = following read -->  
  <!--                                                                                                                                               -->
  <!--                                                                                                                                               -->
  <!-- ============================================================ -->
  <xsl:function name="rvc:nextReadElement" as="element()?">
    <xsl:param name="currentElement" as="element()"/>
    <xsl:choose>
      <xsl:when test="$currentElement[self::choiceOption]">
        <xsl:sequence select="$currentElement/read[1]"/>
      </xsl:when>
      <xsl:when test="$currentElement[parent::choiceOption]">
         <xsl:choose>
          <xsl:when test="$currentElement/following-sibling::read">
            <xsl:sequence select="$currentElement/following-sibling::read[1]"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="$currentElement/ancestor::read[@choice='true']/following::read[1]"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="$currentElement/following::read[1]"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!-- ======================================================== -->

  <!-- Function: Hex2Dec(<value>) => Decimal value              -->

  <!-- Parameters:-                                             -->

  <!--   <value>  - the hex string to be converted to decimal   -->

  <!--              (case of hex string is unimportant)         -->

  <xsl:function name="rvc:hexToDec" as="xsd:integer">

    <xsl:param name="value" as="xsd:string"/>

    <xsl:value-of select="rvc:hex2DecInt($value,1,0)"/>

  </xsl:function>



  <xsl:function name="rvc:hex2DecInt">

    <xsl:param name="value" as="xsd:string"/>

    <xsl:param name="hex-power"/>

    <xsl:param name="accum"/>

    <!-- isolate last hex digit (and convert it to upper case) -->

    <xsl:variable name="hex-digit" select="translate(substring($value,string-length($value),1),'abcdef','ABCDEF')"/>

    <!-- check that hex digit is valid -->

    <xsl:choose>

      <xsl:when test="not(contains('0123456789ABCDEF',$hex-digit))">

        <!-- not a hex digit! -->

        <xsl:text>NaN</xsl:text>

      </xsl:when>

      <xsl:when test="string-length($hex-digit) = 0">

        <!-- unexpected end of hex string -->

        <xsl:text>0</xsl:text>

      </xsl:when>

      <xsl:otherwise>

        <!-- OK so far -->

        <xsl:variable name="remainder" select="substring($value,1,string-length($value)-1)"/>

        <xsl:variable name="this-digit-value" select="string-length(substring-before('0123456789ABCDEF',$hex-digit)) * $hex-power"/>

        <!-- determine whether this is the end of the hex string -->

        <xsl:choose>

          <xsl:when test="string-length($remainder) = 0">

            <!-- end - output final result -->

            <xsl:value-of select="$accum + $this-digit-value"/>

          </xsl:when>

          <xsl:otherwise>

            <!-- recurse to self for next digit -->

            <xsl:value-of select="rvc:hex2DecInt($remainder,$hex-power * 16,$accum + $this-digit-value)"/>

          </xsl:otherwise>

        </xsl:choose>

      </xsl:otherwise>

    </xsl:choose>

  </xsl:function>



  <xsl:function name="rvc:hexByteLength" as="xsd:integer">

    <xsl:param name="hexString" as="xsd:string?"/>

    <xsl:value-of select="string-length($hexString) * 4"/>

  </xsl:function>
  
  

</xsl:stylesheet>

