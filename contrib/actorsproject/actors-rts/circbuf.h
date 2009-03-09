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

#define MAX_CIRCBUF_LEN		4096
#define TOKEN_SIZE			sizeof(int)

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
	char			buf[MAX_CIRCBUF_LEN];		//data buffer
	int				writeptr;					//write pointer
	long			numWrites;					//number of writes
	BLOCK			block;						//output port block
	int				numReaders;					//number of readers
	READER			*reader;					//reader

	STATS			stats;
}CIRC_BUFFER;

extern CIRC_BUFFER		circularBuf[];

extern void init_circbuf(CIRC_BUFFER *cb,int numReaders);
extern int get_circbuf_space(CIRC_BUFFER *cb);
extern int get_circbuf_area(CIRC_BUFFER *cb,int index);
extern int read_circbuf(CIRC_BUFFER *cb,char *buf, int size, int index);
extern int write_circbuf(CIRC_BUFFER *cb,char *buf, int size);
extern int peek_circbuf_area(CIRC_BUFFER *cb,char *buf, int size, int index, int offset);

#endif

