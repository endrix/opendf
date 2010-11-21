/********************************************************************************/
/**********
This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the
Free Software Foundation; either version 2.1 of the License, or (at your
option) any later version. (See <http://www.gnu.org/copyleft/lesser.html>.)

This library is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
more details.
**********/

#include <string.h>
#include "actors-rts.h"
#include <errno.h>
#include <sys/socket.h>
#include <unistd.h>
#include <semaphore.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h> 
#include <stdio.h>
#include <signal.h>

#include "rtsp.h"
#include "bitstream.h"

//#define _DEBUG_PRINT
#define RECORDER
#define _UDP

#define UDP_PORT	20000

extern char *get_resolution(int serviceLevel);
extern int get_fps(int serviceLevel);

typedef struct _dblist
{
	int      len;
	int      pos;
	int      mark;
	char     data[MAX_PACKET_SIZE];
	struct _dblist   *next;
	struct _dblist   *prior;
}dblist;

typedef struct _videodata
{
	int             numBytes;
	int             numFrames;
	pthread_mutex_t mutex;
	dblist         *head;
	dblist         *tail;
}videodata;

// Forward function definitions:
unsigned fCSeq = 0;
char const* const UserAgentHeaderStr = "User-Agent: Actors Project";

unsigned const parseBufferSize = 100;
char *fBaseURL=NULL;
char *fLastSessionId = NULL;
int  Timeout=0;

unsigned long fMaxPlayEndTime = 0;

unsigned VTimestampFrequency = 0;
unsigned ATimestampFrequency = 0;

unsigned char VPayloadType = 0;
unsigned char APayloadType = 0;
int PotPos = 0;
unsigned long VFrameRate = 0;
uint32_t fixed_vop_rate = 0;

unsigned long vloopNum = 0;
int loop = 1;

static int rtpNumber;

int init_rtsp(char *url,struct MediaAttribute *Attribute,int serviceLevel)
{
	char* sdpDescription = NULL;
	struct MediaSubsession *subsession = NULL;
	int subsessionNum = 0;

	int socketNum = -1;
	int result  = -1;

	fBaseURL = strDup(url);

	//fprintf(stderr,"start openConnectionFromURL %s\n",fBaseURL);
	socketNum = openConnectionFromURL(fBaseURL);

	if(socketNum<0)
	{
		fprintf(stderr,"failed to open the URL: %s \n",url);
		return (-1);
	}
	sdpDescription = (char*)malloc(MAX_READBUFSIZE*sizeof(char));
	if(sdpDescription == NULL)
	{
		fprintf(stderr,"failed to alloc the memory\n");
		return (-1);
	}
	memset(sdpDescription,0,MAX_READBUFSIZE);
	//fprintf(stderr,"start getSDPDescriptionFromURL \n");
	result = getSDPDescriptionFromURL(socketNum,fBaseURL,sdpDescription,serviceLevel);
	if (result<0) 
	{
		fprintf(stderr,"Failed to get a SDP description from URL\n");
		free(sdpDescription);
		close(socketNum);
		return (-1);
	}
#ifdef _DEBUG_PRINT
	fprintf(stderr,"Opened URL %s returning a SDP description:\n%s\n",fBaseURL,sdpDescription);
#endif

	//fprintf(stderr,"start initializeWithSDP\n");
	subsession = initializeWithSDP(sdpDescription,&subsessionNum);
	if(subsession == NULL)
	{
		fprintf(stderr,"Failed to initialize a SDP description\n");
		free(sdpDescription);
		close(socketNum);
		return (-1);
	}
	GetMediaAttrbute(Attribute,subsession,subsessionNum);
	setupStreams(socketNum,subsession,subsessionNum);
	
	startPlayingStreams(socketNum,subsession,subsessionNum);
	free(sdpDescription);
	return socketNum;
}

// Receive RTP over TCP, using the encoding defined in
// RFC 2326, section 10.12
int RTP_ReadHandler(int socketNum,struct ResultData* data)
{
	unsigned char c;
	unsigned char streamChannelId;
	unsigned short size;
	unsigned fNextTCPReadSize;
	int result = -1;
	do 
	{
		do 
		{
			result = recv(socketNum, &c, 1, 0);
			
			if (result <=0) 
			{ 	
				// error reading TCP socket
				//fprintf(stderr,"vloopNum is : %lu\n",vloopNum);
				perror("error reading TCP socket\n");
				return 0;
			}
									
		} while (c != '$');

		if (recv(socketNum, &streamChannelId, 1, 0)!= 1){
			result = -2;
			break;
		}
		if (recv(socketNum, (unsigned char*)&size, 2,0) != 2){
			result = -3;
			break;
		}
		fNextTCPReadSize = ntohs(size);
		return networkReadHandler(socketNum,&fNextTCPReadSize,data); 

	} while (0);

	return result;
}

void clearup(int socketNum)
{
	//teardownMediaSession(socketNum);
	
	if(fBaseURL !=NULL) free(fBaseURL);
	if(fLastSessionId !=NULL) free(fLastSessionId);

	if(socketNum>=0)
	{
		close(socketNum);
		printf("%s(%d) socketNum closed!!\n",__FILE__,__LINE__);
	}
}

void startPlayingStreams(int socketNum,struct MediaSubsession *subsession,int subsessionNum)
{

	if(playMediaSession(socketNum,0,-1))
	{
		fprintf(stderr,"Play MediaSession failed\n");
		close(socketNum);
		exit(0);
	}
	//fprintf(stderr,"Play MediaSession successful\n");

}

void setupStreams(int socketNum,struct MediaSubsession *subsession,int subsessionNum) 
{
	struct MediaSubsession *mediasub;
	mediasub = subsession;
	while(subsessionNum>0)
	 {
			if(setupMediaSubsession(socketNum,mediasub)) 
			{	
				fprintf(stderr,"Setup MediaSubsession Failed\n");
				exit(0);
			}
			mediasub = mediasub->fNext;
			subsessionNum--;
	 }
	//fprintf(stderr,"Setup Streams successful\n");
}

int handleRead(int socketNum,unsigned char* buffer,unsigned bufferMaxSize,unsigned *bytesRead,unsigned* NextTCPReadSize) 
{
	int readSuccess = -1;
	unsigned totBytesToRead;
	unsigned curBytesToRead;
	unsigned curBytesRead;

	if (socketNum < 0) 
	{
		fprintf(stderr,"no socket active\n");
		return -1;
	}
	else 
	{
		// Read from the TCP connection:
		*bytesRead = 0;
		totBytesToRead = *NextTCPReadSize;
		*NextTCPReadSize = 0;
    
		if (totBytesToRead > bufferMaxSize) totBytesToRead = bufferMaxSize; 
    
		curBytesToRead = totBytesToRead;
		while ((curBytesRead = recv(socketNum,&buffer[*bytesRead], curBytesToRead,0)) > 0) 
		{
      			(*bytesRead) += curBytesRead;
			if ((*bytesRead) >= totBytesToRead) break;
			curBytesToRead -= curBytesRead;
			
		}
		if (curBytesRead <= 0) 
		{
			*bytesRead = 0;
			readSuccess = -1;
		} 
		else 
		{
			readSuccess = 0;
		}
	}

	return readSuccess;
}


unsigned char* skip(unsigned char * buffer,unsigned numBytes) 
{
  buffer = buffer + numBytes;
	return buffer;
} 

int networkReadHandler(int socketNum,unsigned *NextTCPReadSize,struct ResultData* data)
{
	unsigned bytesRead;
	unsigned datasize;
	unsigned rtpHdr;
	unsigned char *buffer;
	int rtpMarkerBit;
	unsigned long rtpTimestamp;
	static unsigned long LVrtpTimestamp = 0;
	static unsigned long LArtpTimestamp = 0;
	unsigned rtpSSRC;
	unsigned cc;
	unsigned char payloadType = 0;
	static unsigned long subTime = 0;
	static unsigned long MaxFrameNum = 0;
	//static unsigned long vloopNum = 0;
	static unsigned long aloopNum = 0;
	static unsigned long audioFirstTimestamp = 0;
	static unsigned long vedioFirstTimestamp = 0;

	unsigned extHdr;
	unsigned remExtSize;
	unsigned numPaddingBytes;
	unsigned short rtpSeqNo;
	static unsigned long time_inc;
	static unsigned subTimeFlag = 0;
	unsigned char *bufptr;//[MAX_PACKET_SIZE];


	buffer = data->buffer;
	bufptr = (unsigned char *)malloc(MAX_PACKET_SIZE*sizeof(unsigned char));
	if(bufptr == NULL)
	{
		fprintf(stderr,"alloc failed\n");
		return -1;
	}
	memset(bufptr,0,MAX_PACKET_SIZE);
	do 
	{
		if(handleRead(socketNum,buffer,MAX_PACKET_SIZE,&bytesRead,NextTCPReadSize)) break;
		memcpy(bufptr,buffer,bytesRead);
		datasize = bytesRead;
		// Check for the 12-byte RTP header:
		if (datasize < 12) break;
		rtpHdr = ntohl(*(unsigned*)(buffer)); 
		buffer = skip(buffer,4);
		datasize -=4;
		rtpMarkerBit = (rtpHdr&0x00800000) >> 23;
		rtpTimestamp = ntohl(*(unsigned*)(buffer));
		buffer = skip(buffer,4);
		datasize -=4;
		rtpSSRC = ntohl(*(unsigned*)(buffer)); 
		buffer = skip(buffer,4);
		datasize -=4;
    
		if ((rtpHdr&0xC0000000) != 0x80000000) break;
    
		cc = (rtpHdr>>24)&0xF;
		if (datasize < cc) break;
		buffer = skip(buffer,cc);
		datasize-=cc;
		
		// Check for (& ignore) any RTP header extension
		if (rtpHdr&0x10000000) 
		{
			fprintf(stderr,"1 datasize is %u\n",datasize);
			if (datasize < 4) break;
			extHdr = ntohl(*(unsigned*)(buffer)); 
			buffer = skip(buffer,4);
			datasize -=4;
			remExtSize = 4*(extHdr&0xFFFF);
			if (datasize < remExtSize) break;
			buffer = skip(buffer,remExtSize);
			datasize -= remExtSize;
			fprintf(stderr,"2 datasize is %u\n",datasize);
      
		}
    
		// Discard any padding bytes:
		if (rtpHdr&0x20000000) 
		{
			if (datasize == 0) break;
			numPaddingBytes	= (unsigned)(buffer)[datasize-1];
			if (datasize < numPaddingBytes) break;
			if (numPaddingBytes > datasize) numPaddingBytes = datasize;
			datasize -= numPaddingBytes;
		}    

		// Check the Payload Type.
		payloadType = (unsigned char)((rtpHdr&0x007F0000)>>16);

		rtpSeqNo = (unsigned short)(rtpHdr&0xFFFF);

		if(payloadType == VPayloadType)
		{
	
			if(vloopNum == 0)
			{
				vedioFirstTimestamp = rtpTimestamp;
				LVrtpTimestamp = rtpTimestamp;
			}
			
			if(rtpTimestamp<LVrtpTimestamp) break;
			if(rtpTimestamp>=LVrtpTimestamp)
			{
			//	fprintf(stderr,"LVrtpTimestamp is %lu;rtpTimestamp is %lu\n",LVrtpTimestamp,rtpTimestamp);
				LVrtpTimestamp = rtpTimestamp;
			}
				
			memcpy(data->buffer,buffer,datasize);
			data->len = datasize;
			data->fRTPPayloadFormat = payloadType;
			data->frtpTimestamp = rtpTimestamp;
			data->frtpMarkerBit = rtpMarkerBit;
			
			if(rtpTimestamp>vedioFirstTimestamp&&!subTimeFlag)
			{
				if(!fixed_vop_rate)
				{
					subTime = rtpTimestamp-vedioFirstTimestamp;
					time_inc = VTimestampFrequency/subTime;
					VFrameRate = VTimestampFrequency*1000/subTime;
					//fprintf(stderr,"time_inc is %lu\n",time_inc);
					if(time_inc<24) 
					{
						time_inc = 24;
						VFrameRate = 24000*1000/1001;
					}
					//fprintf(stderr,"subTime is %lu\n",subTime);
					//fprintf(stderr,"time_inc is %lu\n",time_inc);
					MaxFrameNum = fMaxPlayEndTime*(time_inc*10-3)+5000;
				}
				else
				{
					MaxFrameNum = fMaxPlayEndTime*VFrameRate*10;	
				}
				subTimeFlag = 1;
				//fprintf(stderr,"VFrameRate is %lu\n",VFrameRate);
				//fprintf(stderr,"fixed_vop_rate is %d\n",fixed_vop_rate);
			}
			
			if(rtpMarkerBit)
			{
				
				vloopNum++;
				if(MaxFrameNum>0&&vloopNum*10000>=MaxFrameNum)
				{
					//fprintf(stderr,"vloopNum is : %lu\n",vloopNum);
					//fprintf(stderr,"MaxFrameNum is : %lu\n",MaxFrameNum);
					//fprintf(stderr,"(rtpTimestamp-vedioFirstTimestamp)/VTimestampFrequency is :%f\n",(double)(rtpTimestamp-vedioFirstTimestamp)/(double)VTimestampFrequency);

					//teardownMediaSession(socketNum);
					//free(bufptr);	
					//return 0;
				}
			}
		}
		else if(payloadType == APayloadType)
		{
			
			if(aloopNum == 0)
			{
				audioFirstTimestamp = rtpTimestamp;
				LArtpTimestamp = rtpTimestamp;
			}
			if(rtpTimestamp<LArtpTimestamp) break;
			if(rtpTimestamp>=LArtpTimestamp)
			{
				//fprintf(stderr,"LArtpTimestamp is %lu;rtpTimestamp is %lu\n",LArtpTimestamp,rtpTimestamp);
				LArtpTimestamp = rtpTimestamp;
			}

			datasize -=4;
			memcpy(buffer,buffer+4,datasize);
			memcpy(data->buffer,buffer,datasize);
			data->len = datasize;
			data->fRTPPayloadFormat = payloadType;
			data->frtpTimestamp = rtpTimestamp;
			data->frtpMarkerBit = rtpMarkerBit;
			if(rtpMarkerBit) aloopNum++;
		}
	} while (0);
	free(bufptr);
	return -1;
}

