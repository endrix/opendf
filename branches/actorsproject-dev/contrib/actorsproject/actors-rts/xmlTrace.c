/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Carl von Platen (carl.von.platen@ericsson.com)
 * All rights reserved.
 *
 * License terms:
 *
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 *     * Redistributions of source code must retain the above 
 *       copyright notice, this list of conditions and the 
 *       following disclaimer.
 *     * Redistributions in binary form must reproduce the 
 *       above copyright notice, this list of conditions and 
 *       the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the names 
 *       of its contributors may be used to endorse or promote 
 *       products derived from this software without specific 
 *       prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "xmlTrace.h"

FILE *xmlCreateTrace(const char *fileName) {
  FILE *f=fopen(fileName, "w");
  if (f!=0) {
    fprintf(f, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    fprintf(f, "<execution-trace>\n");
  }
  return f;
}

void xmlCloseTrace(FILE *f) {
  fprintf(f, "</execution-trace>\n");
  fclose(f);
}

void xmlDeclareNetwork(FILE *f,
		       const char *networkName,
		       AbstractActorInstance *actors[],
		       int numActors) {
  int i;
  int firstInput=0;

  fprintf(f, "<network name=\"%s\">\n", networkName);

  for (i=0; i<numActors; ++i) {
    const AbstractActorInstance *instance=actors[i];
    const ActorClass            *actorClass=instance->actor;
    int                          numInputs=actorClass->numInputPorts;
    const ActorPort             *inputs=instance->inputPort;
    int                          numOutputs=actorClass->numOutputPorts;
    const ActorPort             *outputs=instance->outputPort;
    int                          numActions=actorClass->numActions;
    const ActionDescription     *actions=actorClass->actionDescriptions;
    int                          firstAction=instance->firstActionIndex;
    int j;

    fprintf(f, "  <actor id=\"%d\" class=\"%s\">\n",
	    instance->aid, actorClass->name);

    // <input>
    // id is a running index 0,1,2... (unique among input ports)
    // source is the identity of the source (output port)
    // TODO: port names not known here, would be nice...
    for (j=0; j<numInputs; ++j) {
      fprintf(f, "    <input id=\"%d\" source=\"%d\"/>\n", 
	      firstInput+j, inputs[j].cid);
    }

    // <output>
    // id is unique among output ports
    for (j=0; j<numOutputs; ++j) {
      fprintf(f, "    <output id=\"%d\"/>\n", outputs[j].cid);
    }

    // <action>
    // id is unique among all actions of all actor instances
    // name is not necessarily unique (not even within an actor)
    for (j=0; j<numActions; ++j) {
      int p;

      fprintf(f, "    <action id=\"%d\" name=\"%s\">\n",
	      firstAction+j, actions[j].name);

      // <consumes>
      // count=number of tokens consumed
      // port=reference to input (id)
      for (p=0; p<numInputs; ++p) {
	int cns=actions->consumption[p];
	if (cns)
	  fprintf(f, "      <consumes count=\"%d\" port=\"%d\"/>\n",
		  cns, firstInput+p);
      }

      // <produces>
      // count=number of tokens consumed
      // port=reference to output (id)
      for (p=0; p<numOutputs; ++p) {
	int prd=actions->production[p];
	if (prd)
	  fprintf(f, "      <produces count=\"%d\" port=\"%d\"/>\n",
		  prd, outputs[p].cid);
      }

      fprintf(f, "    </action>\n");
    }

    fprintf(f, "  </actor>\n");
    firstInput+=numInputs;
  }
  fprintf(f, "</network>\n");
}

void xmlTraceAction(FILE *f, int actionIndex) {
  fprintf(f, "<trace action=\"%d\"/>\n", actionIndex);
}
