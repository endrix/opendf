<!--
	AddDirectives.xslt
    
BEGINCOPYRIGHT X
	
	Copyright (c) 2007, Xilinx Inc.
	All rights reserved.
	
	Redistribution and use in source and binary forms, 
	with or without modification, are permitted provided 
	that the following conditions are met:
	- Redistributions of source code must retain the above 
	  copyright notice, this list of conditions and the 
	  following disclaimer.
	- Redistributions in binary form must reproduce the 
	  above copyright notice, this list of conditions and 
	  the following disclaimer in the documentation and/or 
	  other materials provided with the distribution.
	- Neither the name of the copyright holder nor the names 
	  of its contributors may be used to endorse or promote 
	  products derived from this software without specific 
	  prior written permission.
	
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
	CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
	INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
	MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
	DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
	SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
	NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
	LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
	HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
	CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
	OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	
ENDCOPYRIGHT

    Add directive info to xhdl.

-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  version="1.1">
  <xsl:output method="xml"/>
  
  <xd:doc type="stylesheet">
    <xd:short>Annotate design document with parameter values from
    both the global and project directives files.
    </xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>2005-07-25 DBP Created</li>
      </ul>
      Both the default directives and project directives files are read in. For
      each element in the design document the directives are filtered according to context, and
      attached to the element as a Note. If there is more than one applicable parameter with
      the same name, only the last one in directive document order is attached, with project
      directives taking precedence over defaults. 
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: AddDirectives.xslt 2432 2007-06-25 18:30:16Z davep $</xd:cvsId>
  </xd:doc>
  
  <xd:doc>
    <xd:short>Project directives file name.</xd:short>
    <xd:detail>
      The project directives file is an XML document that contains parameter values to be
      copied into the design document. The top-level element must be a <code>&lt;directives></code>
      element, which has the form:<br/>
      <code>&lt;directives {context="<em>xyz</em>" { name="<em>abc</em>" } }><br/>
      </code>... directive or parameter elements ...<br/><code>
        &lt;/directives><br/>
      </code>
      The parameter element has two forms, which specify either a string value or an XML fragment:<br/>
      <code>&lt;parameter name="<em>xyz</em>" value="<em>nnn</em>">, </code> or ... <code><br/>
        &lt;parameter name="<em>xyz</em>"><br/>
        </code>... any valid XML fragment ...<code><br/>
        &lt;/parameter><br/></code>
        The project directives file, "directives.xml", must be in the same directory as the
        design document.
    </xd:detail>
  </xd:doc>
  <xsl:param name="project-directives-uri" select="'directives.xml'"/>

  <xd:doc>
    <xd:short>default directives file name.</xd:short>
    <xd:detail>
      The format of the contents is the same as for the project directives. This file is
      read first, so that project directives of the same name will override. This file 
      resides in the same directory as the transformation.
    </xd:detail>
  </xd:doc>
  <xsl:param name="def-directives-uri" select="'net/sf/opendf/cal/transforms/xlim/DefaultDirectives.xml'"/>

  <xd:doc>
    <xd:short>Filter the directives tree to include only those directives that
      are applicable in the current context, then add applicable parameter values.</xd:short>
    <xd:detail>
      After the directives are filtered to the current context, any parameter values are
      added to the element. The filtering process involves copying the following directive
      elements to the filtered result:
      <ul>
        <li>The <bold>contents</bold> of any <code>&lt;directives></code> elements that have no
          context attribute.</li>
        <li>The <bold>contents</bold> of any <code>&lt;directives></code> elements that have a
          context attribute whose value matches the name of the current design document
          element, provided:
          <ul>
            <li> the directives element has no name attribute, or</li>
            <li> the directives element's name attribute matches the name attribute of the
              current design document element.</li>
          </ul>
        </li>
        <li>Any <code>&lt;directives></code> elements that have a context attribute whose
          value does not match the name of the current design document element are copied
          intact.</li>
      </ul>
      The effect of these rules is that any applicable <code>&lt;parameter></code> elements
      will appear at the root of the filtered result and can be added as Note elements.
      Any <code>&lt;directives></code> elements in the filtered result contain directives that
      may become applicable at a lower level in the design document tree.
      <p>Directives are added to the result document as Notes:<br/>
      <code>&lt;Note kind="Directive" name="<em>abc</em>" { value="<em>xyz</em>" } ></code><br/>
      ... XML fragment from directives file ...<br/>
      <code>&lt;/Note></code><br/>
      </p>
    </xd:detail>
    <xd:param name="directives">
      <xd:short>Contains the set of directives filtered to the parent context.</xd:short>
      <xd:detail>This is initialized at the root not to be the contents of the various
        directives files.</xd:detail>
    </xd:param>
  </xd:doc>        
  <xsl:template match="*">
    <xsl:param  name="directives"/>

    <xsl:variable name="element-name" select="name()"/>
    <xsl:variable name="name-attribute" select="@name"/>
    <xsl:variable name="dir-list">
      <xsl:choose>
        <xsl:when test=" not( parent::* )">
          <!-- Root node must initialize the list of directives -->
          <xsl:copy-of select="document($def-directives-uri)/directives/*"/>
          <!-- Only include the user directives file if it exists. Note that we are
               selecting relative to the URI resolver paths and not relative to '.'
               Our frontend automatically adds '.' to the classpath and our transformations
               load URIs relative to the classpath, so this should be the same.  This
               directives mechanism is deprecated.  -->
          <xsl:if test="doc-available($project-directives-uri)">
            <xsl:message>Using user directives file <xsl:value-of select="$project-directives-uri"/></xsl:message>
            <xsl:copy-of select="document($project-directives-uri,.)/directives/*"/>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$directives/*"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <!-- Filter the directives -->        
    <xsl:variable name="new-directives">
      <xsl:copy-of select="$dir-list/directives[ not(@context) ]/*"/>
      <xsl:copy-of select="$dir-list/directives[ @context and @context=$element-name
        and ( not(@name) or @name=$name-attribute ) ]/*"/>
      <xsl:copy-of select="$dir-list/directives[ @context and @context!=$element-name ]"/>
    </xsl:variable>
  
    <!-- Preserve the existing element information -->  
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>

      <!-- Descend into all elements, keeping a stack of contexts -->
      <xsl:apply-templates>
        <xsl:with-param name="directives">
          <xsl:copy-of select="$new-directives/*"/>
        </xsl:with-param>
      </xsl:apply-templates>

      <!-- If there are multiple settings for a parameter, take the last one -->
      <xsl:for-each select="$new-directives/parameter">
        <xsl:variable name="name" select="@name"/>
        <xsl:variable name="pos" select="position()"/>
        <xsl:if test="count($new-directives/parameter[@name=$name and position()>$pos])=0">
                 
          <Note kind="Directive" name="{@name}">     
            <xsl:call-template name="make-note-body">
              <xsl:with-param name="parameter" select="."/>
            </xsl:call-template>
          </Note>
          
        </xsl:if>
      </xsl:for-each>
        
    </xsl:copy>

  </xsl:template>

  <xd:doc>
    <xd:short>Construct a Note body from a directive parameter element</xd:short>
    <xd:detail>
      There are three forms of <code>&lt;parameter></code> element in the directives
      file, which result in different Notes in the design document:
      <ul>
        <li><code>&lt;parameter name="<em>abc</em>" value="<em>nnn</em>"/></code><br/>
          This form is translated into a Note containing an Expr literal:<br/>
          <code>&lt;Note kind="Directive" name="<em>abc</em>"><br/>
            &lt;Expr kind="Literal" literal-kind="Integer|String" value="<em>nnn</em>"/><br/>
            &lt;/Note></code>
        </li>
        <li><code>&lt;parameter name="<em>abc</em>"></code>... parameter elements only ...<code>
          &lt;/parameter></code><br/>
          This form is translated into a Note containing a list Expr whose contents are determined
          by the recursive application of this template:<br/>
          <code>&lt;Note kind="Directive" name="<em>abc</em>"><br/>
            &lt;Expr kind="List"><br/>
            </code>.. result of expanding child parameter elements ...<br/><code>
            &lt;/Expr><br/>
            &lt;/Note></code>
        </li>
        <li><code>&lt;parameter name="<em>abc</em>"></code>... no parameter elements ...<code>
          &lt;/parameter></code><br/>
            This form is translated into a Note containing a copy of the child elements:<br/>
          <code>&lt;Note kind="Directive" name="<em>abc</em>"><br/>
            </code>.. copy of child elements ...<br/><code>
              &lt;/Note></code>
        </li>
      </ul>
    </xd:detail>
    <xd:param name="parameter">The parameter element from the directives file</xd:param>
  </xd:doc>
  <xsl:template name="make-note-body">
    <xsl:param name="parameter"/>

    <!-- Note: the three 'if' cases below should be mutually exclusive in
         a well-formed directives file -->
         
    <xsl:if test="$parameter/@value">
      <!-- single attribute form: make a literal Expr -->
        
      <!-- look for an integer value -->
      <xsl:variable name="int">
        <xsl:choose>
          <xsl:when test="string-length( $parameter/@value ) > 1">
            <!-- possibly a multi-digit integer -->
            <xsl:variable name="first" select="substring( $parameter/@value, 1, 1)"/>
            <xsl:variable name="rest"  select="substring( $parameter/@value, 2)"/>
            <xsl:if test="string-length( translate( $rest, '0123456789', '') ) = 0 and
                          string-length( translate( $first, '+-0123456789', '') ) = 0">
              <xsl:value-of select="$parameter/@value"/>
            </xsl:if>
          </xsl:when>
          <xsl:otherwise>
            <!-- possibly a single-digit integer -->
            <xsl:if test="string-length( translate( $parameter/@value, '0123456789', '')) = 0">
              <xsl:value-of select="$parameter/@value"/>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:variable name="kind">
        <xsl:choose>
          <xsl:when test="string-length( $int ) > 0">Integer</xsl:when>
          <xsl:otherwise>String</xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
    
      <Expr kind="Literal" literal-kind="{$kind}" value="{$parameter/@value}"/>
    </xsl:if>
    
    <xsl:if test="$parameter/parameter">
      <!-- nested parameter form: make a list Expr with all the child parameters
           as elements of the list -->
      <Expr kind="List">
        <xsl:for-each select="$parameter/parameter">
          <xsl:call-template name="make-note-body">
            <xsl:with-param name="parameter" select="."/>
          </xsl:call-template>
        </xsl:for-each>
      </Expr>
      
    </xsl:if>
    
    <xsl:if test="$parameter/*[ not( parameter ) ]">
      <!-- enclosed element form: just copy the elements to the note body -->
      <xsl:copy-of select="$parameter/*[ not( parameter ) ]"/>
    </xsl:if>
    
  </xsl:template>
 
</xsl:stylesheet>
