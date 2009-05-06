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

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sched.h>
#include <pthread.h>
#include <signal.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sched.h>
#include <syscall.h>
#include "actors-rts.h"
#include "circbuf.h"
#include "dll.h"

#define BLOCKED			0
#define RUNNING			1
#define MAX_LIST_NUM	128

static int				Running = 1;
int						log_level = LOG_ERROR;
int						trace_action = 0;
int						rts_mode = THREAD_PER_ACTOR;
LIST					actorLists[MAX_LIST_NUM];
int						num_lists = 1;
int						numThreads = 1;
int						numInstances = 0;
int						numFifos = 0;
static int				numCpusConfigured;
static int				numCpusOnline;
static int				dispatchMode;

static void stop_run(int sig)
{
	trace(LOG_MUST,"\nprogram stop running: sig=%x pid=%x\n",sig,getpid());
	Running = 0;
}

static void init_print(void)
{
	int						i,j;
	AbstractActorInstance	*pInstance;
	ActorPort				*port;

	printf("\nRuntime System Initialization Info:\n");
	for(i=0; i<numInstances; i++)
	{
		pInstance = actorInstance[i];
		printf("\n %d Actor: %s\n",i,pInstance->actor->name);
		printf("Input ports: %d\n",pInstance->actor->numInputPorts);
		printf("[fifo:index] ");
		for(j=0; j<pInstance->actor->numInputPorts; j++){
			port = &pInstance->inputPort[j];
			printf("%d:%d  ",port->cid,port->readIndex);
		}
		printf("\nOutput ports: %d\n",pInstance->actor->numOutputPorts);
		printf("[fifo] ");
		for(j=0; j<pInstance->actor->numOutputPorts; j++){
			port = &pInstance->outputPort[j];
			printf("%d  ",port->cid);	
		}
		printf("\n");
	}
}

static void printFifostats(void)
{
	int						i,j;
	CIRC_BUFFER				*cb;
	STATS					*s;

	for (i = 0; i < numFifos; i++)
	{
		printf("Fifo: %d\n",i);
		cb = &circularBuf[i];
		s = &cb->stats;
		printf("  Write:   %ld\n",cb->numWrites);
		printf("  Nospace: %d\n",s->nospace);
		printf("  Read:");
		for(j=0; j<cb->numReaders; j++)
		{
			printf("    %ld[%d] %d",cb->reader[j].numReads,j,get_circbuf_area(cb,j));
		}
		printf("\n  Nodata:  %d\n",s->nodata);
	}
}

static void printActorInfo(void)
{
	int						i,j;
	CIRC_BUFFER				*cb;
	AbstractActorInstance	*instance;
	ActorPort				*port;
	BLOCK					*bk;

	for (i = 0; i < numInstances; i++)
	{
		instance = actorInstance[i];
		printf("\n%s: %s\n",instance->actor->name,(actorStatus[i] == 1)?"running":"blocked");
		printf("inputPort[index,readIndex,pinWait]=pinAvail:\n");
		for (j = 0; j < instance->actor->numInputPorts; j++)
		{
			port = &instance->inputPort[j];
			cb = &circularBuf[port->cid];
			bk = &cb->block;
			printf("[%d,%d,%d]=%d ",port->cid,port->readIndex,bk->num,get_circbuf_area(cb,port->readIndex)); 
		}
		printf("\nOut[index,pinWait]=pinAvail:\n");
		for (j = 0; j < instance->actor->numOutputPorts; j++)
		{
			port = &instance->outputPort[j];
			cb = &circularBuf[port->cid];
			bk = &cb->block;
			printf("[%d,%d]=%d ",port->cid,bk->num,get_circbuf_space(cb)); 
		}
	}	
	printf("\n");
}

static DLLIST *node_select(LIST *actorList, DLLIST *lnode)
{
	DLLIST			*node;

	if(lnode && lnode->next)
		node = lnode->next;
	else
		node = actorList->head;

	return node;
}

