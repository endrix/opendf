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

#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <signal.h>
#include <errno.h>
#include "circbuf.h"

#define max(a,b)		(a>b)?a:b

#ifdef CB_MUTEXTED
#define LOCK(_ic)			EnterCriticalSection(&(_ic->lock))
#define UNLOCK(_ic)			LeaveCriticalSection(&(_ic->lock))
#define RELEASE(_ic)		DeleteCriticalSection(&(_ic->lock))
#define INIT(_ic)			InitializeCriticalSection(&(_ic->lock))
#else
#define LOCK(_ic)
#define UNLOCK(_ic)
#define RELEASE(_ic)
#define INIT(_ic)
#endif


CIRC_BUFFER				circularBuf[256];

int InitializeCriticalSection(sem_t *semaphore)
{
	int ret;
	ret = sem_init(semaphore, 0, 1);
	if(ret != 0){
		perror("Unable to initialize the semaphore");
	}
	return ret;
}

int EnterCriticalSection(sem_t *semaphore)
{
	int ret;
	do {
		ret = sem_wait(semaphore);
		if (ret != 0){
		/* the lock wasn't acquired */
		if (errno != EINVAL) {
			perror("Error in sem_wait.");
			return -1;
		} else {
			/* sem_wait() has been interrupted by a signal: looping again */
			printf("sem_wait interrupted. Trying again for the lock...\n");
		}
		}
	} while (ret != 0);

	return ret;
}

int LeaveCriticalSection(sem_t *semaphore)
{
	int ret;
	ret = sem_post(semaphore);
	if (ret != 0)
		perror("Error in sem_post");
	return ret;
}

int DeleteCriticalSection(sem_t *semaphore)
{
	int ret;
	ret = sem_destroy(semaphore);
	return ret; 
}

void init_block(BLOCK *b)
{
	b->aid = 0;
	b->num = 0;
}

void init_circbuf(CIRC_BUFFER *cb,int numReaders)
{
	int i;

	memset(cb,0,sizeof(CIRC_BUFFER));
	init_block(&cb->block);
	INIT(cb);
	cb->numReaders = numReaders;
	cb->reader = (READER*)malloc(sizeof(READER)*numReaders);
	memset(cb->reader,0,sizeof(READER)*numReaders);
	for(i=0;i<numReaders;i++)
		init_block(&cb->reader[i].block);
}

int get_circbuf_space(CIRC_BUFFER *cb)
{
	int space = 0;
	int	i,area = 0;

	LOCK(cb);

	for(i=0; i<cb->numReaders;i++)
	{
		area = max(area,(cb->numWrites - cb->reader[i].numReads));
	}	
	
	space = MAX_CIRCBUF_LEN - area;

	if(space < TOKEN_SIZE)
		cb->stats.nospace++;

	UNLOCK(cb);

	return space;
}	

int get_circbuf_area(CIRC_BUFFER *cb,int index)
{
	int area = 0;

	LOCK(cb);

	area = cb->numWrites - cb->reader[index].numReads;
	
	if(area < TOKEN_SIZE)
		cb->stats.nodata++;

	UNLOCK(cb);

	return area;

}

int peek_circbuf_area(CIRC_BUFFER *cb,char *buf, int size, int index, int offset)
{
	int dist;
	int readptr = cb->reader[index].readptr + offset;

	LOCK(cb);

	if( ( readptr + size) > MAX_CIRCBUF_LEN )  
	{
		dist = MAX_CIRCBUF_LEN - readptr;
		memcpy(buf, (cb->buf + readptr), dist);
		memcpy(buf + dist, cb->buf, size - dist);
	}
	else
	{
		memcpy(buf, (cb->buf + readptr), size);

	}

	UNLOCK(cb);

	return 0;	
}

int read_circbuf(CIRC_BUFFER *cb,char *buf, int size, int index)
{
	int dist;

	LOCK(cb);

	if( ( cb->reader[index].readptr + size) > MAX_CIRCBUF_LEN )  
	{
		dist = MAX_CIRCBUF_LEN - cb->reader[index].readptr;
		memcpy(buf, (cb->buf + cb->reader[index].readptr), dist);
		memcpy(buf + dist, cb->buf, size - dist);
		cb->reader[index].readptr = size - dist;
	}
	else
	{
		memcpy(buf, (cb->buf + cb->reader[index].readptr), size);
		cb->reader[index].readptr += size;

	}
	cb->reader[index].numReads += size; 

	UNLOCK(cb);

	return 0;
}

int write_circbuf(CIRC_BUFFER *cb,char *buf, int size)
{
	int dist;

	LOCK(cb);

	if( ( cb->writeptr + size) > MAX_CIRCBUF_LEN)
	{

		dist = MAX_CIRCBUF_LEN - cb->writeptr;
		memcpy( (buf + cb->writeptr), buf,dist);
		
		memcpy(cb->buf, (buf + dist), size - dist);
		cb->writeptr = size - dist;
	}
	else
	{
		memcpy( (cb->buf + cb->writeptr), buf,size);
		cb->writeptr += size;
	}

	cb->numWrites += size;

	UNLOCK(cb);

	return 0;
}

void release_circbuf(CIRC_BUFFER *cb)
{
	RELEASE(cb);

	return;
}
