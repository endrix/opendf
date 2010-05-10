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
#include <string.h>
#include <libxml/parser.h>

#include "xmlParser.h"
#include "internal.h"


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
#define CATEGORY_VALUE                      (const xmlChar*)"value"
#define INTERFACE_NAME                      (const xmlChar*)"name"
#define CURRENT_SERVICE_INDEX               (const xmlChar*)"index"
#define GRANULARITY_VALUE                   (const xmlChar*)"value"
#define GROUPID_BASE                        (const xmlChar*)"base"
#define SERVICE_LEVEL_INDEX                 (const xmlChar*)"index"
#define QUALITY_OF_SERVICE                  (const xmlChar*)"quality"
#define BMDISTRIBUTION_ID                   (const xmlChar*)"id"
#define BMDISTRIBUTION_VALUE                (const xmlChar*)"value"
#define TOTALBW_VALUE                       (const xmlChar*)"value"
#define TOTALBW_MODE                        (const xmlChar*)"mode"

void parseParttioning(xmlNode *node);
void parseScheduling(xmlNode *node);
void parsePartition(xmlNode *node);
void parseConnection(xmlNode *node);
void parsePMInterface(xmlNode *node);
void parseCategory(xmlNode *node);
void parseCurrentService(xmlNode *node);
void parseGranularity(xmlNode *node);
void parseGroupID(xmlNode *node);
void parseServiceLevels(xmlNode *node);
void parseServiceLevel(xmlNode *node);
void parseQuality(xmlNode *node);
void parseBWDistribution(xmlNode *node);
void parseBWDistributions(xmlNode *node);
void parseTotalBW(xmlNode *node);

typedef void (*HANDLER)(xmlNode *cur_node);

typedef struct _tagID{
  char *name;
  HANDLER handler;
} TagID;

TagID configTag[] ={
{"Partitioning", parseParttioning},
{"RMInterface",  parsePMInterface},
{"Scheduling",   parseScheduling},
{0}
};

TagID partitioningTag[] ={
{"Partition",    parsePartition},
{"Connection",   parseConnection},
{0}
};

TagID pmInterfaceTag[] ={
{"Category",       parseCategory},
{"InitialService", parseCurrentService},
{"GroupID",        parseGroupID},
{"ServiceLevels",  parseServiceLevels},
{0}
};

TagID serviceLevelsTag[] ={
{"ServiceLevel",     parseServiceLevel},
{0}
};

TagID serviceLevelTag[] ={
{"QualityOfService", parseQuality},
{"Granularity",      parseGranularity},
{"TotalBW",          parseTotalBW},
{"BWDistributions",  parseBWDistributions},
{0}
};

TagID bmDistributionsTag[] ={
{"BWDistribution",  parseBWDistribution},
{0}
};

AffinityID    instanceAfinity[MAX_ACTOR_NUM];
ConnectID     connects[MAX_CONNECTS];
ScheduleID    schedule;
RMInterface   rmInterface={"caltest",3,0,0,1,{0,100,25000,200,1,1,{0,80}}};
int			  numPartitions=1;

static int _numInstances;
static int _numConnects;

void printout()
{
  int i,j;

  printf("Schedule type:       %s\n",schedule.type);
  printf("Number of partition: %d\n",numPartitions);
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

  printf("RMInterface name   : %s\n",rmInterface.name);
  printf("Category           : %d\n",rmInterface.categoryValue);
  printf("CurrentServiceLevel: %d\n",rmInterface.currentServiceIndex);
  printf("GroupIDBase        : %d\n",rmInterface.groupIDBase);
  printf("NumServiceLevels   : %d\n",rmInterface.numServiceLevels);
  for(i=0;i<rmInterface.numServiceLevels;i++)
  {
    ServiceLevel *sl=&rmInterface.serviceLevels[i];
    printf("  ServiceLevelIndex  : %d\n",sl->index);
    printf("  QualityOfService   : %d\n",sl->quality);
    printf("  Granularity        : %d\n",sl->granularityValue);
    printf("  TotalBWValue       : %d\n",sl->totalBW);
    printf("  TotalBWMODE        : %d\n",sl->mode);
    printf("  NumBWDistribs      : %d\n",sl->numBMDistributions);
    for(j=0; j<sl->numBMDistributions; j++)
    {
      BWDistribution *bwd = &sl->bwDistributions[j];
      printf("    id                 : %d\n",bwd->id);  
      printf("    value              : %d\n",bwd->value);  
    }
  }
}

void parseNode(xmlNode *node,TagID *tagID)
{
  xmlNode *cur_node;

  for(cur_node = node->children; cur_node != NULL; cur_node = cur_node->next)
  {
    if(cur_node->type == XML_ELEMENT_NODE)
    {
      TagID *tag=tagID;
      for(;tag;tag++)
      {
        if(!xmlStrcmp(cur_node->name, (const xmlChar *) tag->name))
        {    
          if(tag->handler)
            tag->handler(cur_node);
          break;
        }
      }
    }
  }
}