int RTP_ReadHandler2(struct ResultData* data,int bytesRead)
{
	//unsigned bytesRead;
	unsigned datasize;
	unsigned rtpHdr;
	unsigned char *buffer;
	int rtpMarkerBit;
	unsigned long rtpTimestamp;
	static unsigned long LVrtpTimestamp = 0;
	static unsigned long LArtpTimestamp = 0;
	unsigned rtpSSRC;
	unsigned cc;
	unsigned char payloadType = 0;
	static unsigned long subTime = 0;
	static unsigned long MaxFrameNum = 0;
	//static unsigned long vloopNum = 0;
	static unsigned long aloopNum = 0;
	static unsigned long audioFirstTimestamp = 0;
	static unsigned long vedioFirstTimestamp = 0;

	unsigned extHdr;
	unsigned remExtSize;
	unsigned numPaddingBytes;
	unsigned short rtpSeqNo;
	static unsigned long time_inc;
	static unsigned subTimeFlag = 0;
	unsigned char *bufptr;//[MAX_PACKET_SIZE];


	buffer = data->buffer;
	bufptr = (unsigned char *)malloc(MAX_PACKET_SIZE*sizeof(unsigned char));
	if(bufptr == NULL)
	{
		fprintf(stderr,"alloc failed\n");
		return -1;
	}
	memset(bufptr,0,MAX_PACKET_SIZE);
	do 
	{
		memcpy(bufptr,buffer,bytesRead);
		datasize = bytesRead;
		// Check for the 12-byte RTP header:
		if (datasize < 12) break;
		rtpHdr = ntohl(*(unsigned*)(buffer)); 
		buffer = skip(buffer,4);
		datasize -=4;
		rtpMarkerBit = (rtpHdr&0x00800000) >> 23;
		rtpTimestamp = ntohl(*(unsigned*)(buffer));
		buffer = skip(buffer,4);
		datasize -=4;
		rtpSSRC = ntohl(*(unsigned*)(buffer)); 
		buffer = skip(buffer,4);
		datasize -=4;
    
		if ((rtpHdr&0xC0000000) != 0x80000000) break;
    
		cc = (rtpHdr>>24)&0xF;
		if (datasize < cc) break;
		buffer = skip(buffer,cc);
		datasize-=cc;
		
		// Check for (& ignore) any RTP header extension
		if (rtpHdr&0x10000000) 
		{
			fprintf(stderr,"1 datasize is %u\n",datasize);
			if (datasize < 4) break;
			extHdr = ntohl(*(unsigned*)(buffer)); 
			buffer = skip(buffer,4);
			datasize -=4;
			remExtSize = 4*(extHdr&0xFFFF);
			if (datasize < remExtSize) break;
			buffer = skip(buffer,remExtSize);
			datasize -= remExtSize;
			fprintf(stderr,"2 datasize is %u\n",datasize);
      
		}
    
		// Discard any padding bytes:
		if (rtpHdr&0x20000000) 
		{
			if (datasize == 0) break;
			numPaddingBytes	= (unsigned)(buffer)[datasize-1];
			if (datasize < numPaddingBytes) break;
			if (numPaddingBytes > datasize) numPaddingBytes = datasize;
			datasize -= numPaddingBytes;
		}    

		// Check the Payload Type.
		payloadType = (unsigned char)((rtpHdr&0x007F0000)>>16);

		rtpSeqNo = (unsigned short)(rtpHdr&0xFFFF);

		if(payloadType == VPayloadType)
		{
			if(vloopNum == 0)
			{
				vedioFirstTimestamp = rtpTimestamp;
				LVrtpTimestamp = rtpTimestamp;
			}
			
			if(rtpTimestamp<LVrtpTimestamp) break;
			if(rtpTimestamp>=LVrtpTimestamp)
			{
			//	fprintf(stderr,"LVrtpTimestamp is %lu;rtpTimestamp is %lu\n",LVrtpTimestamp,rtpTimestamp);
				LVrtpTimestamp = rtpTimestamp;
			}
				
			memcpy(data->buffer,buffer,datasize);
			data->len = datasize;
			data->fRTPPayloadFormat = payloadType;
			data->frtpTimestamp = rtpTimestamp;
			data->frtpMarkerBit = rtpMarkerBit;
			
			if(rtpTimestamp>vedioFirstTimestamp&&!subTimeFlag)
			{
				if(!fixed_vop_rate)
				{
					subTime = rtpTimestamp-vedioFirstTimestamp;
					time_inc = VTimestampFrequency/subTime;
					VFrameRate = VTimestampFrequency*1000/subTime;
					//fprintf(stderr,"time_inc is %lu\n",time_inc);
					if(time_inc<24) 
					{
						time_inc = 24;
						VFrameRate = 24000*1000/1001;
					}
					//fprintf(stderr,"subTime is %lu\n",subTime);
					//fprintf(stderr,"time_inc is %lu\n",time_inc);
					MaxFrameNum = fMaxPlayEndTime*(time_inc*10-3)+5000;
				}
				else
				{
					MaxFrameNum = fMaxPlayEndTime*VFrameRate*10;	
				}
				subTimeFlag = 1;
				//fprintf(stderr,"VFrameRate is %lu\n",VFrameRate);
				//fprintf(stderr,"fixed_vop_rate is %d\n",fixed_vop_rate);
			}
			
			if(rtpMarkerBit)
			{
				vloopNum++;
				if(MaxFrameNum>0&&vloopNum*10000>=MaxFrameNum)
				{
					//fprintf(stderr,"vloopNum is : %lu\n",vloopNum);
					//fprintf(stderr,"MaxFrameNum is : %lu\n",MaxFrameNum);
					//fprintf(stderr,"(rtpTimestamp-vedioFirstTimestamp)/VTimestampFrequency is :%f\n",(double)(rtpTimestamp-vedioFirstTimestamp)/(double)VTimestampFrequency);

					//teardownMediaSession(socketNum);
					//free(bufptr);	
					//return 0;
				}
			}
		}
		else if(payloadType == APayloadType)
		{
			
			if(aloopNum == 0)
			{
				audioFirstTimestamp = rtpTimestamp;
				LArtpTimestamp = rtpTimestamp;
			}
			if(rtpTimestamp<LArtpTimestamp) break;
			if(rtpTimestamp>=LArtpTimestamp)
			{
				//fprintf(stderr,"LArtpTimestamp is %lu;rtpTimestamp is %lu\n",LArtpTimestamp,rtpTimestamp);
				LArtpTimestamp = rtpTimestamp;
			}

			datasize -=4;
			memcpy(buffer,buffer+4,datasize);
			memcpy(data->buffer,buffer,datasize);
			data->len = datasize;
			data->fRTPPayloadFormat = payloadType;
			data->frtpTimestamp = rtpTimestamp;
			data->frtpMarkerBit = rtpMarkerBit;
			if(rtpMarkerBit) aloopNum++;
		}
	} while (0);
	free(bufptr);
	return -1;
}

int parseResponseCode(char* line, unsigned int * responseCode) 
{
	if (sscanf(line, "%*s%u", responseCode) != 1) 
	{
		fprintf(stderr,"no response code in line: %s\n",line);
		return -1;
	}

	return 0;
}

int optionsMediaSession(int socketNum) 
{
	char* cmd = NULL;
	char cmdFmt[] =
			"OPTIONS %s RTSP/1.0\r\n"
			"CSeq: %d\r\n"
			"%s\r\n"
			"Session: %s\r\n"
			"Authorization: Basic cm9vdDpwYXNz\r\n\r\n";
	unsigned cmdSize;
	unsigned const readBufSize = 10000;
	char *readBuffer;
	char* readBuf;
	int bytesRead;
	char* firstLine;
	char* nextLineStart;
	unsigned responseCode;
	if(socketNum < 0) return -1;
	readBuffer = (char*)malloc(sizeof(char)*(readBufSize+1));
	if(readBuffer == NULL) return -1;
	memset(readBuffer,0,readBufSize+1);
	do 
	{
		// First, make sure that we have a RTSP session in progress
		if (fLastSessionId == NULL) 
		{
			fprintf(stderr,"No RTSP session is currently in progress\n");
			break;
		}

		// Send the OPTIONS command:

		cmdSize = strlen(cmdFmt)
			+ strlen(fBaseURL)
			+ 20 /* max int len */
			+ strlen(fLastSessionId)
			+ strlen(UserAgentHeaderStr);
		cmd = (char*)malloc(sizeof(char)*cmdSize);
		if(cmd == NULL) 
		{
			free(readBuffer);
			return -1;
		}
		memset(cmd,0,cmdSize);
		sprintf(cmd, cmdFmt,
			fBaseURL,
			++fCSeq,
			UserAgentHeaderStr,
			fLastSessionId
			);
#ifdef _DEBUG_PRINT
		fprintf(stderr,"OPTIONS-%d:\n%s\n",fCSeq,cmd);
#endif
		if (send(socketNum,cmd,strlen(cmd),0)<0) 
		{
			fprintf(stderr,"OPTIONS send() failed\n ");
			break;
		}
#ifdef _UDP

		readBuf = readBuffer;
		// Get the response from the server:
		bytesRead = getResponse(socketNum,readBuf, readBufSize);

		if (bytesRead <= 0)
		{
			fprintf(stderr,"getResponse failed: %d\n",bytesRead);
			break;
		}
#ifdef _DEBUG_PRINT
		fprintf(stderr,"OPTIONS response-%d:\n%s\n",fCSeq,readBuf);
#endif
		
		// Inspect the first line to check whether it's a result code 200
		firstLine = readBuf;
		nextLineStart = getLine(firstLine);
		if (parseResponseCode(firstLine,&responseCode)) break;
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle OPTIONS response\n ");
			break;
		}
#endif		
		free(cmd);
		free(readBuffer);
		return 0;
	} while (0);

	free(cmd);
	free(readBuffer);
	return -1;
}