static int wait_on_write(AbstractActorInstance *instance)
{
	CIRC_BUFFER		*cb;
	BLOCK			*bk;
	ActorPort		*port;
	int				i;

	for(i=0; i<instance->actor->numOutputPorts; i++)
	{
		port = &instance->outputPort[i];
		cb = &circularBuf[port->cid];
		bk = &cb->block;
		if(bk->num)
		{
			if(get_circbuf_space(cb) >= bk->num){
				bk->num = 0;
				return 0;
			}
			actorTrace(instance,LOG_EXEC,"%s_%d waiting for write %d, pinAvail(%d)=%d\n",instance->actor->name,instance->aid,port->cid,bk->num,get_circbuf_space(cb));
		}
	}
	return 2;	
}

static int wait_on_read(AbstractActorInstance *instance)
{
	CIRC_BUFFER		*cb;
	BLOCK			*bk;
	ActorPort		*port;
	int				i;

	for(i=0; i<instance->actor->numInputPorts; i++)
	{
		port = &instance->inputPort[i];
		cb = &circularBuf[port->cid];
		bk = &cb->reader[port->readIndex].block;
		if(bk->num)
		{
			if(get_circbuf_area(cb,port->readIndex) >= bk->num){
 				bk->num = 0;
				return 0;
			}	
			actorTrace(instance,LOG_EXEC,"%s_%d waiting for read %d, pinAvail(%d,%d)=%d\n",instance->actor->name,instance->aid,bk->num,port->cid,port->readIndex,get_circbuf_area(cb,port->readIndex));
		}
	}
	return 1;
}

static void exec_actor_unit(AbstractActorInstance *instance)
{
	int				block;

	trace(LOG_MUST,"Actor %s start running......\n",instance->actor->name);

	//action scheduler
	if(!instance->actor->action_scheduler)
		pthread_exit(NULL);

	while(Running)
	{
		//actor dispatch
		instance->actor->action_scheduler(instance);

		//check for write&read
		pthread_mutex_lock(&instance->mt);

		block = wait_on_write(instance);
		if(block)
			block = wait_on_read(instance);

		if(block)
		{
 			actorTrace(instance,LOG_INFO,"%s_%d blocked on %s\n",instance->actor->name,instance->aid,(block==1)?"read":"write");
			actorStatus[instance->aid]=BLOCKED;
			pthread_cond_wait(&instance->cv, &instance->mt);
			actorStatus[instance->aid]=RUNNING;
			actorTrace(instance,LOG_INFO,"%s_%d wakeup\n",instance->actor->name,instance->aid);
		}
		pthread_mutex_unlock(&instance->mt);
	}

	pthread_exit(NULL);
}

static void exec_lists_unit(LIST *actorList)
{
	DLLIST			*lnode = NULL;
	AbstractActorInstance *instance;
	int				noRuns = 0;
	unsigned long	mask;

	if(actorList->numNodes == 0)
		pthread_exit(NULL);

	//distribute thread on to cpu
	if(dispatchMode == 1){	
		mask = actorList->lid%numCpusOnline;
		mask = 1<<mask;
		syscall(__NR_sched_setaffinity, 0, sizeof(mask), &mask);
	}

	trace(LOG_MUST,"Thread(%d) containing %d actors start running at cpu %d......\n",actorList->lid,actorList->numNodes,sched_getcpu());

	while(Running)
	{
		//select actor instance
		lnode = node_select(actorList,lnode);
		instance = (AbstractActorInstance*)lnode->obj;

		//actor dispatch
		if(instance->execState){
			if(instance->actor->action_scheduler){
// 				pthread_mutex_lock(&instance->mt);				
				instance->execState = 0;
// 				pthread_mutex_unlock(&instance->mt);
				noRuns = 0;
				instance->actor->action_scheduler(instance);
			}
		}
		else
		{
			noRuns++;
			if(noRuns>=actorList->numNodes)
			{
				noRuns = 0;
				sched_yield();
			}
		}
	}
	pthread_exit(NULL);
}

