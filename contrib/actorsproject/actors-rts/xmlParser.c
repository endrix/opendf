/* 
 * Copyright (c) Ericsson AB, 2009
 * Author: Charles Chen Xu (charles.chen.xu@ericsson.com)
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

#include <stdio.h>
#include <stdlib.h>
#include <libxml/parser.h>

#include "xmlParser.h"

#define MAX_ACTORS                          256
#define MAX_CONNECTS                        1024

#define CONFIGURATION                       (const xmlChar*)"Configuration"
#define CONNECTION                          (const xmlChar*)"Connection"
#define DST                                 (const xmlChar*)"dst"
#define DST_PORT                            (const xmlChar*)"dst-port"
#define SRC                                 (const xmlChar*)"src"
#define SRC_PORT                            (const xmlChar*)"src-port"
#define FIFO_SIZE                           (const xmlChar*)"size"
#define INSTANCE                            (const xmlChar*)"Instance"
#define INSTANCE_NAME                       (const xmlChar*)"name"
#define PARTITION_ID                        (const xmlChar*)"id"
#define SCHEDULING                          (const xmlChar*)"Scheduling"
#define SCHEDULING_TYPE                     (const xmlChar*)"type"

void parseParttioning(xmlNode *node);
void parseScheduling(xmlNode *node);
void parsePartition(xmlNode *node);
void parseConnection(xmlNode *cur_node);

typedef void (*HANDLER)(xmlNode *cur_node);

typedef struct _tagID{
  char *name;
  HANDLER handler;
} TagID;

TagID configTag[] ={
{"Partitioning", parseParttioning},
{"Scheduling",   NULL},
};

TagID partitioningTag[] ={
{"Partition",    parsePartition},
{"Connection",   parseConnection},
};

AffinityID instanceAfinity[MAX_ACTORS];
ConnectID connects[MAX_CONNECTS];
ScheduleID schedule;

static int _numInstances;
static int _numConnects;

void printout()
{
  int i;

  printf("Schedule type: %s\n",schedule.type);
  printf("partition InstanceName\n");
  for(i=0; i<_numInstances; i++)
    printf("%d  %s\n",instanceAfinity[i].affinity,
                      instanceAfinity[i].name);
  printf("src src_port dst dst_port size\n");
  for(i=0; i<_numConnects; i++)
    printf("%s %s %s %s %d\n",connects[i].src,
                              connects[i].src_port,
                              connects[i].dst,
                              connects[i].dst_port,
                              connects[i].size);
}

void parseConnection(xmlNode *cur_node)
{
  char *size;

  if(!xmlStrcmp(cur_node->name, CONNECTION));
  {
    connects[_numConnects].dst      = (char*)xmlGetProp(cur_node,DST);
    connects[_numConnects].dst_port = (char*)xmlGetProp(cur_node,DST_PORT);
    connects[_numConnects].src      = (char*)xmlGetProp(cur_node,SRC);
    connects[_numConnects].src_port = (char*)xmlGetProp(cur_node,SRC_PORT);
    size = (char*)xmlGetProp(cur_node,FIFO_SIZE);
    if(size)
      connects[_numConnects].size = atoi(size);

    if(connects[_numConnects].dst &&
       connects[_numConnects].dst_port &&
       size)
      _numConnects++;

    xmlFree(size);
  }
}

void parsePartition(xmlNode *node)
{
  xmlNode *child_node;
  char *id;

  id=(char*)xmlGetProp(node,PARTITION_ID);

  for(child_node = node->children; child_node != NULL; child_node = child_node->next)
  {
    if ( child_node->type == XML_ELEMENT_NODE  &&
         !xmlStrcmp(child_node->name, INSTANCE ))
    {
      instanceAfinity[_numInstances].name = (char*)xmlGetProp(child_node,INSTANCE_NAME);
      instanceAfinity[_numInstances].affinity = atoi(id);
      _numInstances++;
    }
  }
  xmlFree(id);
}

void parseParttioning(xmlNode *node)
{
  xmlNode *cur_node;
  for(cur_node = node->children; cur_node != NULL; cur_node = cur_node->next)
  {
    if(cur_node->type == XML_ELEMENT_NODE)
    {
      int i,size=sizeof(partitioningTag)/sizeof(TagID);
      TagID *tag=&partitioningTag[0];
      for(i=0; i<size; i++,tag++)
      {
        if(!xmlStrcmp(cur_node->name, (const xmlChar *) tag->name))
          if(tag->handler)
            tag->handler(cur_node);
      }
    }
  }
}

void parseScheduling(xmlNode *node)
{

  if(!xmlStrcmp(node->name, (const xmlChar *) SCHEDULING))
  {
    schedule.type = (char*)xmlGetProp(node,SCHEDULING_TYPE);
  }
}

void xmlCleanup(xmlDocPtr doc)
{
  /*free the document */
  xmlFreeDoc(doc);

  /*
   *Free the global variables that may
   *have been allocated by the parser.
   */
  xmlCleanupParser();
}

int xmlParser(char *filename, int numInstances)
{
  int       retval = 0;
  xmlNode   *cur_node;
  xmlDocPtr doc;

  if(!filename)
  {
    printf("error: missing config filename\n");
    return -1;
  }

  doc = xmlParseFile(filename);

  if (doc == NULL)
  { 
    printf("error: could not parse file %s\n",filename);
    return -1;
  }

  // --------------------------------------------------------------------------
  // XML root.
  // --------------------------------------------------------------------------
  xmlNode *root = NULL;
  root = xmlDocGetRootElement(doc);
  
  // --------------------------------------------------------------------------
  // Must have root element, a name and the name must be "Configuration"
  // --------------------------------------------------------------------------
  if( !root ||
      !root->name ||
      xmlStrcmp(root->name,CONFIGURATION)) 
  {
    printf("Invalid input file!\n");
    xmlFreeDoc(doc);
    return -1;
  }

  // --------------------------------------------------------------------------
  // Configuration children
  // --------------------------------------------------------------------------
  for(cur_node = root->children; cur_node != NULL; cur_node = cur_node->next)
  {
    if(cur_node->type == XML_ELEMENT_NODE)
    {
      int i,size=sizeof(configTag)/sizeof(TagID);
      TagID *tag=&configTag[0];
      for(i=0; i<size; i++,tag++)
      {
        if(!xmlStrcmp(cur_node->name, (const xmlChar *) tag->name))
          if(tag->handler)
            tag->handler(cur_node);
      }
    }
  }

  if(_numInstances != numInstances)
  {
    printf("error: instance number in config file (%d) DOESN'T match network (%d)\n",_numInstances,numInstances);
    _numConnects = -1;
  }

  // --------------------------------------------------------------------------

  xmlCleanup(doc);

  return _numConnects;
}