int setupMediaSubsession(int socketNum,struct MediaSubsession* subsession) 
{
//	static int rtpNumber;
	static int rtcpNumber;
//	static char *fLastSessionId = NULL;
	int fSocketNum = socketNum;
	char* lineStart;
	char* cmd = NULL;
	unsigned int cmdSize; 
	unsigned const readBufSize = 10000;
	char *readBuffer;//[readBufSize+1]; 
	char* readBuf ;
	char cmdFmt[]= "SETUP %s RTSP/1.0\r\n"
			"CSeq: %d\r\n"
			"%s\r\n"
#ifdef _UDP
			"Transport: RTP/AVP;unicast;client_port=%d-%d\r\n"
#else
			"Transport: RTP/AVP/TCP;unicast\r\n"
#endif
			"Authorization: Basic cm9vdDpwYXNz\r\n\r\n";
	char* firstLine;
	char* nextLineStart;
	unsigned responseCode;
	int bytesRead;
	char *SessionId;
	char *Sessionstr;

	readBuffer = (char *)malloc((readBufSize+1)*sizeof(char));
	SessionId = (char *)malloc((readBufSize+1)*sizeof(char));
	if(fSocketNum<0) return -1;
	
	if(readBuffer == NULL||SessionId == NULL) return -1;
	memset(readBuffer,0,readBufSize+1);
	readBuf = readBuffer;
	do 
	{
		if (fLastSessionId != NULL) 
		{
			Sessionstr = (char *)malloc((20+strlen(fLastSessionId))*sizeof(char)); 
			if(Sessionstr == NULL)
			{
				fprintf(stderr,"Sessionstr failed to alloc the memory\n");
				free(readBuffer);
				free(SessionId);
				return -1;
			}
			sprintf(Sessionstr, "Session: %s\r\n", fLastSessionId);
		} 
		else 
		{
			Sessionstr = "";
		}
		rtcpNumber = rtpNumber + 1;
		cmdSize = strlen(cmdFmt)
				+ strlen(subsession->fControlPath)
				+ 20 /* max int len */
				+2*5 /* max port len */
				+strlen(Sessionstr)
				+ strlen(UserAgentHeaderStr);
		cmd = (char *)malloc((cmdSize+1)*sizeof(char));
		if(cmd == NULL)
		{
			fprintf(stderr,"cmd failed to alloc the memory\n");
			if(Sessionstr[0]!='\0') free(Sessionstr);
			free(readBuffer);
			free(SessionId);
			return -1;
		}
		memset(cmd,0,cmdSize);
		sprintf(cmd, cmdFmt,
			subsession->fControlPath,
			++fCSeq,
            UserAgentHeaderStr,
#ifdef _UDP
			rtpNumber, rtcpNumber,
#endif
			Sessionstr);
		//rtpNumber +=2;
#ifdef _DEBUG_PRINT
		fprintf(stderr,"SETUP command-%d:\n%s\n",fCSeq,cmd);
#endif
		if (send(fSocketNum,cmd,strlen(cmd),0)<0)
		{
			fprintf(stderr,"SETUP send() failed\n");
			break;
		}
		
		// Get the response from the server:
  
		bytesRead = getResponse(fSocketNum,readBuf, readBufSize);
		if (bytesRead <= 0) break;
#ifdef _DEBUG_PRINT
		fprintf(stderr,"SETUP response-%d:\n%s\n",fCSeq,readBuf);
#endif
		// Inspect the first line to check whether it's a result code 200
		firstLine = readBuf;
		nextLineStart = getLine(firstLine);
		if (parseResponseCode(firstLine, &responseCode)) break;
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle SETUP response\n");
			break;
		}
		while (1) 
		{
			lineStart = nextLineStart;
			if (lineStart == NULL||lineStart[0] == '\0') break;
			nextLineStart = getLine(lineStart);
			if (sscanf(lineStart, "Session: %s timeout=%d",SessionId,&Timeout) == 2)
			{
				subsession->fSessionId = strDup(SessionId);
				if(fLastSessionId!=NULL) free(fLastSessionId);
				fLastSessionId = strDup(SessionId);
				if(fLastSessionId[strlen(fLastSessionId)-1] ==';')
					fLastSessionId[strlen(fLastSessionId)-1] = 0;
				break;
			}	////////////////
		}
		if (subsession->fSessionId == NULL) 
		{
			fprintf(stderr,"Session header is missing in the response\n");
			break;
		}
		if(Sessionstr[0]!='\0') free(Sessionstr);
		free(SessionId);
		free(cmd);
		free(readBuffer);
		return 0;
	} while (0);

	if(Sessionstr[0]!='\0') free(Sessionstr);
	free(SessionId);
	free(cmd);
	free(readBuffer);
	return -1;
}

////////////////////////////

int getResponse(int socketNum,char* responseBuffer,unsigned responseBufferSize) 
{
	int fSocketNum;
	char *lastToCheck=NULL;
	char* p = NULL;//responseBuffer;
	int bytesRead = 0; // because we've already read the first byte
	unsigned bytesReadNow  = 0;
	fSocketNum = socketNum;
	if (responseBufferSize == 0) return 0; // just in case...
	*(responseBuffer) = '\0';
	while (bytesRead < (int)responseBufferSize) 
	{
		lastToCheck = NULL;
		if(blockUntilReadable(fSocketNum)<=0)
		{
			fprintf(stderr,"socket is unreadable\n");
			break;
		}
		bytesReadNow = recv(fSocketNum,(unsigned char*)(responseBuffer+bytesRead),1, 0);
		if (bytesReadNow != 1) 
		{
			fprintf(stderr,"RTSP response was truncated\n");
			break;
		}
		bytesRead++;

		lastToCheck = responseBuffer+bytesRead-4;
		if (lastToCheck < responseBuffer) continue;
		p = lastToCheck;
		if (*p == '\r' && *(p+1) == '\n' &&
						*(p+2) == '\r' && *(p+3) == '\n') 
		{
			*(responseBuffer+bytesRead)= '\0';
			// Before returning, trim any \r or \n from the start:
			while (*responseBuffer == '\r' || *responseBuffer == '\n'||*responseBuffer!=0x52) 
			{
				++responseBuffer;
				--bytesRead;
			}
			if(*responseBuffer == 0x52)//&&responseBuffer[1]=='T'&&responseBuffer[2]=='S'&&responseBuffer[3]=='P')
				return bytesRead;

		}
	}
	return bytesRead;
}
/////////////////////
char* parseSDPLine(char* inputLine)
{
	char *ptr;
	for (ptr = inputLine; *ptr != '\0'; ++ptr) 
	{
		if (*ptr == '\r' || *ptr == '\n') 
		{
			++ptr;
			while (*ptr == '\r' || *ptr == '\n') ++ptr;
			if (ptr[0] == '\0') ptr = NULL; // special case for end
			break;
		}
	}

	if (inputLine[0] == '\r' || inputLine[0] == '\n') return ptr;
	if (strlen(inputLine) < 2 || inputLine[1] != '='
		    || inputLine[0] < 'a' || inputLine[0] > 'z') 
	{
		fprintf(stderr,"Invalid SDP line \n");
		return NULL;
	}

	return ptr;
}

char * parseSDPAttribute_rtpmap(char* sdpLine,unsigned* rtpTimestampFrequency,unsigned *fnumChannels) 
{
	unsigned rtpmapPayloadFormat;
	unsigned numChannels = 1;
	char* codecName = strDupSize(sdpLine); // ensures we have enough space
	if (sscanf(sdpLine, "a=rtpmap: %u %[^/]/%u/%u",
			&rtpmapPayloadFormat, codecName, rtpTimestampFrequency,
			&numChannels) == 4
		|| sscanf(sdpLine, "a=rtpmap: %u %[^/]/%u",
			&rtpmapPayloadFormat, codecName, rtpTimestampFrequency) == 3
		|| sscanf(sdpLine, "a=rtpmap: %u %s",
			&rtpmapPayloadFormat, codecName) == 2
		|| sscanf(sdpLine, "a=rtpmap: %u %s/%u",
			&rtpmapPayloadFormat, codecName, rtpTimestampFrequency) == 3
		)
	{
		*fnumChannels = numChannels;  
		return strDup(codecName);

	}
	free(codecName);

	return NULL;
}

char * parseSDPAttribute_control(char* sdpLine) 
{
  
	char* controlPath = strDupSize(sdpLine); // ensures we have enough space
	if (sscanf(sdpLine, "a=control:%s", controlPath) == 1) 
	{
		return strDup(controlPath);
	}
	free(controlPath);

	return NULL;
}

int parseSDPAttribute_range(char* sdpLine) 
{
	int parseSuccess = -1;
	char playEndTime[32];
	char temp[32];
	int index = 0;
	int j = 0;
	unsigned long endtime = 0;
	char *endstr;
	int k;
	int i,num;
	if (sscanf(sdpLine, "a=range:npt=0-  %s", playEndTime)==1) 
	{
		parseSuccess = 0;
		for(index = 0;index<strlen(playEndTime);index++)
		{
			if(playEndTime[index]!='.')
			{
				temp[j] = playEndTime[index];
				j++;
			}
			else
			{
				for(k=index+1;k<strlen(playEndTime);k++)
				{
					temp[j] = playEndTime[k];
					j++;
					PotPos++;
				}
				break;
			}

		}
		temp[j] = '\0';
		
		
		endtime = strtoul(temp,&endstr,10);

		if (endtime > fMaxPlayEndTime) 
		{
			if(PotPos>=3)
			{
				num = 1;
				for(i = 0;i<PotPos-3;i++)
				{
					num = 10*num;
				}
				fMaxPlayEndTime = endtime/num;
			}
			else
			{
				num = 1;
				for(i = 0;i<3-PotPos;i++)
				{
					num = 10*num;
				}
				fMaxPlayEndTime = endtime*num;
			}
			PotPos = 1000;
			
		}
		fprintf(stderr,"fMaxPlayEndTime is %lu\n",fMaxPlayEndTime);
	}

	return parseSuccess;
}


char * parseSDPAttribute_fmtp(char* sdpLine) 
{
	char* lineCopy;
	char* line;
	char* valueStr;
	char* c;
	do 
	{
		if (strncmp(sdpLine, "a=fmtp:", 7) != 0) break; 
		sdpLine += 7;
		while (isdigit(*sdpLine)) ++sdpLine;
		lineCopy = strDup(sdpLine); 
		line = lineCopy;
		for (c = line; *c != '\0'; ++c) *c = tolower(*c);
    
		while (*line != '\0' && *line != '\r' && *line != '\n') 
		{
			valueStr = strDupSize(line);
			if (sscanf(line, " config = %[^; \t\r\n]", valueStr) == 1) 
			{
				free(lineCopy);
				
				return strDup(valueStr);
      			}
      			free(valueStr);

			while (*line != '\0' && *line != '\r' && *line != '\n'&& *line != ';') ++line;
			while (*line == ';') ++line;
		}
		free(lineCopy);
	} while (0);
	return NULL;
}


struct MediaAttribute * GetMediaAttrbute(struct MediaAttribute *Attribute,struct MediaSubsession * subsession,int subsessionNum)
{
	struct MediaSubsession *mediasub;
	int len = 0;
	mediasub = subsession;
	while(subsessionNum>0)
	{
		if(strcmp(mediasub->fCodecName,"MP4V-ES") == 0)
		{	
			Attribute->fVideoFrequency = mediasub->frtpTimestampFrequency;
			Attribute->fVideoPayloadFormat = mediasub->fRTPPayloadFormat;
			if(mediasub->fConfig){
			len = strlen(mediasub->fConfig);
			memcpy(Attribute->fConfigAsc,mediasub->fConfig,len);
			a_hex(mediasub->fConfig,Attribute->fConfigHex,len);
			Attribute->fConfigHexLen = len/2;
			parse_vovod(Attribute->fConfigHex,Attribute->fConfigHexLen,&(Attribute->fVideoFrameRate),&(Attribute->fTimeIncBits),&(Attribute->fVideoWidth),&(Attribute->fVideoHeight));
			}
			Attribute->fixed_vop_rate = fixed_vop_rate;
			VFrameRate = Attribute->fVideoFrameRate;
		}
		else if(strcmp(mediasub->fCodecName,"MPA") == 0||strcmp(mediasub->fCodecName,"mpeg4-generic") == 0)
		{	
			Attribute->fAudioFrequency = mediasub->frtpTimestampFrequency;
			Attribute->fAudioPayloadFormat = mediasub->fRTPPayloadFormat;
			Attribute->fTrackNum = mediasub->fNumChannels;
		}	
		mediasub = mediasub->fNext;
		subsessionNum--;
	}
	return Attribute;
}

struct MediaSubsession * initializeWithSDP(char* sdpDescription,int *SubsessionNum)
{
 

	int Num = 0;
	char*  sdpLine = sdpDescription;
	char* nextSDPLine = NULL;
	struct MediaSubsession *fSubsessionsHead;
	struct MediaSubsession *fSubsessionsTail;
	struct MediaSubsession* subsession;
	char* mediumName;
	char * CodecName;
	char * ControlPath;
	char *Config;
	unsigned payloadFormat;
	if (sdpDescription == NULL) return NULL;
	fSubsessionsHead = fSubsessionsTail = NULL;
	while (1) 
	{
		if ((nextSDPLine = parseSDPLine(sdpLine)) == NULL)
		{
			return NULL;		
		}
		if (sdpLine[0] == 'm') 
		{
			Num++;
			break;
		}
		sdpLine = nextSDPLine;
		if (sdpLine == NULL) break; // there are no m= lines at all 
		if (!parseSDPAttribute_range(sdpLine)) continue;//check a=range:npt=
	}
    
