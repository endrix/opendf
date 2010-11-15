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

#ifndef _RTSP_CLIENT_H
#define _RTSP_CLIENT_H

#include <stdio.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <errno.h>
#include <ctype.h>
#include <time.h>
#include <sys/timeb.h>
#include <sys/time.h>

#define MAX_READBUFSIZE 4096 /*20000*/
#define MAX_PACKET_SIZE 2048 /*10000*/

#define RESOLUTION	"176x144"
#define FPS			30

struct MediaAttribute
{
	unsigned fVideoFrequency;
	unsigned char fVideoPayloadFormat;
	char fConfigAsc[100];
	unsigned char fConfigHex[50];
	int fConfigHexLen;
	unsigned fVideoFrameRate;
	unsigned fTimeIncBits;
	unsigned fixed_vop_rate;
	unsigned fVideoWidth;
	unsigned fVideoHeight; 
	unsigned fAudioFrequency;
	unsigned char fAudioPayloadFormat;
	unsigned fTrackNum;
};

struct ResultData
{
	unsigned char *buffer;
	int len;
	unsigned char fRTPPayloadFormat;
	unsigned long frtpTimestamp;
	int frtpMarkerBit;
};

struct MediaSubsession 
{
	char * fMediumName;
	char * fCodecName;
	char * fControlPath;// "a=control:<control-path>" 
	char * fConfig;
	unsigned short fClientPortNum;
	unsigned fNumChannels;
	unsigned frtpTimestampFrequency;
	unsigned char fRTPPayloadFormat;
	char * fSessionId;
	char *fFileName;
	FILE* fFid;
	struct MediaSubsession *fNext;
};

int blockUntilwriteable(int socket);
int blockUntilReadable(int socket);
char* parseSDPLine(char* inputLine);
				   
int parseResponseCode(char* line, unsigned int * responseCode);
int getResponse(int socketNum,char* responseBuffer,unsigned responseBufferSize) ;

char* getLine(char* startOfLine);

int parseRTSPURL(char* url,char* address,int* portNum); 

int setupMediaSubsession(int socketNum,struct MediaSubsession* subsession) ;

char * parseSDPAttribute_rtpmap(char* sdpLine,unsigned* rtpTimestampFrequency,unsigned *fnumChannels) ;//Check for a "a=rtpmap:<fmt> <codec>/<freq>line
char * parseSDPAttribute_control(char* sdpLine) ;//Check for a "a=control:<control-path>" line
int parseSDPAttribute_range(char* sdpLine);//Check for a "a=range:npt=<startTime>-<endTime>" line
char * parseSDPAttribute_fmtp(char* sdpLine) ;//Check for a "a=fmtp:" line



int networkReadHandler(int socketNum,unsigned *NextTCPReadSize,struct ResultData* data) ;
unsigned char* skip(unsigned char * buffer,unsigned numBytes);
int handleRead(int socketNum,unsigned char* buffer,unsigned bufferMaxSize,unsigned* bytesRead,unsigned* NextTCPReadSize) ;


int getSDPDescriptionFromURL(int socketNum,char* url,char* Description,int servicelevel);
int openConnectionFromURL(char* url);


struct MediaSubsession * initializeWithSDP(char* sdpDescription,int *SubsessionNum); 
struct MediaAttribute * GetMediaAttrbute(struct MediaAttribute *Attribute,struct MediaSubsession * subsession,int subsessionNum);

void setupStreams(int socketNum,struct MediaSubsession *subsession,int subsessionNum);
void startPlayingStreams(int socketNum,struct MediaSubsession *subsession,int subsessionNum); 
int teardownMediaSession(int socketNum);
void resumeStreams(int socketNum) ;
int pauseMediaSession(int socketNum);
int playMediaSession(int socketNum,int start,int end);

char* strDup(char* str) ;
char* strDupSize(char* str);
int hex_a(unsigned char *hex, char *a,unsigned char length);
int a_hex( char *a,unsigned char *hex,unsigned char len);

//extern int init_rtsp(char *url,struct MediaAttribute *Attribute);
int init_rtsp(char *url,struct MediaAttribute *Attribute,int serviceLevel);

extern int RTP_ReadHandler(int socketNum,struct ResultData* data);
extern void clearup(int socketNum);

extern unsigned fCSeq;
extern char const* const UserAgentHeaderStr;

extern unsigned const parseBufferSize;
extern char *fBaseURL;
extern char *fLastSessionId;
extern unsigned long fMaxPlayEndTime;

extern unsigned VTimestampFrequency;
extern unsigned ATimestampFrequency;

extern unsigned char VPayloadType;
extern unsigned char APayloadType;
extern unsigned long VFrameRate;
extern unsigned long vloopNum;

#endif