static void exec_list_unit(void *t)
{
	DLLIST			*lnode = NULL;
	AbstractActorInstance *instance;
	LIST 			*alist = &actorLists[0];
	unsigned long	mask;

	if(alist->numNodes == 0)
		pthread_exit(NULL);

	//distribute thread on to cpu
	if(dispatchMode == 1)
	{
		mask = (int)t%numCpusOnline;
		mask = 1<<mask;
		syscall(__NR_sched_setaffinity, 0, sizeof(mask), &mask);
	}
	trace(LOG_MUST,"Thread(%d) containing %d actors start running at cpu %d......\n",(int)t,alist->numNodes,sched_getcpu());

	while(Running)
	{
		//select actor instance
		lnode = node_select(alist,lnode);
		instance = (AbstractActorInstance*)lnode->obj;

		//actor dispatch
		pthread_mutex_lock(&instance->mt);		
		if(instance->execState){
			if(instance->actor->action_scheduler){
				instance->execState = 0;
				instance->actor->action_scheduler(instance);
			}
		}
		pthread_mutex_unlock(&instance->mt);
	}
	pthread_exit(NULL);
}

int actors_status(void)
{
	int i;
	int ret = BLOCKED;
	int status;

	for(i=0; i<numInstances; i++)
	{
		if(rts_mode == THREAD_PER_ACTOR)
			status = actorStatus[i];
		else
			status = actorInstance[i]->execState;

		if(status== RUNNING)
		{
			ret = RUNNING;
			break;
		}
	}
	return ret;
}

void display_sys_info()
{
	if (numCpusConfigured != -1)
	{
		trace(LOG_MUST,"Processors configured : %d\n",numCpusConfigured);
	}
	else
	{
		trace(LOG_MUST,"Unable to read configured processor count.\n");
	}

	if (numCpusOnline != -1)
	{
		trace(LOG_MUST,"Processors online     : %d\n",numCpusOnline);
	}
	else
	{
		trace(LOG_MUST,"Unable to read online processor count.\n");
	}
}