	while (sdpLine != NULL) 
	{
		subsession = (struct MediaSubsession*)malloc(sizeof(struct MediaSubsession));
		if (subsession == NULL) 
		{
			fprintf(stderr,"Unable to create new MediaSubsession\n");
			return NULL;
		}

		if (fSubsessionsTail == NULL) 
		{
			fSubsessionsHead = fSubsessionsTail = subsession;
		} 
		else 
		{
			fSubsessionsTail->fNext = subsession;
			subsession->fNext = NULL;
			fSubsessionsTail = subsession;
			
		}

		mediumName = strDupSize(sdpLine);
    
		if (sscanf(sdpLine, "m=%s %hu RTP/AVP %u",mediumName, &subsession->fClientPortNum, &payloadFormat) != 3
			|| payloadFormat > 127) 
		{
			fprintf(stderr,"Bad SDP line\n");
			free(mediumName);
			return NULL;
		}
    
		subsession->fMediumName = strDup(mediumName);
		free(mediumName);
		subsession->fRTPPayloadFormat = payloadFormat;
#ifdef _DEBUG_PRINT
		fprintf(stderr, "Medium name: %s\n",subsession->fMediumName);
		fprintf(stderr, "RTP Payload: %d\n",subsession->fRTPPayloadFormat);
#endif
		while (1) 
		{
			sdpLine = nextSDPLine;//a=rtpmap...
			if (sdpLine == NULL) break; // we've reached the end
			if ((nextSDPLine = parseSDPLine(sdpLine)) == NULL)
			{
				//*SubsessionNum = Num;
				//return fSubsessionsHead;			
			}
			if (sdpLine[0] == 'm') 
			{
				Num ++;
				break; // we've reached the next subsession
			}
			// Check for various special SDP lines that we understand:
			CodecName = strDupSize(sdpLine);
			if ((CodecName = parseSDPAttribute_rtpmap(sdpLine,&(subsession->frtpTimestampFrequency),&(subsession->fNumChannels)))!=NULL)
			{
				subsession->fCodecName = strDup(CodecName);
				if(strcmp(CodecName,"MP4V-ES") == 0)
				{	
					VTimestampFrequency = subsession->frtpTimestampFrequency;
					VPayloadType = subsession->fRTPPayloadFormat;
				}
				else if(strcmp(CodecName,"H264") == 0||strcmp(CodecName,"h264") == 0)
				{	
					ATimestampFrequency = subsession->frtpTimestampFrequency;
					APayloadType = subsession->fRTPPayloadFormat;
				}
				free(CodecName);
#ifdef _DEBUG_PRINT
				fprintf(stderr, "Timestamp Frequency: %d\n",subsession->frtpTimestampFrequency);
				fprintf(stderr, "Payload Type: %d\n",subsession->fRTPPayloadFormat);
#endif
				continue;
			}
			ControlPath = strDupSize(sdpLine);
			if ((ControlPath = parseSDPAttribute_control(sdpLine))!=NULL)
			{
				subsession->fControlPath = strDup(ControlPath);
				free(ControlPath);
#ifdef _DEBUG_PRINT
				fprintf(stderr, "Control Path: %s\n",subsession->fControlPath);
#endif
				continue;
			}
	
			Config = strDupSize(sdpLine);
			if ((Config = parseSDPAttribute_fmtp(sdpLine))!=NULL)
			{
				subsession->fConfig = strDup(Config);
				free(Config);
				continue;
			}
		}
	}
	
	*SubsessionNum = Num;
	return fSubsessionsHead;
  
}
int blockUntilwriteable(int socket)//, struct timeval* timeout)
{
	int result = -1;
	static unsigned numFds;
	fd_set wd_set;
	do 
	{
		FD_ZERO(&wd_set);
		if (socket < 0) break;
		FD_SET((unsigned) socket, &wd_set);
		numFds = socket+1;
		
		result = select(numFds, NULL, &wd_set, NULL,0);// timeout);
		if (result == 0)
		{
			break; // this is OK - timeout occurred
		}
		else if(result <= 0) 
		{
			fprintf(stderr, "select() error\n ");
			break;
		}

		if (!FD_ISSET(socket, &wd_set)) 
		{
			fprintf(stderr, "select() error - !FD_ISSET\n");
			break;
		}
	} while (0);
	return result;
};

int blockUntilReadable(int socket)//, struct timeval* timeout) 
{
	fd_set rd_set;
	int result = -1;
	static unsigned numFds;
	
	do 
	{
		FD_ZERO(&rd_set);
		if (socket < 0) break;
		FD_SET((unsigned) socket, &rd_set);
		numFds = socket+1;
		result = select(numFds, &rd_set, NULL, NULL,0);
		if (result == 0) 
		{
			break; // this is OK - timeout occurred
		} 
		else if (result <= 0) 
		{
			fprintf(stderr, "select() error\n ");
			break;
		}
    
		if (!FD_ISSET(socket, &rd_set)) 
		{
			fprintf(stderr, "select() error - !FD_ISSET\n");
			break;
		}	
	} while (0);

	return result;
}


int getSDPDescriptionFromURL(int socketNum,char* url,char* Description, int serviceLevel)
{
	int fSocketNum;
	char *readBuffer;//[MAX_READBUFSIZE+1]; 
	char* readBuf;

	int bytesRead;

	char* cmdFmt = "DESCRIBE %s?videocodec=mpeg4&resolution=%s&fps=%d RTSP/1.0\r\n"
				"CSeq: %d\r\n"
				"%s\r\n"
				"Accept: application/sdp\r\n"
				"Authorization: Basic cm9vdDpwYXNz\r\n\r\n";
	unsigned cmdSize = 0;										
  	char* cmd = NULL;
	char* firstLine;
	char* nextLineStart;
	unsigned responseCode;
	int contentLength = -1;
	char* lineStart;  
	unsigned numExtraBytesNeeded;
	char* ptr;
	int bytesRead2;
	char *resolution;
	int  fps;

	readBuffer = (char *)malloc((MAX_READBUFSIZE+1)*sizeof(char));
	if(readBuffer == NULL)
	{
		fprintf(stderr,"failed to alloc the memory\n");
    		return -1;
	}
	memset(readBuffer,0,MAX_READBUFSIZE+1);
	readBuf =readBuffer;
	
	//fprintf(stderr,"start alloc cmd\n");
	cmdSize = strlen(cmdFmt)
				+ strlen(url)
				+ 20 /* max int len */
				+ 10 /* resolution */
				+ 10 /* fps */
				+ strlen(UserAgentHeaderStr);
	cmd = (char *)malloc((cmdSize+1)*sizeof(char));
	if(cmd ==NULL)
	{
		fprintf(stderr,"failed to alloc the memory\n");
    		free(readBuffer);
    		return -1;
	}
	//fprintf(stderr,"alloc cmd successful\n");
	memset(cmd,0,cmdSize+1);
	fSocketNum = socketNum;

	resolution = get_resolution(serviceLevel);
	fps=get_fps(serviceLevel);

	do 
	{  
 
		// Send the DESCRIBE command:
		//fprintf(stderr,"Send the DESCRIBE command:\n");
		sprintf(cmd, cmdFmt,url,resolution,fps,++fCSeq,UserAgentHeaderStr);
		//����description ����
		//fprintf(stderr,"start blockUntilwriteable\n");
  		if(blockUntilwriteable(fSocketNum)<=0)
  		{ 
  			fprintf(stderr,"socket is unwriteable\n");
  			break;
  		}
#ifdef _DEBUG_PRINT
  		fprintf(stderr,"\n%s\n",cmd);
#endif
  		if (send(fSocketNum, cmd, strlen(cmd), 0)<0) 
		{
			fprintf(stderr,"DESCRIBE send() failed\n ");
			break;
		}
		//fprintf(stderr,"DESCRIBE send() successful\n");
			// Get the response from the server:
		//fprintf(stderr,"Get the response from the server\n");
		bytesRead = getResponse(fSocketNum,readBuf, MAX_READBUFSIZE);
		if (bytesRead <= 0) break;
#ifdef _DEBUG_PRINT
		fprintf(stderr,"DESCRIBE response-%d:\n%s\n",fCSeq,readBuf);
#endif
		//Received DESCRIBE response
    
		// Inspect the first line to check whether it's a result code that
		// we can handle.

		firstLine = readBuf;
		nextLineStart = getLine(firstLine);
		if (sscanf(firstLine, "%*s%u", &responseCode) != 1) 
		{
			fprintf(stderr,"no response code in line\n");
			break;
		}
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle DESCRIBE response\n");
			break;
		}

		// Skip every subsequent header line, until we see a blank line
		// The remaining data is assumed to be the SDP descriptor that we want.
		// We should really do some checking on the headers here - e.g., to
		// check for "Content-type: application/sdp", "Content-base",
		// "Content-location", "CSeq", etc. #####
		//fprintf(stderr,"start get Content-Length\n");
		while (1) 
		{
			lineStart = nextLineStart;
			if (lineStart == NULL||lineStart[0] == '\0') break;

			nextLineStart = getLine(lineStart);
			if (sscanf(lineStart, "Content-Length: %d", &contentLength) == 1
				|| sscanf(lineStart, "Content-length: %d", &contentLength) == 1) 
			if(contentLength>0)
			{
				if (contentLength < 0) 
				{
					fprintf(stderr,"Bad Content-length\n");
					break;
				}
				// Use the remaining data as the SDP descr, but first, check
				// the "Content-length:" header (if any) that we saw.  We may need to
				// read more data, or we may have extraneous data in the buffer.
				//bodyStart = nextLineStart;
				// We saw a "Content-length:" header
				
				//numBodyBytes = &readBuf[bytesRead] - bodyStart;
				//if (contentLength > (int)numBodyBytes) 
				//{
				// We need to read more data.  First, make sure we have enough
				// space for it:
				numExtraBytesNeeded = contentLength;// - numBodyBytes;
			
				// Keep reading more data until we have enough:
				bytesRead = 0;
				//fprintf(stderr,"start recv %d\n",contentLength);
				while (numExtraBytesNeeded > 0) 
				{
					if(blockUntilReadable(fSocketNum)<=0)
    					{
    						fprintf(stderr,"socket is unreadable\n");
    						break;
    					}
    					//fprintf(stderr,"start recv\n");
					ptr = Description+bytesRead;
					//ptr = &readBuf[bytesRead];
					bytesRead2 = recv(fSocketNum, (unsigned char*)ptr,numExtraBytesNeeded,0);
					if (bytesRead2 <0) break;
					ptr[bytesRead2] = '\0';
						

					bytesRead += bytesRead2;
					numExtraBytesNeeded -= bytesRead2;
				}
				if (numExtraBytesNeeded > 0) break; // one of the reads failed
			
				free(readBuffer);
				free(cmd);
				return 0;
			}
		}
	}while(0);
	free(readBuffer);
	free(cmd);
	Description = NULL;
	return -1;
}

char* getLine(char* startOfLine) 
{
	// returns the start of the next line, or NULL if none
	char* ptr;
	for (ptr = startOfLine; *ptr != '\0'; ++ptr) 
	{
		if (*ptr == '\r' || *ptr == '\n') 
		{
			// We found the end of the line
			*ptr++ = '\0';
			if (*ptr == '\n') ++ptr;
			return ptr;
		}
	}

	return NULL;
}

int openConnectionFromURL(char* url)
{
	struct hostent *hp;
	char address[100];
	int destPortNum;
	struct sockaddr_in server;
	int fSocketNum = -1;
	if (url == NULL) return -1;
	memset(address,0,100);
	if (parseRTSPURL(url, address, &destPortNum)) return -1;
  
	fSocketNum = socket(AF_INET, SOCK_STREAM, 0);
  
	if (fSocketNum < 0) 
	{
		fprintf(stderr,"Client: Error Opening socket\n");
		return -1;
	}
	hp = gethostbyname(address);
	if (hp == NULL ) 
	{
		fprintf(stderr,"Client: Cannot resolve address [%s]\n",address);
		return -1;
	}
	
	memset(&server,0,sizeof(struct sockaddr_in));
	memcpy(&(server.sin_addr),hp->h_addr,hp->h_length);
	server.sin_family = AF_INET;
	server.sin_port = htons((unsigned short)destPortNum);
	if (connect(fSocketNum, (struct sockaddr*)&server, sizeof(struct sockaddr_in))!= 0) 
	if (connect(fSocketNum, (struct sockaddr*)&server, sizeof(struct sockaddr_in))!= 0) 
	{
		fprintf(stderr,"connect() failed\n");
		//closesocket(fSocketNum);
		close(fSocketNum);
		return -1;
	}
//fprintf(stderr,"connect() successful \n");
	return fSocketNum;
}

int parseRTSPURL(char* url,char * address,int* portNum) 
{
	
	char const* prefix = "rtsp://";
	unsigned const prefixLength = 7;
	char* from = NULL ;
	char* to = NULL;
	unsigned i;
	char nextChar;
	if (strncmp(url, prefix, prefixLength) != 0) 
	{
		fprintf(stderr,"URL is not of the form\n");
		return -1;
	}
		
	from = &url[prefixLength];
	to = &address[0];
	for (i = 0; i < parseBufferSize; ++i) 
	{
		if (*from == '\0' || *from == ':' || *from == '/') 
		{
	// We've completed parsing the address
			*to = '\0';
			break;
		}
		*to++ = *from++;
	}
	if (i == parseBufferSize) 
	{
		fprintf(stderr,"URL is too long\n");
		return -1;
	}

 	*portNum = 554; // default value
	nextChar = *from;
	if (nextChar == ':') 
	{
		int portNumInt;
		if (sscanf(++from, "%d", &portNumInt) != 1) 
		{
			fprintf(stderr,"No port number follows :%d\n",portNumInt);
			return -1;
		}
		if (portNumInt < 1 || portNumInt > 65535) 
		{
			fprintf(stderr,"Bad port number\n");
			return -1;
		}
		*portNum = portNumInt;
	}
	//fprintf(stderr,"address is %s;portNum is %d \n",address,*portNum);
	return 0;
}

////////////////////////
void resumeStreams(int socketNum)
{
	//double start;
	//start = (double)(vloopNum*1000)/(double)(VFrameRate);
	long start;
	start = vloopNum*1000/VFrameRate;

	//if(playMediaSession(socketNum,start,(double)fMaxPlayEndTime/(double)1000)) 
	if(playMediaSession(socketNum,start,-1)) 
	{	
			fprintf(stderr,"Play MediaSubsession Failed\n");
			exit(0);
	}
	fprintf(stderr,"Play Streams successful\n");
}

