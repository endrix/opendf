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

#ifndef _CIRCBUF_H
#define _CIRCBUF_H

#include <semaphore.h>

/* make the header usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#define MAX_CIRCBUF_LEN		4096
#define TOKEN_SIZE			sizeof(int32_t)
#define MAX_CIRCBUF_NUM		256

//#define	CB_MUTEXED

typedef struct _STATS{
	unsigned int	nodata;
	unsigned int	nospace;
}STATS;

typedef struct _BLOCK{
	int				aid;						//back point to the blocked actor
	int				num;						//block on number of data or space 
}BLOCK;

typedef struct _READER{
	int 			readptr;					//read pointer
	long			numReads;					//number of reads
	BLOCK			block;						//input port block
}READER;

typedef struct _CIRC_BUFFER{
	char			*buf;						//data buffer
	int				length;						//data length
	int				writeptr;					//write pointer
	long			numWrites;					//number of writes
	BLOCK			block;						//output port block
	int				numReaders;					//number of readers
	READER			*reader;					//reader
	sem_t			lock;						//mutex	

	STATS			stats;
}CIRC_BUFFER;

extern CIRC_BUFFER		circularBuf[];

/** Initializes the CIRC_BUFFER \a cb for the given number of \a numReaders .*/
extern void init_circbuf(CIRC_BUFFER *cb,int numReaders,int length);

/** Returns the free space in CIRC_BUFFER \a cb in bytes, all readers are considered. */
extern int get_circbuf_space(CIRC_BUFFER *cb);

/** Returns the number of readable bytes in the CIRC_BUFFER \a cb for reader \a index. */
extern int get_circbuf_area(CIRC_BUFFER *cb,int index);

/** Reads \a size number of bytes from the CIRC_BUFFER \a cb into the buffer \a buf
  * for the reader \a index .*/
extern int read_circbuf(CIRC_BUFFER *cb,char *buf, int size, int index);

/** Writes \a size number of bytes from the buffer \a buf into the CIRC_BUFFER \a cb. */
extern int write_circbuf(CIRC_BUFFER *cb,const char *buf, int size);

/** Reads \a size number of bytes from the CIRC_BUFFER \a cb starting from the given \a offset
 * relative to the current read position into the buffer \a buf for the reader \a index .
 * It doesn't modify the current read position in the buffer. */
extern int peek_circbuf_area(CIRC_BUFFER *cb,char *buf, int size, int index, int offset);

extern int InitializeCriticalSection(sem_t *semaphore);
extern int EnterCriticalSection(sem_t *semaphore);
extern int LeaveCriticalSection(sem_t *semaphore);

#ifdef __cplusplus
}
#endif

#endif

