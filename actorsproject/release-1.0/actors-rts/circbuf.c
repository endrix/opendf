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

CIRC_BUFFER				circularBuf[256];

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

	for(i=0; i<cb->numReaders;i++)
	{
		area = max(area,(cb->numWrites - cb->reader[i].numReads));
	}	
	
	space = MAX_CIRCBUF_LEN - area;

	if(space < TOKEN_SIZE)
		cb->stats.nospace++;

	return space;
}	

int get_circbuf_area(CIRC_BUFFER *cb,int index)
{
	int area = 0;

	area = cb->numWrites - cb->reader[index].numReads;
	
	if(area < TOKEN_SIZE)
		cb->stats.nodata++;

	return area;

}

int peek_circbuf_area(CIRC_BUFFER *cb,char *buf, int size, int index, int offset)
{
	int dist;
	int readptr = cb->reader[index].readptr + offset;

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

	return 0;	
}

int read_circbuf(CIRC_BUFFER *cb,char *buf, int size, int index)
{
	int dist;

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

	return 0;
}

int write_circbuf(CIRC_BUFFER *cb,char *buf, int size)
{
	int dist;

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

	return 0;
}