int playMediaSession(int socketNum,int start,int end)//double start, double end)
{
	char* cmd = NULL;
	char cmdFmt[] =
			"PLAY %s?videocodec=mpeg4 RTSP/1.0\r\n"
			"CSeq: %d\r\n"
			"%s\r\n"
			"Session: %s\r\n"
			//"Range: \r\n"
			//"Range: npt=%s-%s\r\n"
			"Authorization: Basic cm9vdDpwYXNz\r\n\r\n";
	char startStr[30], endStr[30];
	unsigned cmdSize;
	unsigned const readBufSize = 10000;
	char *readBuffer;
	char* readBuf;
	int bytesRead;
	char* firstLine;
	char* nextLineStart;
	unsigned responseCode;
	readBuffer = (char*)malloc(sizeof(char)*(readBufSize+1));
	if(readBuffer == NULL) return -1;
	memset(readBuffer,0,readBufSize+1);
	do 
	{
		// First, make sure that we have a RTSP session in progress
		if (fLastSessionId == NULL) 
		{
			fprintf(stderr,"No RTSP session is currently in progress\n");
			break;
		}

		// Send the PLAY command:

		// First, construct an authenticator string:
//		sprintf(startStr, "%.3f", start); 
//sprintf(endStr, "%.3f", end);
		sprintf(startStr,"%d",start);
		sprintf(endStr,"%d",end);
		
		if (start==-1) startStr[0]='\0';
		if (end == -1) endStr[0] = '\0';
			
		cmdSize = strlen(cmdFmt)
			+ strlen(fBaseURL)
		/*	+ strlen(subsession->fControlPath) */
			+ 20 /* max int len */
			+ strlen(fLastSessionId)
			//+ strlen(startStr) + strlen(endStr)
			+ strlen(UserAgentHeaderStr);
		cmd = (char*)malloc(sizeof(char)*cmdSize);
		if(cmd == NULL) 
		{
			free(readBuffer);
			return -1;
		}
		memset(cmd,0,cmdSize);
		sprintf(cmd, cmdFmt,
			fBaseURL, /* subsession->fControlPath, */
			++fCSeq,
			UserAgentHeaderStr,
			fLastSessionId
			/*startStr, endStr,*/
			);
#ifdef _DEBUG_PRINT
		fprintf(stderr,"PLAY command-%d:\n%s\n",fCSeq,cmd);
#endif
		if (send(socketNum,cmd,strlen(cmd),0)<0) 
		{
			fprintf(stderr,"PLAY send() failed\n ");
			break;
		}
		readBuf = readBuffer;
		// Get the response from the server:
		bytesRead = getResponse(socketNum,readBuf, readBufSize);

		if (bytesRead <= 0)
		{
			fprintf(stderr,"getResponse failed: %d\n",bytesRead);
			break;
		}
#ifdef _DEBUG_PRINT
		fprintf(stderr,"PLAY response-%d:\n%s\n",fCSeq,readBuf);
#endif
		
		// Inspect the first line to check whether it's a result code 200
		firstLine = readBuf;
		nextLineStart = getLine(firstLine);
		if (parseResponseCode(firstLine,&responseCode)) break;
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle PLAY response\n ");
			break;
		}

		free(cmd);
		free(readBuffer);
		return 0;
	} while (0);

	free(cmd);
	free(readBuffer);
	return -1;
}

/*int getPauseResponse(int socketNum,char* responseBuffer,unsigned responseBufferSize) 
{
	int fSocketNum;
	char *lastToCheck=NULL;
	char* p = NULL;//responseBuffer;
	int bytesRead = 0; // because we've already read the first byte
	unsigned bytesReadNow	= 0;
	fSocketNum = socketNum;

	if (responseBufferSize == 0) return 0; // just in case...
	//responseBuffer[0] = '\0'; // ditto
	*(responseBuffer) = '\0';

	// Keep reading data from the socket until we see "\r\n\r\n" (except
	// at the start), or until we fill up our buffer.
	// Don't read any more than this.
	while (bytesRead < (int)responseBufferSize) 
	{
		lastToCheck = NULL;
		if(blockUntilReadable(fSocketNum)<=0)
		{
			fprintf(stderr,"socket is unreadable\n");
			break;
		}
		bytesReadNow = recv(fSocketNum,(unsigned char*)(responseBuffer+bytesRead),1, 0);
		if (bytesReadNow != 1) 
		{
			fprintf(stderr,"RTSP response was truncated\n");
			break;
		}
		bytesRead++;
		
		// Check whether we have "\r\n\r\n":
	
		lastToCheck = responseBuffer+bytesRead-4;
		if (lastToCheck < responseBuffer) continue;
		p = lastToCheck;
		if (*p == '\r' && *(p+1) == '\n' &&
						*(p+2) == '\r' && *(p+3) == '\n') 
		{
			*(responseBuffer+bytesRead)= '\0';
			// Before returning, trim any \r or \n from the start:
			while (*responseBuffer == '\r' || *responseBuffer == '\n') 
			{
				++responseBuffer;
				--bytesRead;
			}
			if(strncmp(responseBuffer,"RTSP",4)==0)
				return bytesRead;
		}
	}
	return bytesRead;
}*/

/////////////


int pauseMediaSession(int socketNum)
{
	char* cmd = NULL;
	char cmdFmt[] =
			"PAUSE %s RTSP/1.0\r\n"
			"CSeq: %d\r\n"
			"%s\r\n"
			"Session: %s\r\n\r\n";
	unsigned cmdSize;
	unsigned readBufSize = 10000;
	char* readBuffer; 
	char* readBuf;
	int bytesRead;
	char* firstLine;
	unsigned responseCode;
	readBuffer = (char*)malloc(sizeof(char)*(readBufSize+1));
	if(readBuffer == NULL) return -1;
	memset(readBuffer,0,readBufSize+1);

	do 
	{
		// First, make sure that we have a RTSP session in progress
		if (fLastSessionId == NULL) 
		{
			fprintf(stderr,"No RTSP session is currently in progress\n");
			break;
		}
		
		// Send the PAUSE command:
		
		// First, construct an authenticator string:
		cmdSize = strlen(cmdFmt)
			+ strlen(fBaseURL)//+strlen(subsession->fControlPath)
			+ 20 /* max int len */
			+ strlen(fLastSessionId)
			+ strlen(UserAgentHeaderStr);
		cmd = (char*)malloc(sizeof(char)*cmdSize);
		if(cmd == NULL) 
		{
			free(readBuffer);
			return -1;
		}
		memset(cmd,0,cmdSize);
		sprintf(cmd, cmdFmt,
			fBaseURL,
			//subsession->fControlPath,
			++fCSeq,
			UserAgentHeaderStr,
			fLastSessionId);
#ifdef _DEBUG_PRINT
		fprintf(stderr,"PAUSE command-%d:\n%s\n",fCSeq,cmd);
#endif
		
		if (send(socketNum,cmd,strlen(cmd),0)<0) 
		{
			fprintf(stderr,"PAUSE send() failed!\n ");
			break;
		}
		
		// Get the response from the server:
		readBuf = readBuffer;
		bytesRead = getResponse(socketNum,readBuf, readBufSize);

		if (bytesRead <= 0) break;
#ifdef _DEBUG_PRINT		
		fprintf(stderr,"bytesRead is %d\n",bytesRead);
		fprintf(stderr,"PAUSE response-%d:\n%s\n",fCSeq,readBuf);
#endif
		// Inspect the first line to check whether it's a result code 200
		firstLine = readBuf;
		/*char* nextLineStart =*/ getLine(firstLine);
		
		if (parseResponseCode(firstLine,&responseCode)) break;
		
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle PAUSE response\n ");
			break;
		}
		// (Later, check "CSeq" too #####)
		
		free(cmd);
		free(readBuffer);
		fprintf(stderr,"Pause Streams successful\n");
		return 0;
	} while (0);
	
	free(cmd);
	free(readBuffer);
	fprintf(stderr,"Pause Streams failed\n");
	return -1;
}


///////////////////////////
int teardownMediaSession(int socketNum) 
{
	char* cmd = NULL;
	char* const cmdFmt = "TEARDOWN %s RTSP/1.0\r\n"
				"CSeq: %d\r\n"
				"%s\r\n"
				"Session: %s\r\n"
				"\r\n";
	unsigned cmdSize;
	unsigned readBufSize = 10000;
	char* readBuffer; 
	char* readBuf;
	char* firstLine;
	unsigned responseCode;
	int bytesRead;
	readBuffer = (char *)malloc((readBufSize+1)*sizeof(char));
	if(readBuffer == NULL) return -1;
	memset(readBuffer,0,readBufSize+1);
	readBuf = readBuffer;
	do 
	{
		if (fLastSessionId == NULL) 
		{
			fprintf(stderr,"No RTSP session is currently in progress\n");
			break;
		}
		
		// Send the TEARDOWN command:

		// First, construct an authenticator string:
		
		cmdSize = strlen(cmdFmt)
			+ strlen(fBaseURL)
			+ 20 /* max int len */
			+ strlen(fLastSessionId)
			+ strlen(UserAgentHeaderStr);
		cmd = (char*)malloc(cmdSize*sizeof(char));
		if(cmd == NULL) return -1;
		memset(cmd,0,cmdSize);
		sprintf(cmd, cmdFmt,
			fBaseURL,
			++fCSeq,
			UserAgentHeaderStr,
			fLastSessionId);
#ifdef _DEBUG_PRINT
		fprintf(stderr,"TEARDOWN command-%d:\n%s\n",fCSeq,cmd);
#endif
		if (send(socketNum,cmd,strlen(cmd),0)<0)
		{
			fprintf(stderr,"TEARDOWN send() failed\n ");
			break;
		}

		// Get the response from the server:
		
		bytesRead = getResponse(socketNum,readBuf, readBufSize);
		if (bytesRead <= 0) break;
#ifdef _DEBUG_PRINT
		fprintf(stderr,"TEARDOWN response-%d:\n%s\n",fCSeq,readBuf);
#endif
		// Inspect the first line to check whether it's a result code 200
		firstLine = readBuf;
		/*char* nextLineStart =*/ getLine(firstLine);
		if (parseResponseCode(firstLine,&responseCode)) break;
		if (responseCode != 200) 
		{
			fprintf(stderr,"cannot handle TEARDOWN response\n ");
			break;
		}
		
		free(readBuffer);
		free(cmd);
		return 0;
	} while (0);

	free(readBuffer);
	free(cmd);
	return -1;
}


char* strDup(char* str) 
{
	char* copy;
	unsigned int len;
	if (str == NULL) return NULL;
	len = strlen(str) + 1;
	copy = (char*)malloc(len*sizeof(char));

	if (copy != NULL) {
		memcpy(copy, str, len);
	}
	return copy;
}

char* strDupSize(char* str) 
{
	char* copy;
	unsigned int len;
	if (str == NULL) return NULL;
	len = strlen(str) + 1;
	copy = (char *)malloc(len*sizeof(char));

	return copy;
}

int hex_a(unsigned char *hex, char *a,unsigned char length)
{
	unsigned char hLowbit,hHighbit;
	int i;

	for(i=0;i<length*2;i+=2)
	{
		hLowbit=hex[i/2]&0x0f;
		hHighbit=hex[i/2]/16;
		if (hHighbit>=10) a[i]=hHighbit+'7';
		else a[i]=hHighbit+'0';
		if (hLowbit>=10) a[i+1]=hLowbit+'7';
		else a[i+1]=hLowbit+'0';
	}
	a[length*2]='\0';
	return 0;
}


int a_hex( char *a,unsigned char *hex,unsigned char len)
{
	 short i;
	 unsigned char aLowbit,aHighbit;
	 unsigned char hconval,lconval;

	 for(i=0;i<len;i+=2)
	 {
		aHighbit=toupper(a[i]);
		if ((aHighbit>='A')&&(aHighbit<='F')) 
			hconval='7';
		else
			 if ((aHighbit>='0')&&(aHighbit<='9')) 
			 	hconval='0';
			 else	
			 	return -1;
		aLowbit=toupper(a[i+1]);
		if ((aLowbit>='A')&&(aLowbit<='F')) 
			lconval='7';
		else
			 if ((aLowbit>='0')&&(aLowbit<='9')) 
			 	lconval='0';
			 else 
			 	return -1;
		hex[(i/2)]=((aHighbit-hconval)*16+(aLowbit-lconval));
	 }
	 hex[len/2]=0x00;
	 return 0;
}

/************************************************************************************/
/**********
This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the
Free Software Foundation; either version 2.1 of the License, or (at your
option) any later version. (See <http://www.gnu.org/copyleft/lesser.html>.)

This library is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
more details.
**********/


#include <stdio.h>
#include <memory.h>
#include <stdlib.h>         /* For _MAX_PATH definition */
#include <malloc.h>

/////////////////////////////////////
uint32_t *bitstream;
///////////////////////
/* reads n bits from bitstream without changing the stream pos */

