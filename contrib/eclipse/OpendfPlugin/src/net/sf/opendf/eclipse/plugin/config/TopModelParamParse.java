/* 
BEGINCOPYRIGHT X
  
  Copyright (c) 2008, Xilinx Inc.
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
 */
package net.sf.opendf.eclipse.plugin.config;

import static net.sf.opendf.util.xml.Util.xpathEvalElements;

import java.util.ArrayList;
import java.util.List;

import net.sf.opendf.cal.util.CalWriter;
import net.sf.opendf.cli.Util;
import net.sf.opendf.util.Loading;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TopModelParamParse {

	public static List<ModelParameter> parseModel(String topModel,
			String[] modelPath) throws ModelAnalysisException {
		/* StreamLocator[] sl = */Util.initializeLocators(modelPath,
				TopModelParamParse.class.getClassLoader());

		Node document = Loading.loadActorSource(topModel);
		if (document == null) {
			throw new ModelAnalysisException("Model '" + topModel
					+ "' could not be loaded");
		}

		java.util.List<Element> parameters;
		// java.util.List<Element> inputs;
		// java.util.List<Element> outputs;

		if (xpathEvalElements("//Note[@kind='Report'][@severity='Error']",
				document).size() > 0) {
			throw new ModelAnalysisException("Model contains errors");
		}

		parameters = xpathEvalElements(
				"(Actor|Network)/Decl[@kind='Parameter']|XDF/Parameter",
				document);
		/* inputs = */xpathEvalElements(
				"(XDF|Actor|Network)/Port[@kind='Input']", document);
		/* outputs = */xpathEvalElements(
				"(XDF|Actor|Network)/Port[@kind='Output']", document);

		ArrayList<ModelParameter> modelParams = new ArrayList<ModelParameter>();
		for (Element e : parameters) {
			NodeList types = e.getElementsByTagName("Type");
			String type = types.getLength() > 0 ? CalWriter.CalmlToString(types
					.item(0)) : "";
			String name = e.getAttribute("name");
			NodeList values = e.getElementsByTagName("Expr");
			String value = values.getLength() > 0 ? CalWriter
					.CalmlToString(values.item(0)) : "";

			modelParams.add(new ModelParameter(name, type, value));
		}

		return modelParams;
	}

	public static class ModelParameter {
		private String name;
		private String type;
		private String defaultValue;

		public ModelParameter(String nam, String typ, String val) {
			this.name = nam;
			this.type = typ;
			this.defaultValue = val;
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
		}

		public String getValue() {
			return this.defaultValue;
		}
	}

	public static class ModelAnalysisException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ModelAnalysisException(String msg) {
			super(msg);
		}

	}
}
