<!--
	MergePriorityInequalities.xslt
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

-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xd="http://www.pnp-software.com/XSLTdoc"
  extension-element-prefixes="xsl xd"
  version="1.1">
<xsl:output method="xml"/>

  <xd:doc type="stylesheet">
    <xd:short>Merge all action priority inequalities into a single relationship</xd:short>
    <xd:detail>
      <ul>
        <li>Reads/writes either CALML or XDF</li>
        <li>Relies on AnnotateActionQIDs</li>
        <li>2005-07-25 DBP Created</li>
        <li>TODO: Generalize away from arbitrary total order</li>
      </ul>
    </xd:detail>
    <xd:author>DBP</xd:author>
    <xd:copyright>Xilinx, 2005</xd:copyright>
    <xd:cvsId>$Id: MergePriorityInequalities.xslt 758 2005-07-28 21:42:19Z davep $</xd:cvsId>
  </xd:doc>
  
  <xd:doc>
    <xd:short>Copy each Actor and annotate with a single action priority relationship that
    respects all the priority inequalities.</xd:short>
    <xd:detail>
      Multiple priority inequalities are analyzed to produce a single data structure
      to drive guard evaluation. The transformation currently implements this as
      a list that represents an otherwise arbitrary total order respecting
      the Actor's individual priority inequalities. The form of the list is<br/>
      <code>&lt;Note kind="MergedPriorityInequalities"><br/>
        &lt;Note kind="ActionId" id="<em>nnn</em>"/><br/>
       ... etc ...<br/>
      &lt;/Note></code><br/>
      The list starts with unnamed actions, then actions that do not appear in any inequality.
    </xd:detail>
  </xd:doc>
 
  <xsl:template match="Actor">

    <xsl:copy>
      <!-- Preserve the existing element information -->  
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:copy-of select="*"/>

      <xsl:choose>
        <!-- DBP: added this test because of a situation that occurs with an experimental flow -->
        <xsl:when test="Action[not( @id )]">
          <xsl:message>
            <xsl:text>Skipping MergePriorityInequalities for Actor </xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text> (there are actions without generated IDs)</xsl:text>
          </xsl:message>
        </xsl:when>
        <xsl:otherwise>
          <!-- Create the note containing an ordering of all the priority inequalities -->
          <Note kind="MergedPriorityInequalities">
            
            <!-- First come unnamed actions -->
            <xsl:for-each select="Action[ not( QID ) ]">
              <Note kind="ActionId" id="{@id}"/>
            </xsl:for-each>
            
            <!-- Next named actions that do not appear in a priority statement -->
            <xsl:for-each select="Action[ QID ]">
              <xsl:variable name="id" select="@id"/>
              <xsl:if test="not( ../Priority/QID/Note[@kind='ActionId' and @id=$id] )">
                <Note kind="ActionId" id="{$id}"/>
              </xsl:if>
            </xsl:for-each>  
            
            <!-- Now prioritize the remaining actions -->
            <xsl:call-template name="total-order">
              <xsl:with-param name="in">
                <xsl:for-each select="Priority">
                  <inequality>
                    <xsl:for-each select="QID">
                      <equiv>
                        <xsl:copy-of select="Note[@kind='ActionId']"/>
                      </equiv>
                    </xsl:for-each>
                  </inequality>
                </xsl:for-each>        
              </xsl:with-param>
            </xsl:call-template>
            
          </Note>    
        </xsl:otherwise>
      </xsl:choose>

    </xsl:copy>
 
  </xsl:template>

  <xd:doc>
    <xd:short>Template called recursively to combine multiple
       partial orders into a single total order.</xd:short>
    <xd:detail>The starting partial orders are provided through the parameter "in".
    The growing result is
    passed on recursive applications through the parameter "out", which is initially
    empty. When all "in" groups are empty the transformation is complete and the
    "out" list is copied to the result tree. On successive applications, any elements
    at the heads of individual groups which do not occur later in any group
    are removed from the "in" groups and copied to the "out" list. It is a fatal
    error condition to have a non-empty "in" group when there are no such unconstrained elements.
    (Source CAL has circular priority inequalities).</xd:detail>
    
    <xd:param name="in">Starting list of partial orders. Form is<br/>
    <code>&lt;group><br/>
    &lt;Note kind="ActionId" id="<em>nnn</em>"/><br/>
      ... more action IDs ...<br/>
    &lt;/group><br/>
      ... more groups ..<br/>
    </code><br/>
    </xd:param>
    
    <xd:param name="out">Resultant list of ActionId Notes.</xd:param>
      
  </xd:doc>
  <xsl:template name="total-order">

    <xsl:param name="in"/>
    <xsl:param name="out" select="_default_empty_list_"/>

    <!-- a list (with possible repetition) of all actions that appear in the highest
      priority equivalence group of any priority relationship --> 
    <xsl:variable name="highest-priority">
      <xsl:copy-of select="$in/inequality/equiv[1]/*"/>
    </xsl:variable>
    
    <xsl:choose>
      
      <!-- Recursion ends when there are no more actions in the partial ordering -->
      <xsl:when test="count( $in/inequality/equiv ) = 0">
        <xsl:copy-of select="$out/*"/>
      </xsl:when>

      <xsl:otherwise>
 
        <!-- find all the highest-priority actions that do not appear in any other relationships -->
        <xsl:variable name="unconstrained">
          <xsl:for-each select="$highest-priority/*">
            <xsl:variable name="id" select="@id"/>
            <xsl:variable name="pos" select="position()"/>
          
            <xsl:choose>
              <xsl:when test="$highest-priority/*[ @id=$id and position() &lt; $pos]">
                <!-- this is a repeat - ignore it -->
              </xsl:when>
              <xsl:when test="not( $in/inequality/equiv[ position() > 1 ]/*[ @id=$id ] )">
                <!-- this is unconstrained -->
                <xsl:copy-of select="."/>
              </xsl:when>
            </xsl:choose>
          
          </xsl:for-each>
        </xsl:variable>
                 
        <xsl:if test="count($unconstrained/*) = 0">
          <xsl:message terminate="yes">
            <xsl:text>Fatal error: there are circular priority relationships involving Action QIDs:&#10;</xsl:text>
            <xsl:for-each select="Action">
              <xsl:variable name="id" select="@id"/>
              <xsl:if test="$highest-priority/*[@id=$id]">
                <xsl:text>  </xsl:text>
                <xsl:value-of select="QID/@name"/>
                <xsl:text>&#10;</xsl:text>
              </xsl:if>
            </xsl:for-each>
          </xsl:message>
        </xsl:if>

        <!-- Prune the input sets of actions that have been added to the total order already -->
        <xsl:variable name="whats-left">
          <xsl:for-each select="$in/inequality">
            <inequality>
              <xsl:for-each select="equiv">
                
                <xsl:choose>
              
                  <!-- remove any actions that have just been added to the total order -->
                  <xsl:when test="position() = 1">
                    <xsl:variable name="remainder">
                      <equiv>
                        <xsl:for-each select="*">
                          <xsl:variable name="id" select="@id"/>
                          <xsl:if test="not( $unconstrained/*[ @id = $id ] )">
                            <xsl:copy-of select="."/>
                          </xsl:if>
                        </xsl:for-each>
                      </equiv>
                    </xsl:variable>
                
                    <!-- only keep the top equivalence group if it still contains actions -->
                    <xsl:if test="count( $remainder/equiv/* ) > 0">
                      <xsl:copy-of select="$remainder"/>
                    </xsl:if>
                  </xsl:when>
              
                  <xsl:otherwise>
                    <!-- lower priority equivalence groups are not affected -->
                    <xsl:copy-of select="."/>
                  </xsl:otherwise>
                  
                </xsl:choose>
                
              </xsl:for-each>
            </inequality>
          </xsl:for-each>
        </xsl:variable>
        
        <!-- Move the unconstrained actions to the output list and re-apply template -->
        <xsl:call-template name="total-order">
          <xsl:with-param name="in">
            <xsl:copy-of select="$whats-left/*"/>
          </xsl:with-param>
          <xsl:with-param name="out">
            <xsl:copy-of select="$out/*"/>
            <xsl:copy-of select="$unconstrained/*"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>

    </xsl:choose>
  </xsl:template>

  <xd:doc> Default just copies the input element to the result tree </xd:doc>
  <xsl:template match="*">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:attribute name="{name()}"><xsl:value-of select="."/></xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates/>   
    </xsl:copy>
  </xsl:template>
  
</xsl:stylesheet>