uint32_t BitstreamShowBits(Bitstream * bs,uint32_t bits)
{
	int nbit = (bits + bs->pos) - 32;
	uint32_t ret_value;
	
	if (nbit > 0) 
	{
		ret_value =  ((bs->bufa & (0xffffffff >> bs->pos)) << nbit) |
				(bs->bufb >> (32 - nbit));
	}
	else 
	{
		ret_value = (bs->bufa & (0xffffffff >> bs->pos)) >> (32 - bs->pos - bits);
	}
//	fprintf(stderr,"show 2 bufa :%x; bufb :%x ;pos :%u;\n",bs->bufa,bs->bufb,bs->pos);
//	fprintf(stderr,"showbits %d %x\n", bits, ret_value);
	
	return ret_value;
}

/* skip n bits forward in bitstream */

void BitstreamSkip(Bitstream * bs,uint32_t bits)
{
	uint32_t tmp;
	bs->pos += bits;
	if (bs->pos >= 32) 
	{
		bs->bufa = bs->bufb;
		tmp = *((uint32_t *)bs->tail + 2);
		bs->bufb = tmp;
		bs->tail++;
		bs->pos -= 32;
	}
//	fprintf(stderr,"skip 2 bufa :%x; bufb :%x ;pos :%u;\n",bs->bufa,bs->bufb,bs->pos);
	//fprintf(stderr,"used %d\n", bits);
}


/* bitstream length (unit bits) */

uint32_t BitstreamPos(Bitstream * bs)
{
    return 8 * ((uint32_t)bs->tail - (uint32_t)bs->start) + bs->pos;
}


/*	flush the bitstream & return length (unit bytes)
	NOTE: assumes no futher bitstream functions will be called.
 */

uint32_t BitstreamLength(Bitstream * bs)
{
	uint32_t len = (uint32_t) bs->tail - (uint32_t) bs->start;
	uint32_t b;
    if (bs->pos)
    {
		b = bs->buf;
		*bs->tail = b;

		len += (bs->pos + 7) / 8;
    }

	return len;
}


/* read n bits from bitstream */

uint32_t BitstreamGetBits(Bitstream * bs,uint32_t n)
{
	uint32_t ret = BitstreamShowBits(bs, n);
	BitstreamSkip(bs, n);
	return ret;
}


/* read single bit from bitstream */

uint32_t BitstreamGetBit(Bitstream * const bs)
{
	return BitstreamGetBits(bs, 1);
}


////////////////////////////////////


int log2bin(int value)
{
	int n = 0;
	while (value)
	{
		value >>= 1;
		n++;
	}
	return n;
}


static const uint32_t intra_dc_threshold_table[] =
{
	32,	/* never use */
	13,
	15,
	17,
	19,
	21,
	23,
	1,
};

////////////////////////////
void BitstreamByteAlign(Bitstream *bs)
{
	uint32_t remainder = bs->pos % 8;
	if (remainder)
	{
		BitstreamSkip(bs, 8 - remainder);
	}
}

/*
decode headers
returns coding_type, or -1 if error
*/

int BitstreamReadHeaders(Bitstream * bs, XVID_DEC_PARAM * param,int findvol)
{
	uint32_t rounding; 
	uint32_t quant;
	uint32_t fcode;
	uint32_t intra_dc_threshold;
	static uint32_t vol_ver_id = 0;
	static uint32_t time_inc_resolution = 0;
	static uint32_t time_inc_bits = 0;
	static uint32_t coding_type = 0;
	static uint32_t fixed_vop_time_increment = 0;
	uint32_t start_code;
	static uint32_t width = 0, height = 0;
	static uint32_t shape = 0;
	static uint32_t interlacing = 0;
	static uint32_t top_field_first = 0;
	static uint32_t alternate_vertical_scan = 0;
	static uint32_t quant_bits = 0;
	static uint32_t quant_type = 0;
	static uint32_t quarterpel = 0;
	static uint32_t have_short_header = 0;
	int hours, minutes, seconds;
	uint32_t temp_width=0, temp_height=0;
	uint32_t horiz_mc_ref, vert_mc_ref;
	uint32_t source_format;
	

	do
	{
		BitstreamByteAlign(bs);
		start_code = BitstreamShowBits(bs, 32);
//		fprintf(stderr,"1 start_code is %x\n",start_code);
		//BitstreamSkip(bs,32);
		
		if (start_code == VISOBJSEQ_START_CODE)
		{
			BitstreamSkip(bs, 32);				// visual_object_sequence_start_code
			BitstreamSkip(bs, 8);					// profile_and_level_indication
		}
		else if (start_code == VISOBJSEQ_STOP_CODE)
		{
			BitstreamSkip(bs, 32);				// visual_object_sequence_stop_code
		}
		else if (start_code == VISOBJ_START_CODE)
		{
			BitstreamSkip(bs,32);					// visual_object_start_code
			if (BitstreamGetBit(bs))				// is_visual_object_identified
			{
				vol_ver_id = BitstreamGetBits(bs,4);	// visual_object_ver_id
				BitstreamSkip(bs, 3);				// visual_object_priority
			}
			else
			{
				vol_ver_id = 1;
			}

			if (BitstreamShowBits(bs, 4) != VISOBJ_TYPE_VIDEO)	// visual_object_type
			{
//				DEBUG("visual_object_type != video");
				return -1;
			}
			BitstreamSkip(bs, 4);

			// video_signal_type

			if (BitstreamGetBit(bs))				// video_signal_type
			{
	//			DEBUG("+ video_signal_type");
				BitstreamSkip(bs, 3);				// video_format
				BitstreamSkip(bs, 1);				// video_range
				if (BitstreamGetBit(bs))			// color_description
				{
		//			DEBUG("+ color_description");
					BitstreamSkip(bs, 8);			// color_primaries
					BitstreamSkip(bs, 8);			// transfer_characteristics
					BitstreamSkip(bs, 8);			// matrix_coefficients
				}
			}
		}
		else if ((start_code & ~0x1f) == VIDOBJ_START_CODE)
		{
#ifdef _DEBUG_PRINT
			fprintf(stderr,"video_object_start_code \n");
#endif
			BitstreamSkip(bs, 32);		// video_object_start_code
			
		} 
		else if ((start_code & ~0xf) == VIDOBJLAY_START_CODE)
		{
			// DEBUG("video_object_layer");
			BitstreamSkip(bs, 32);					// video_object_layer_start_code

			BitstreamSkip(bs, 1);									// random_accessible_vol

			// video_object_type_indication
			if (BitstreamShowBits(bs, 8) != VIDOBJLAY_TYPE_SIMPLE &&
				BitstreamShowBits(bs, 8) != VIDOBJLAY_TYPE_CORE &&
				BitstreamShowBits(bs, 8) != VIDOBJLAY_TYPE_MAIN &&
				BitstreamShowBits(bs, 8) != 0)		// BUGGY DIVX
			{
			//	DEBUG1("video_object_type_indication not supported", BitstreamShowBits(bs, 8));
				fprintf(stderr,"video_object_type_indication not supported %d\n", BitstreamShowBits(bs, 8));
				return -1;
			}
			BitstreamSkip(bs, 8);


			if (BitstreamGetBit(bs))					// is_object_layer_identifier
			{
				//DEBUG("+ is_object_layer_identifier");
				vol_ver_id = BitstreamGetBits(bs,4);		// video_object_layer_verid
				BitstreamSkip(bs, 3);					// video_object_layer_priority
			}
			else
			{
				vol_ver_id = 1;
			}
			//DEBUGI("vol_ver_id", vol_ver_id);

			if (BitstreamGetBits(bs, 4) == VIDOBJLAY_AR_EXTPAR)	// aspect_ratio_info
			{
				//DEBUG("+ aspect_ratio_info");
				BitstreamSkip(bs, 8);						// par_width
				BitstreamSkip(bs, 8);						// par_height
			}

			if (BitstreamGetBit(bs))		// vol_control_parameters
			{
				//DEBUG("+ vol_control_parameters");
				BitstreamSkip(bs, 2);						// chroma_format
				BitstreamSkip(bs, 1);						// low_delay
				if (BitstreamGetBit(bs))					// vbv_parameters
				{
					//DEBUG("+ vbv_parameters");
					BitstreamSkip(bs, 15);				// first_half_bitrate
					READ_MARKER();
					BitstreamSkip(bs, 15);				// latter_half_bitrate
					READ_MARKER();
					BitstreamSkip(bs, 15);				// first_half_vbv_buffer_size
					READ_MARKER();
					BitstreamSkip(bs, 3);					// latter_half_vbv_buffer_size
					BitstreamSkip(bs, 11);				// first_half_vbv_occupancy
					READ_MARKER();
					BitstreamSkip(bs, 15);				// latter_half_vbv_occupancy
					READ_MARKER();
				
				}
			}

			shape = BitstreamGetBits(bs, 2);	// video_object_layer_shape
			// DEBUG1("shape", dec->shape);
			
			if (shape == VIDOBJLAY_SHAPE_GRAYSCALE && vol_ver_id != 1)
			{
				BitstreamSkip(bs, 4);		// video_object_layer_shape_extension
			}

			READ_MARKER();

			time_inc_resolution = BitstreamGetBits(bs, 16);	// vop_time_increment_resolution
			//time_inc_resolution--;
#ifdef _DEBUG_PRINT
			fprintf(stderr,"time_inc_resolution is %u\n",time_inc_resolution);
#endif
			if (time_inc_resolution > 0)
			{
				time_inc_bits = log2bin(time_inc_resolution);
			}
			else
			{
				// dec->time_inc_bits = 0;

				// for "old" xvid compatibility, set time_inc_bits = 1
				time_inc_bits = 1;
			}
			READ_MARKER();

			if (BitstreamGetBit(bs))						// fixed_vop_rate
			{
				//BitstreamSkip(bs, time_inc_bits);	// fixed_vop_time_increment
				fixed_vop_time_increment = BitstreamGetBits(bs, time_inc_bits);	// fixed_vop_time_increment
				param->framerate = time_inc_resolution*1000/fixed_vop_time_increment;
				fixed_vop_rate = 1;
			}
			else
			{
				param->framerate = time_inc_resolution*1000/1001;
			}

			if (shape != VIDOBJLAY_SHAPE_BINARY_ONLY)
			{

				if (shape == VIDOBJLAY_SHAPE_RECTANGULAR)
				{
					READ_MARKER();
					temp_width = BitstreamGetBits(bs, 13);			// video_object_layer_width
					//DEBUGI("width", width);
					//fprintf(stderr,"width is %u\n",width);
					READ_MARKER();
					temp_height = BitstreamGetBits(bs, 13);		// video_object_layer_height
					//DEBUGI("height", height);	
					//fprintf(stderr,"height is %u\n",height);
					READ_MARKER();

					if (findvol == 0) 
					{
					  if (temp_width != width || temp_height != height)
					    {
					      fprintf(stderr,"FATAL: video dimension discrepancy ***");
					      fprintf(stderr,"bitstream width %u /height %u\n", temp_width, temp_height);
					      fprintf(stderr,"param width %u /height %u\n", width, height);
					      return -1;
					    }
					} 
					else 
					{
					  width = temp_width;
					  height = temp_height;
					}
										

				}

				if ((interlacing = BitstreamGetBit(bs)))
				{
					fprintf(stderr,"vol: interlacing\n");
				}

				if (!BitstreamGetBit(bs))				// obmc_disable
				{
					fprintf(stderr,"IGNORED/TODO: !obmc_disable\n");
					// TODO
					// fucking divx4.02 has this enabled
				}

				if (BitstreamGetBits(bs, (vol_ver_id == 1 ? 1 : 2)))  // sprite_enable
				{
					fprintf(stderr,"sprite_enable; not supported\n");
					return -1;
				}
			
				if (vol_ver_id != 1 && shape != VIDOBJLAY_SHAPE_RECTANGULAR)
				{
					BitstreamSkip(bs, 1);					// sadct_disable
				}

				if (BitstreamGetBit(bs))						// not_8_bit
				{
					//DEBUG("+ not_8_bit [IGNORED/TODO]");
					quant_bits = BitstreamGetBits(bs, 4);	// quant_precision
					BitstreamSkip(bs, 4);						// bits_per_pixel
				}
				else
				{
					quant_bits = 5;
				}

				if (shape == VIDOBJLAY_SHAPE_GRAYSCALE)
				{
					BitstreamSkip(bs, 1);			// no_gray_quant_update
					BitstreamSkip(bs, 1);			// composition_method
					BitstreamSkip(bs, 1);			// linear_composition
				}

				quant_type = BitstreamGetBit(bs);		// quant_type
				// DEBUG1("**** quant_type", dec->quant_type);

				if (quant_type)
				{
					if (BitstreamGetBit(bs))		// load_intra_quant_mat
					{
					}
					else
						//set_intra_matrix(get_default_intra_matrix());

					if (BitstreamGetBit(bs))		// load_inter_quant_mat
					{
					}
					else
						//set_inter_matrix(get_default_inter_matrix());

					if (shape == VIDOBJLAY_SHAPE_GRAYSCALE)
					{
						// TODO
						fprintf(stderr,"TODO: grayscale matrix stuff\n");
						return -1;
					}

				}

			
				if (vol_ver_id != 1)
				{
					quarterpel = BitstreamGetBit(bs);	// quarter_sampe
					if (quarterpel)
					{
						fprintf(stderr,"IGNORED/TODO: quarter_sample\n");
					}
				}
				else
				{
					quarterpel = 0;
				}

				if (!BitstreamGetBit(bs))			// complexity_estimation_disable
				{
					fprintf(stderr,"TODO: complexity_estimation header\n");
					// TODO
					return -1;
				}

				if (!BitstreamGetBit(bs))			// resync_marker_disable
				{
#ifdef _DEBUG_PRINT
					fprintf(stderr,"IGNORED/TODO: !resync_marker_disable\n");
#endif
					// TODO
				}

				if (BitstreamGetBit(bs))		// data_partitioned
				{
					fprintf(stderr,"+ data_partitioned\n");
					BitstreamSkip(bs, 1);		// reversible_vlc
				}

				if (vol_ver_id != 1)
				{
					if (BitstreamGetBit(bs))			// newpred_enable
					{
						fprintf(stderr,"+ newpred_enable\n");
						BitstreamSkip(bs, 2);			// requested_upstream_message_type
						BitstreamSkip(bs, 1);			// newpred_segment_type
					}
					if (BitstreamGetBit(bs))			// reduced_resolution_vop_enable
					{
						fprintf(stderr,"TODO: reduced_resolution_vop_enable\n");
						// TODO
						return -1;
					}
				}
				
				if (BitstreamGetBit(bs))	// scalability
				{
					// TODO
					fprintf(stderr,"TODO: scalability\n");
					return -1;
				}
			}
			else	// dec->shape == BINARY_ONLY
			{
				if (vol_ver_id != 1)
				{
					if (BitstreamGetBit(bs))	// scalability
					{
						// TODO
						fprintf(stderr,"TODO: scalability\n");
						return -1;
					}
				}
				BitstreamSkip(bs, 1);			// resync_marker_disable

			}
			param->width = width;
		  param->height = height;
			param->time_inc_bits  = time_inc_bits;
			param->framerate = (time_inc_resolution+1)*1000/1001;
			return 1;
		}
		else if (start_code == GRPOFVOP_START_CODE)
		{
			// DEBUG("group_of_vop");
			BitstreamSkip(bs, 32);
			{
				
				hours = BitstreamGetBits(bs, 5);
				minutes = BitstreamGetBits(bs, 6);
				READ_MARKER();
				seconds = BitstreamGetBits(bs, 6);
				// DEBUG3("hms", hours, minutes, seconds);
			}
			BitstreamSkip(bs, 1);			// closed_gov
			BitstreamSkip(bs, 1);			// broken_link
		}
		else if (start_code == VOP_START_CODE)
		{
		  if (findvol != 0) return -1;
			// DEBUG("vop_start_code");
			BitstreamSkip(bs, 32);						// vop_start_code

			coding_type = BitstreamGetBits(bs, 2);		// vop_coding_type
			//DEBUG1("coding_type", coding_type);

			while (BitstreamGetBit(bs) != 0) ;			// time_base
	
			READ_MARKER();
	 
			//DEBUG1("time_inc_bits", dec->time_inc_bits);
			//DEBUG1("vop_time_incr", BitstreamShowBits(bs, dec->time_inc_bits));
			if (time_inc_bits)
			{
				BitstreamSkip(bs, time_inc_bits);	// vop_time_increment
			}

			READ_MARKER();

			if (!BitstreamGetBit(bs))					// vop_coded
			{
				return N_VOP;
			}

		
			if (coding_type != I_VOP)
			{
				rounding = BitstreamGetBit(bs);	// rounding_type
				//DEBUG1("rounding", *rounding);
			}

			if (shape != VIDOBJLAY_SHAPE_RECTANGULAR)
			{
							
				temp_width = BitstreamGetBits(bs, 13);
				READ_MARKER();
				temp_height = BitstreamGetBits(bs, 13);
				READ_MARKER();
				horiz_mc_ref = BitstreamGetBits(bs, 13);
				READ_MARKER();
				vert_mc_ref = BitstreamGetBits(bs, 13);
				READ_MARKER();

				// DEBUG2("vop_width/height", width, height);
				// DEBUG2("ref             ", horiz_mc_ref, vert_mc_ref);

				BitstreamSkip(bs, 1);				// change_conv_ratio_disable
				if (BitstreamGetBit(bs))			// vop_constant_alpha
				{
					BitstreamSkip(bs, 8);			// vop_constant_alpha_value
				}
			}
				

			if (shape != VIDOBJLAY_SHAPE_BINARY_ONLY)
			{
				// intra_dc_vlc_threshold
				intra_dc_threshold = intra_dc_threshold_table[ BitstreamGetBits(bs,3) ];

				if (interlacing)
				{
					if ((top_field_first = BitstreamGetBit(bs)))
					{
						fprintf(stderr,"vop: top_field_first\n");
					}
					if ((alternate_vertical_scan = BitstreamGetBit(bs)))
					{
						fprintf(stderr,"vop: alternate_vertical_scan");
					}
				}
			}
						
			quant = BitstreamGetBits(bs, quant_bits);		// vop_quant
			//DEBUG1("quant", *quant);
						
			if (coding_type != I_VOP)
			{
				fcode = BitstreamGetBits(bs, 3);			// fcode_forward
			}
				
			if (coding_type == B_VOP)
			{
				// *fcode_backward = BitstreamGetBits(bs, 3);		// fcode_backward
			}
			return coding_type;
		}
		else if (start_code == USERDATA_START_CODE)
		{
			// DEBUG("user_data");
			BitstreamSkip(bs, 32);		// user_data_start_code
		}
		else if ((start_code & 0xfffffc03) == 0x00008002) 
		{
		  // Short video header.  Skip short_video_start_marker,
		  // temporal reference, marker and zero bit
		  have_short_header = 1;
		  shape = VIDOBJLAY_SHAPE_RECTANGULAR;
		  interlacing = 0;
		  quant_bits = 5;
		  quant_type = 0;
		  quarterpel = 0;
		  intra_dc_threshold = intra_dc_threshold_table[0];
		  rounding = 0;
		  fcode = 1;
		  BitstreamSkip(bs, 22);
		  BitstreamSkip(bs, 8 + 5);
		  source_format = BitstreamGetBits(bs, 3);
		  switch (source_format) {
		  case 1: // sub-QCIF
		    width = 128;
		    height = 96;
		    break;
		  case 2: // QCIF
		    width = 176;
		    height = 144;
		    break;
		  case 3: // CIF
		    width = 352;
		    height = 288;
		    break;
		  case 4: // 4CIF
		    width = 704;
		    height = 576;
		    break;
		  case 5:
		    width = 1408;
		    height = 1152;
		    break;
		  default:
		    fprintf(stderr,"FATAL: illegal code in short video header %u\n", source_format);
		    return -1;
		  }
		  if (findvol == 0) {
		    if (temp_width != width || temp_height != height)
		      {
						fprintf(stderr,"FATAL: video dimension discrepancy ***");
						fprintf(stderr,"bitstream width %u /height %u \n", width, height);
						fprintf(stderr,"param width %u /height %u \n", width, height);
						return -1;
		      }
		  } 
			else 
			{
		    width = temp_width;
		    height = temp_height;
				param->width = width;
				param->height = height;
		    return 1;
		  }
		  if (BitstreamGetBit(bs)) 
			{
		    // P frame
		    coding_type = P_VOP;
		  }
			else 
			{
		    coding_type = I_VOP;
		  }
		  BitstreamSkip(bs, 4); // skip 4 reserved 0 bits
		  quant = BitstreamGetBits(bs, 5);
		  BitstreamSkip(bs, 1);
		  while (BitstreamGetBit(bs) == 1) BitstreamSkip(bs, 8); // pei and psupp
		  return coding_type;
		}
		else  // start_code == ?
		{
			if (BitstreamShowBits(bs, 24) == 0x000001)
			{
				fprintf(stderr,"*** WARNING: unknown start_code%u\n", BitstreamShowBits(bs, 32));
			}
			BitstreamSkip(bs, 8);
		}
	}
	while ((BitstreamPos(bs) >> 3) < bs->length);

	fprintf(stderr,"*** WARNING: no vop_start_code found");
	if (findvol != 0) return 0;
	return -1; /* ignore it */
}