void init_actor_network(const NetworkConfig *network)
{
	CIRC_BUFFER				*cb;
	ActorClass				*ptr;
	ActorPort				*port;
	int						i,j;
	DLLIST					*lnode;
	ActorConfig				**actors;
	AbstractActorInstance	*pInstance;
	int						cid;
	int						FifoSizes[MAX_CIRCBUF_LEN];
	int						NumReaderInstances[128];
	int						FifoOutputPortIndex[128];
	int						FifoInputPortIndex[128][128];

	int						listIndex = 0;
	int						numActorsPerList = 0;

	memset(NumReaderInstances,0,sizeof(NumReaderInstances));

	actors = network->networkActors;
	numCpusConfigured = sysconf(_SC_NPROCESSORS_CONF);
	numCpusOnline     = sysconf(_SC_NPROCESSORS_ONLN);

	numInstances = network->numNetworkActors;

	for(i=0; i<MAX_CIRCBUF_LEN;i++)
		FifoSizes[i] = MAX_CIRCBUF_LEN;

	//get number of actors per list in average
	if(rts_mode != THREAD_PER_ACTOR)
	{
		numActorsPerList = network->numNetworkActors/num_lists;
		while(network->numNetworkActors - numActorsPerList*num_lists > numActorsPerList)
			numActorsPerList++;
	}

	//actor port init
	for(i=0; i<network->numNetworkActors; i++)
	{
		
		ptr = actors[i]->actorClass;
		pInstance = (AbstractActorInstance*)malloc(ptr->sizeActorInstance);
		memset(pInstance,0,ptr->sizeActorInstance);
		pInstance->actor = ptr;
		pInstance->aid = i;

		//setup parameters if any
		if(pInstance->actor->set_param)
			pInstance->actor->set_param(pInstance,actors[i]->numParams,actors[i]->params);

		pInstance->inputPort = (ActorPort*)malloc(sizeof(ActorPort)*ptr->numInputPorts);
		pInstance->outputPort = (ActorPort*)malloc(sizeof(ActorPort)*ptr->numOutputPorts);

		for( j=0; j<ptr->numInputPorts; j++)
		{
			port = &pInstance->inputPort[j];
			cid = actors[i]->inputPorts[j];
			port->cid = cid;
			port->aid = i;
			port->portDir = INPUT;
			port->readIndex = NumReaderInstances[cid];
// 			port->tokenSize = TOKEN_SIZE;
			port->tokenSize = ptr->inputPortSizes[j];
			FifoInputPortIndex[cid][port->readIndex] = i;
			NumReaderInstances[cid]++;
		}

		for( j=0; j<ptr->numOutputPorts; j++)
		{
			port = &pInstance->outputPort[j];	
			cid = actors[i]->outputPorts[j];
			port->cid = cid;
			port->aid = i;
// 			port->tokenSize = TOKEN_SIZE;
 			port->tokenSize = ptr->outputPortSizes[j];
			port->portDir = OUTPUT;
			//per fifo size
// 			portSizes[cid] = actors[i]->portSizes[j];
			FifoOutputPortIndex[cid] = i;
		}

		pthread_mutex_init(&pInstance->mt, NULL);
		pthread_cond_init (&pInstance->cv, NULL);

		pInstance->aid = i;
		pInstance->execState = 1;
		actorStatus[i] = 1;
		actorInstance[i] = pInstance;

		//append to a double linked list
		if(rts_mode != THREAD_PER_ACTOR)
		{
			if(i >= numActorsPerList-1){
				if(i%numActorsPerList == 0){
					if((network->numNetworkActors - 1 - i) > numActorsPerList/2)
						listIndex++;
				}
			}
			lnode = (DLLIST *)malloc(sizeof(DLLIST));
			lnode->obj = pInstance;
			append_node(&actorLists[listIndex],lnode);
			actorLists[listIndex].lid = listIndex;
		}		

	}
	if(rts_mode != THREAD_PER_ACTOR)
	{
		if(num_lists < listIndex + 1){
			num_lists = listIndex + 1;
			trace(LOG_MUST,"Number of list adjusted to %d\n",num_lists); 
		}
	}
		
	//fifo init
	numFifos = network->numFifos;
	for(i=0; i<numFifos; i++)
	{
		cb = &circularBuf[i]; 
		init_circbuf(cb, NumReaderInstances[i],FifoSizes[i]);
		cb->block.aid = FifoOutputPortIndex[i];
		for(j=0; j<NumReaderInstances[i]; j++)
			cb->reader[j].block.aid = FifoInputPortIndex[i][j];
	}

#if 0
	trace(LOG_EXEC,"\nFifo configuration:\n");
 	for(i=0; i<numFifos; i++)
 	{
		cb = &circularBuf[i]; 
		trace(LOG_EXEC,"Fifo[%d]:\n",i);
		trace(LOG_EXEC,"Input from actor: ");
		trace(LOG_EXEC,"%d\n",cb->block.aid);
		trace(LOG_EXEC,"Output to actor : ");
		for(j=0; j<NumReaderInstances[i]; j++)
 			trace(LOG_EXEC,"%d ",cb->reader[j].block.aid);
		trace(LOG_INFO,"\n");
 	}
#endif

	//constructor
	for (i=0; i<numInstances; i++) {
		if(actorInstance[i]->actor->constructor){
			actorInstance[i]->actor->constructor(actorInstance[i]);
		}
	}
}