void parseConnection(xmlNode *cur_node)
{
  char *size;

  if(!xmlStrcmp(cur_node->name, CONNECTION));
  {
	if(_numConnects>=MAX_CONNECTS)
	{
	  printf("Number of connections over max allowed (%d)\n",MAX_CONNECTS);
	  return;
	}
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
	  if(_numInstances >= MAX_ACTOR_NUM)
	  {
        printf("Number of actor instances over max allowed (%d)\n", MAX_ACTOR_NUM);
		return;
	  }
      instanceAfinity[_numInstances].name = (char*)xmlGetProp(child_node,INSTANCE_NAME);
      instanceAfinity[_numInstances].affinity = atoi(id);
      _numInstances++;
    }
  }
  numPartitions++;
  xmlFree(id);
}

void parseCategory(xmlNode *node)
{
  char *value;
  value = (char*)xmlGetProp(node,CATEGORY_VALUE);
  if(value){
    if(!strcmp(value,"High"))
      rmInterface.categoryValue=3;
    else if(!strcmp(value,"Medium"))
      rmInterface.categoryValue=2;
    else if(!strcmp(value,"Low"))
      rmInterface.categoryValue=1;
    else
      rmInterface.categoryValue=0;  
  }

  xmlFree(value);
}

void parseCurrentService(xmlNode *node)
{
  char *index;
  index = (char*)xmlGetProp(node,CURRENT_SERVICE_INDEX);
  if(index){
    rmInterface.currentServiceIndex=atoi(index);
  }
  xmlFree(index);
}

void parseGroupID(xmlNode *node)
{
  char *base;
  base = (char*)xmlGetProp(node,GROUPID_BASE);
  if(base){
    rmInterface.groupIDBase=atoi(base);
  }
  xmlFree(base);
}

void parseServiceLevel(xmlNode *node)
{
  char *index;
  index = (char*)xmlGetProp(node,SERVICE_LEVEL_INDEX);
  if(index){
    ServiceLevel *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
    sl->index=atoi(index);
  }

  xmlFree(index);

  parseNode(node,serviceLevelTag);

  rmInterface.numServiceLevels++;
}  

void parseQuality(xmlNode *node)
{
  char *quality;
  quality = (char*)xmlGetProp(node,QUALITY_OF_SERVICE);
  if(quality){
    ServiceLevel *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
    sl->quality=atoi(quality);
  }
  xmlFree(quality);
}

void parseGranularity(xmlNode *node)
{
  char *value;
  value = (char*)xmlGetProp(node,GRANULARITY_VALUE);
  if(value){
    ServiceLevel *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
    sl->granularityValue=atoi(value);
  }
  xmlFree(value);
}

void parseTotalBW(xmlNode *node)
{
  char *value;
  char *mode;

  value = (char*)xmlGetProp(node,TOTALBW_VALUE);
  mode = (char*)xmlGetProp(node,TOTALBW_MODE); 
  if(value){
    ServiceLevel *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
    sl->totalBW=atoi(value);
  }
  if(mode)
  {
     ServiceLevel *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
	 if(strcmp(mode,"Absolute")==0)
	 	sl->mode=1;
	 else if(strcmp(mode,"Relative")==0)
		sl->mode=2;
	 else
        sl->mode=0;
  }
  xmlFree(value);
  xmlFree(mode);
}

void parseBWDistribution(xmlNode *node)
{
  char   *id, *value;

  id    = (char*)xmlGetProp(node,BMDISTRIBUTION_ID);
  value = (char*)xmlGetProp(node,BMDISTRIBUTION_VALUE);
/*
  if(id)
  {
	int index = atoi(id);
	if (index >= MAX_NUM_BMDISTRIBUTIONS)
	{
	  printf("Over max BW distributions: %d\n",index);
	  return;
	}
	if(value)
	{
	  ServiceLevel   *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
	  BWDistribution *bwd=&sl->bwDistributions[index];
	  bwd->value=atoi(value);
	  bwd->id=index;
	  sl->numBMDistributions++;
	}
  }
*/  
	  
  if(id&&value)
  {
    ServiceLevel   *sl=&rmInterface.serviceLevels[rmInterface.numServiceLevels];
    BWDistribution *bwd=&sl->bwDistributions[sl->numBMDistributions];
    bwd->id=atoi(id);
    bwd->value=atoi(value);
    sl->numBMDistributions++;
  }

  xmlFree(id);
  xmlFree(value);
}

void parseBWDistributions(xmlNode *node)
{
  //reset number of BW distributions from default
  rmInterface.serviceLevels[rmInterface.numServiceLevels].numBMDistributions=0;

  parseNode(node,bmDistributionsTag);
}

void parseServiceLevels(xmlNode *node)
{
  //reset number of service levels from default
  rmInterface.numServiceLevels=0;

  parseNode(node,serviceLevelsTag);
}

void parsePMInterface(xmlNode *node)
{
  char *name;
  name = (char*)xmlGetProp(node,INTERFACE_NAME);
  if(name){
    rmInterface.name=name;
  }

  parseNode(node,pmInterfaceTag);
}

void parseParttioning(xmlNode *node)
{
  numPartitions=0;
  parseNode(node,partitioningTag);
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
  parseNode(root,configTag);

  printout();  

  if(_numInstances != numInstances)
  {
    printf("error: instance number in config file (%d) DOESN'T match network (%d)\n",_numInstances,numInstances);
    _numConnects = -1;
  }

  // --------------------------------------------------------------------------

  xmlCleanup(doc);

  return _numConnects;
}