////////////////////////////////////
/* initialise bitstream structure */

void BitstreamInit(Bitstream * bs,/*const uint32_t * bitstream,*/uint32_t length)
{
	uint32_t tmp;
	bs->start = bs->tail = (uint32_t*)bitstream;
	
	tmp = *(uint32_t *)bitstream;
	bs->bufa = tmp;
	tmp = *((uint32_t *)bitstream+1);

	bs->bufb = tmp;

	bs->buf = 0;
	bs->pos = 0;
	bs->length = length;

}


//////////////////////////////
// entire function added for mpeg4ip

int decoder_find_vol( uint32_t length,XVID_DEC_PARAM * param)
{
	Bitstream bs;
	int ret;
	BitstreamInit(&bs,length);

	ret = BitstreamReadHeaders(&bs,param,1);
	return ret;
}

int parse_vovod(unsigned char *ConfigHex,unsigned int HexLen,unsigned *FrameRate,unsigned *TimeIncBits,unsigned *Width,unsigned *Height)
{
  int ret = -1;
  int size_int = 0;
  int i = 0,j = 0;
  XVID_DEC_PARAM param;
  // Get the VO/VOL header.  If we fail, set the bytestream back
	size_int = HexLen/sizeof(uint32_t)+1;
	bitstream = (uint32_t *)malloc(size_int*sizeof(uint32_t));
	if(bitstream == NULL)
	{
		fprintf(stderr,"bitstream alloc failed \n");
		return ret;	
	}
	for(i=0;i<size_int;i++)
	{
		bitstream[i] = ((ConfigHex[j]&0xff)<<24)|((ConfigHex[j+1]&0xff)<<16)|((ConfigHex[j+2]&0xff)<<8)|(ConfigHex[j+3]&0xff);
		j = j+sizeof(uint32_t);
		//fprintf(stderr,"bitstream i %d,%x\n",i,*(bitstream+i));
	}
	ret = 0;
	ret = decoder_find_vol(HexLen, &param);
	*Width = param.width;
	*Height = param.height;
	*TimeIncBits = param.time_inc_bits;
	*FrameRate = param.framerate;
	free(bitstream);
	return ret;
}

/*
 * Actor art_Rtsp (ActorClass_art_Rtsp)
 */

// #include "actors-rts.h"
// #include <errno.h>
// #include <sys/socket.h>
// #include <unistd.h>
// #include <semaphore.h>
// #include <netinet/in.h>
// #include <arpa/inet.h>
// #include <stdlib.h> 
// #include <stdio.h>
// #include "rtsp_client.h"

#define OUT0_Out   ART_OUTPUT(0)
#define OUT1_Out   ART_OUTPUT(1)
#define IN0_In     ART_INPUT(0)

extern void switch_service(void *instance);

typedef struct {
  AbstractActorInstance base;
  char           *url;
  int            socketNum;
  int            udpSock;
  char           buf[MAX_PACKET_SIZE];
  int            pos;
  int            loop;
  int            size;
  videodata      vdata;
  int            serviceLevel;
  int32_t        happiness;
  int            now;
  FILE			 *fp;
} ActorInstance_art_Rtsp;

typedef struct cpu_runtime_data {
  struct cpu_runtime_data *cpu; /* Pointer to first element in this list */
  int            cpu_count;
  int            cpu_index;
  void           *(*main)(struct cpu_runtime_data *, int);
  pthread_t      thread;
  int            physical_id; /* physical index of this cpu */
  sem_t          *sem;
  int            *sleep; // Odd value indicates thread sleeping
} cpu_runtime_data_t;

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 1, 1
};

static const int exitcode_block_In_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};


ART_ACTION_CONTEXT(1, 2);