int evaluate_args(int argc, char *argv[])
{
	int c;
	int num = 1;
	//command line param parser
	while ((c = getopt (argc, argv, "tvhn:l:m:d:")) != -1)
	{
		switch (c)
		{
			case 'h':
				fprintf (stderr, "%s [-l <trace level>] [-h]...\n",argv[0]);
				fprintf (stderr, "  -l <trace level>   set trace level(0:must 1:error 2:warn 3:info 4:exec)\n");
				fprintf (stderr, "  -m <rts mode>      set rts mode (1:per actor 2:per list 3:single list\n");
				fprintf (stderr, "  -n <num of threads>set number of threads (default is 1)\n");
				fprintf (stderr, "  -d <disptch mode>  set thread dispatch mode (0: auto 1: fixed)\n");
				fprintf (stderr, "  -t                 turn on trace for action scheduler\n");
				fprintf (stderr, "  -h                 print this help and exit\n");
				fprintf (stderr, "  -v                 print version information and exit\n");
				return 1;
			case 'v':
				fprintf (stderr, "RTS Version: %s\n",RTS_VERSION);
				return 1;
			case 'l':
				log_level = atoi(optarg);
				break;
			case 'n':
				num = atoi(optarg);
				break;
			case 't':
				trace_action = 1;
				break;
			case 'm':
				rts_mode = atoi(optarg);
				break;
			case 'd':
				dispatchMode = atoi(optarg);
				break;
 			case '?':
// 			if (optopt == 'l')
// 				fprintf (stderr, "Option -%c requires an argument.\n", optopt);
 				return 1;
			default:
				break;
		}
	}

	//catch prog faults
	signal(SIGINT, stop_run);

	switch(rts_mode){
		case THREAD_PER_LIST:
			num_lists = num;
			break;
		case SINGLE_LIST:
			numThreads = num;
			num_lists = 1;
			break;
		case THREAD_PER_ACTOR:
			num_lists = 0;
			break;
		default:
			trace(LOG_MUST, "Invalid rts mode %d, default to THREAD_PER_ACTOR mode\n",rts_mode);
			rts_mode = THREAD_PER_ACTOR;
			break;
	}
	return 0;
}

int run_actor_network(void)
{
	int			interval=2;
	int			count=0;
	pthread_attr_t attr;
	pthread_t	tid[MAX_ACTOR_NUM];
	int numberOfThreads = 0;

	int i = 0;

        /* For portability, explicitly create threads in a joinable state */
	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	switch(rts_mode){
		case THREAD_PER_ACTOR:
			numberOfThreads = numInstances;
			for(i=0; i<numberOfThreads; i++)
			{
				pthread_create(&tid[i], &attr, (void*)exec_actor_unit, (void *) actorInstance[i]);
			}
			break;
		case THREAD_PER_LIST:
			numberOfThreads = num_lists;
			for(i=0; i<numberOfThreads; i++)
			{
				pthread_create(&tid[i], &attr, (void*)exec_lists_unit, (void *) &actorLists[i]);
			}
			break;
		case SINGLE_LIST:
			numberOfThreads = numThreads;
			for(i=0; i<numberOfThreads; i++)
			{
				pthread_create(&tid[i], &attr, (void*)exec_list_unit, (void *)i);
			}
			break;
		default:
			break;
	}	

 	while(Running)
 	{
		count++;
 		sleep(interval);
		if(actors_status() == BLOCKED)
		{
 			Running = 0;
			trace(LOG_MUST,"All the actors got blocked, ready to exit\n");
		}

		if(log_level >=LOG_WARN)
			printActorInfo();

		if(log_level >=LOG_EXEC)
 			printFifostats();
	}

	/* doesn't work yet, because in thread-per-actor mode the threads are sitting in
	 a pthread_cond_wait(), which is not signalled
	for (i=0; i<numberOfThreads; i++) {
	   pthread_join(tid[i], NULL);
	} */


	for (i=0; i<numInstances; i++) {
		if(actorInstance[i]->actor->destructor){
			actorInstance[i]->actor->destructor(actorInstance[i]);
		}
	}
	sleep(1);
	return 0;
}

int execute_network(int argc, char *argv[], const NetworkConfig *networkConfig)
{
	int result = evaluate_args(argc, argv);
	if (result != 0) {
		return result;
	}

	init_actor_network(networkConfig);

	display_sys_info();

	trace(LOG_MUST,"ModuleName            : %s\n",argv[0],numInstances,numFifos);
	trace(LOG_MUST,"numInstances          : %d\n",numInstances);
	trace(LOG_MUST,"numFifos              : %d\n",numFifos);
	trace(LOG_MUST,"Number of lists       : %d\n",num_lists);
	if(rts_mode == SINGLE_LIST)
		trace(LOG_MUST,"Number of threads     : %d\n",numThreads);

	if(log_level >=LOG_INFO) 
		init_print();

	run_actor_network();

	return 0;
}