int full_frame(dblist *head)
{
	dblist *node=head;
#ifdef _UDP
	if(node)
		return 1;
	else
		return 0;
#else
	while(node)
	{
		if(node->mark)
			return 1;
		node=node->next;
	}
	return 0;
#endif
}
ART_ACTION_SCHEDULER(art_Rtsp_action_scheduler)
{
  const int *result = EXIT_CODE_YIELD;
  ActorInstance_art_Rtsp *thisActor = (ActorInstance_art_Rtsp*)pBase;
  int n,serviceLevel;
  int32_t in;
  struct timeb tb;
  int now;
  videodata *vdata =&thisActor->vdata;
  dblist    *head = vdata->head;

  ART_ACTION_SCHEDULER_ENTER(1, 2);

  n = pinAvailOut_int32_t(OUT0_Out);
  in = pinAvailIn_int32_t(IN0_In);
	
  ART_ACTION_SCHEDULER_LOOP {
    ART_ACTION_SCHEDULER_LOOP_TOP;

		
    if (thisActor->size == 0) {
      result = EXITCODE_TERMINATE;
		  goto out;
    }


  //Happiness
  ftime(&tb);
  now = tb.time*1000 + tb.millitm;
  if(now - thisActor->now >= 1000)
  {
    thisActor->now = now;
    int space=pinAvailOut_int32_t(OUT1_Out);
    if(space>=1){
      ART_ACTION_ENTER(happinessOut,2);
      pinWrite_int32_t(OUT1_Out,thisActor->happiness);
      ART_ACTION_EXIT(happinessOut,2);
    }
  }
	//Service leve
	if(in > 0)
	{
		in--;
		ART_ACTION_ENTER(serviceLevelIn,1);
		serviceLevel=pinRead_int32_t(IN0_In);
		if(serviceLevel != thisActor->serviceLevel){
			thisActor->serviceLevel=serviceLevel;
			thisActor->loop=0;
			Timeout=0;
#ifdef _UDP
			switch_service(pBase);
#endif
		}
		ART_ACTION_EXIT(serviceLevelIn, 1);
	}

    //Streaming data
    if (full_frame(head) && n > 0) {
      n--;
      ART_ACTION_ENTER(streamingOut, 0);
      pinWrite_int32_t(OUT0_Out, head->data[head->pos]);
#ifdef RECORDER
	  fputc(head->data[head->pos], thisActor->fp);
#endif	  
      head->pos++;
      if(head->pos>=head->len){
        pthread_mutex_lock(&vdata->mutex);
		if(head->next)
		{
			head->next->prior = NULL;
        	vdata->head = head->next;
		}
		else
			vdata->head = NULL;
        vdata->numFrames--;
        vdata->numBytes -= head->len;
        pthread_mutex_unlock(&vdata->mutex);
        free(head);
		head=vdata->head;
      }
      ART_ACTION_EXIT(streamingOut, 0);
    } else {
      result = exitcode_block_Out_1;
      goto out;
    }

    ART_ACTION_SCHEDULER_LOOP_BOTTOM;
  }
	
out:
  ART_ACTION_SCHEDULER_EXIT(1, 2);
  return result;

}

static void wakeup_me(void *instance)
{
  if(instance){
    ActorInstance_art_Rtsp *thisActor=(ActorInstance_art_Rtsp*)instance;
		cpu_runtime_data_t *cpu=(cpu_runtime_data_t *)thisActor->base.cpu;
		// wake me up if I'm sleeping
		if(*cpu->sleep&1)
      sem_post(cpu->sem);		
  }
}

void strip(videodata *vdata)
{
	dblist *list = vdata->tail;
	int mark;
	pthread_mutex_lock(&vdata->mutex);
	while(list){
		mark=list->mark;
		if(mark)
			break;
		vdata->tail=list->prior;
		if(vdata->tail)
			vdata->tail->next=NULL;
		free(list);
		list=vdata->tail;
	}
	if(vdata->tail==NULL)
		vdata->head=NULL;
	pthread_mutex_unlock(&vdata->mutex);
}

void store(void *instance,struct ResultData *data)	
{
	ActorInstance_art_Rtsp *thisActor = (ActorInstance_art_Rtsp*)instance;
	videodata *vdata = &thisActor->vdata;
	dblist *list;

	if(data->len==0)
		return;

	list = (dblist*)malloc(sizeof(dblist));
	memset(list,0,sizeof(dblist));
	memcpy(list->data,data->buffer,data->len);
	list->mark = data->frtpMarkerBit;
	list->len = data->len;

	pthread_mutex_lock(&vdata->mutex);
	if(vdata->head == NULL)
		vdata->head = vdata->tail = list;
	else
	{
		list->prior = vdata->tail;
		vdata->tail->next=list;
		vdata->tail = list;
	}
	vdata->numFrames++;
	vdata->numBytes += data->len;
	if(vdata->numFrames >= 20)
		thisActor->happiness = 0;
	else
		thisActor->happiness = 1;
	fprintf(stderr,"NumFrames: %d NumBytes: %d\r",vdata->numFrames,vdata->numBytes);
	pthread_mutex_unlock(&vdata->mutex);

	wakeup_me(instance);	

	return;
}

void *recvUdpProc(void *instance)
{
	ActorInstance_art_Rtsp *thisActor = (ActorInstance_art_Rtsp*)instance;
	struct sockaddr_in client,server;
	int s, slen=sizeof(server);
	unsigned char buf[MAX_PACKET_SIZE];
	struct ResultData data;
	int retVal;
	int then;
  	struct timeb tb;

	if ((thisActor->udpSock=socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP))==-1){
		perror("socket error");
		exit(0);
	}
	memset((char *) &client, 0, sizeof(client));
	client.sin_family = AF_INET;
	client.sin_port = htons(rtpNumber);
	client.sin_addr.s_addr = htonl(INADDR_ANY);
	if (bind(thisActor->udpSock, &client, sizeof(client))==-1){
		perror("bind error");
		exit(0);
	}
	data.buffer=buf;
	ftime(&tb);
	then=tb.time;
	while(loop)
	{
		//keep alive
		if(Timeout)
		{
  			int now;
  			ftime(&tb);
  			now = tb.time;
  			if(now - then >= (Timeout>>1)){
				then=now;
				optionsMediaSession(thisActor->socketNum);
			}
		}

		retVal = recvfrom(thisActor->udpSock, buf, MAX_PACKET_SIZE, 0, &server, &slen);
		if(retVal>0)
		{
			RTP_ReadHandler2(&data,retVal);
			if(data.fRTPPayloadFormat == 96)
			{
				store(instance,&data);
			}
		}
		else
			thisActor->size=0;
	}
	thisActor->size=0;
}

static void *recvProc(void *instance)
{
	ActorInstance_art_Rtsp *thisActor = (ActorInstance_art_Rtsp*)instance;
	char* url = thisActor->url;
	int socketNum;
	struct timeb startTime;
	struct timeb currentTime;
	char *timeline;
	struct ResultData data;
	struct MediaAttribute Attribute;
	int Finished = -1;
	unsigned char* streamBuf;
	unsigned int video_count=0;
	unsigned int audio_count=0;
	int          then;
    struct timeb tb;

	memset(Attribute.fConfigAsc,0,100);
	memset(Attribute.fConfigHex,0,50);
	Attribute.fVideoFrameRate	= 0;
	Attribute.fTimeIncBits = 0;
	Attribute.fVideoWidth = 0;
	Attribute.fVideoHeight = 0;
	vloopNum = 0;

	socketNum = thisActor->socketNum = init_rtsp(url,&Attribute,thisActor->serviceLevel);
	if(socketNum<0){
		fprintf(stderr,"Error get socket %d\n",socketNum);
		return NULL;
	}

	ftime(&startTime);
	timeline = ctime( & ( startTime.time ) );

	fprintf(stderr,"Start receive streaming....\n");
	fprintf(stderr,"Start time is: %.19s.%hu %s\n",timeline, startTime.millitm, &timeline[20]);

#ifdef _UDP
	pthread_exit(NULL);
#endif

	ftime(&tb);
    then = tb.time;

	streamBuf = (unsigned char *)malloc(MAX_PACKET_SIZE*sizeof(unsigned char));
	if(streamBuf == NULL)
	{
		fprintf(stderr,"alloc streamBuf failed\n");
		clearup(socketNum);
		return NULL;
	}
	data.buffer = streamBuf;

	while(thisActor->loop)
	{
		//Keep alive using OPTIONS
		if(Timeout)
		{
			ftime(&tb);
			if(tb.time-then>(Timeout>>1))
			{
				then=tb.time;	
				optionsMediaSession(socketNum);
			}
		}
		memset(data.buffer,0,MAX_PACKET_SIZE);
		data.len = 0;
		data.fRTPPayloadFormat = 0;
		data.frtpTimestamp = 0;

		//Finished = tcpReadHandler(socketNum,&data);
		Finished = RTP_ReadHandler(socketNum,&data);

		if(data.fRTPPayloadFormat == 96)
		{
			video_count+=data.len;
			//fprintf(stderr,"video: %d bytes,audio:%d bytes\r",video_count,audio_count);
			store(instance,&data);
		}
		else if(data.fRTPPayloadFormat == 97)
		{
			audio_count+=data.len;
			//fprintf(stderr,"video: %d bytes,audio:%d bytes\r",video_count,audio_count); 
		}

		if(!Finished) 
		{
			//Attribute.fVideoFrameRate = VFrameRate;
			fprintf(stderr,"MediaAttribute fVideoFrequency is %u\n",Attribute.fVideoFrequency);
			fprintf(stderr,"MediaAttribute fVideoPayloadFormat is %u\n",Attribute.fVideoPayloadFormat);
			fprintf(stderr,"MediaAttribute fConfigAsc is %s\n",Attribute.fConfigAsc);
			fprintf(stderr,"MediaAttribute fVideoFrameRate is %u\n",Attribute.fVideoFrameRate);
			fprintf(stderr,"MediaAttribute fTimeIncBits is %u\n",Attribute.fTimeIncBits);
			fprintf(stderr,"MediaAttribute fixed_vop_rate is %u\n",Attribute.fixed_vop_rate);
			fprintf(stderr,"MediaAttribute fVideoWidth is %u\n",Attribute.fVideoWidth);
			fprintf(stderr,"MediaAttribute fVideoHeight is %u\n",Attribute.fVideoHeight);
			fprintf(stderr,"MediaAttribute fAudioFrequency is %u\n",Attribute.fAudioFrequency);
			fprintf(stderr,"MediaAttribute fAudioPayloadFormat is %u\n",Attribute.fAudioPayloadFormat);
			fprintf(stderr,"MediaAttribute fTrackNum is %u\n",Attribute.fTrackNum);		
			ftime(&currentTime);
			timeline = ctime( & ( currentTime.time ) );
			fprintf(stderr,"Receive successful\n");
			fprintf(stderr,"End time is: %.19s.%hu %s\n",timeline, currentTime.millitm, &timeline[20]);
			thisActor->size = 0;
			wakeup_me(instance);
			break;
		}
	}
	free(streamBuf);

	switch_service(instance);

	pthread_exit(NULL);
}

void switch_service(void *instance)
{
	ActorInstance_art_Rtsp *thisActor=(ActorInstance_art_Rtsp*)instance;
	pthread_t thread;

	printf("Switch to service leve: %d[%s,%d]\n",
           thisActor->serviceLevel,
           get_resolution(thisActor->serviceLevel),
           get_fps(thisActor->serviceLevel));
#ifndef _UDP
	strip(&thisActor->vdata);
#endif
	teardownMediaSession(thisActor->socketNum);

	close(thisActor->socketNum);
	thisActor->loop = 1;

	pthread_create(&thread,NULL,recvProc,(void*)thisActor);
}

static void crash(int sig){
  printf("program fault: sig=%x pid=%x\n",sig,getpid());
  loop=0;
  signal(SIGINT, SIG_DFL);
}

static void constructor(AbstractActorInstance *pBase) 
{
  ActorInstance_art_Rtsp *thisActor = (ActorInstance_art_Rtsp*)pBase;
  pthread_t thread;
  int       rc=0;
  videodata *vdata = &thisActor->vdata;
  struct timeb tb;
  char *ip;

  ftime(&tb);
  thisActor->now=tb.time*1000+tb.millitm;
  thisActor->happiness = 1;

  thisActor->socketNum = -1;
  thisActor->pos = 0;
  thisActor->size = -1;
  thisActor->serviceLevel=0;
  thisActor->loop = 1;
  srand(time(NULL));
  rtpNumber = UDP_PORT + (rand()%100)>>1;

  vdata->head = vdata->tail = NULL;
  vdata->numBytes = 0;
  vdata->numFrames = 0;
  pthread_mutex_init(&vdata->mutex, 0);
#ifdef RECORDER
  thisActor->fp=fopen("./record.bit","wb");
#endif

  //catch ctrl-c	
  signal(SIGINT,crash);

#ifdef _UDP
  rc = pthread_create(&thread,NULL,recvUdpProc,(void*)thisActor);
  if(rc)
  {
    printf("ERROR; return code from pthread_create() is %d\n", rc);
    exit(-1);
  }
#endif

  rc = pthread_create(&thread,NULL,recvProc,(void*)thisActor);
  if(rc)
  {
    printf("ERROR; return code from pthread_create() is %d\n", rc);
    exit(-1);
  }

}

static void destructor(AbstractActorInstance *pBase)
{
  ActorInstance_art_Rtsp *thisActor=(ActorInstance_art_Rtsp*) pBase;
  clearup(thisActor->socketNum);
}

static void setParam(AbstractActorInstance *pBase, 
                     const char *paramName, 
                     const char *value)
{
  ActorInstance_art_Rtsp *thisActor=(ActorInstance_art_Rtsp*) pBase;
  if (strcmp(paramName, "url")==0) {
    thisActor->url=(char*)value;
  } else {
	runtimeError(pBase,"No such parameter: %s", paramName);
  }
}

static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)},
  {"Out2", sizeof(int32_t)}
};

static const PortDescription inputPortDescriptions[]={
  {"In", sizeof(int32_t)}
};

static const int portRate_0[] = {
  0
};

static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"streamingOut",   portRate_0, portRate_1},
  {"serviceLevelIn", portRate_1, portRate_0},
  {"happinessOut",   portRate_0, portRate_1},

};

ActorClass ActorClass_art_Rtsp = INIT_ActorClass(
  "art_Rtsp",
  ActorInstance_art_Rtsp,
  constructor,
  setParam,
  art_Rtsp_action_scheduler,
  destructor,
  1, inputPortDescriptions,
  2, outputPortDescriptions,
  3, actionDescriptions
);

