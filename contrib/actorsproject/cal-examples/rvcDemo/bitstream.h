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


#ifndef _BITSTREAM_H_
#define _BITSTREAM_H_

#include <stdint.h>

#define uint32_t unsigned int
// comment any #defs we dont use

#define VIDOBJ_START_CODE		0x00000100	/* ..0x0000011f  */
#define VIDOBJLAY_START_CODE	0x00000120	/* ..0x0000012f */
#define VISOBJSEQ_START_CODE	0x000001b0
#define VISOBJSEQ_STOP_CODE		0x000001b1	/* ??? */
#define USERDATA_START_CODE		0x000001b2
#define GRPOFVOP_START_CODE		0x000001b3
//#define VIDSESERR_ERROR_CODE	0x000001b4
#define VISOBJ_START_CODE		0x000001b5
//#define SLICE_START_CODE		0x000001b7
//#define EXT_START_CODE		0x000001b8


#define VISOBJ_TYPE_VIDEO				1
//#define VISOBJ_TYPE_STILLTEXTURE		2
//#define VISOBJ_TYPE_MESH				3
//#define VISOBJ_TYPE_FBA				4
//#define VISOBJ_TYPE_3DMESH			5


#define VIDOBJLAY_TYPE_SIMPLE			1
//#define VIDOBJLAY_TYPE_SIMPLE_SCALABLE	2
#define VIDOBJLAY_TYPE_CORE				3
#define VIDOBJLAY_TYPE_MAIN				4


//#define VIDOBJLAY_AR_SQUARE			1
//#define VIDOBJLAY_AR_625TYPE_43		2
//#define VIDOBJLAY_AR_525TYPE_43		3
//#define VIDOBJLAY_AR_625TYPE_169		8
//#define VIDOBJLAY_AR_525TYPE_169		9
#define VIDOBJLAY_AR_EXTPAR				15


#define VIDOBJLAY_SHAPE_RECTANGULAR		0
#define VIDOBJLAY_SHAPE_BINARY			1
#define VIDOBJLAY_SHAPE_BINARY_ONLY		2
#define VIDOBJLAY_SHAPE_GRAYSCALE		3

#define VO_START_CODE	0x8
#define VOL_START_CODE	0x12
#define VOP_START_CODE	0x1b6

#define	NUMBITS_VP_HEC					1
#define NUMBITS_VOP_QUANTIZER			5
#define	NUMBITS_VP_QUANTIZER			NUMBITS_VOP_QUANTIZER
#define NUMBITS_VOP_PRED_TYPE			2
#define	NUMBITS_VP_PRED_TYPE			NUMBITS_VOP_PRED_TYPE
#define NUMBITS_VOP_WIDTH				13
#define NUMBITS_VOP_HEIGHT				13
#define NUMBITS_VOP_HORIZONTAL_SPA_REF	13
#define NUMBITS_VOP_VERTICAL_SPA_REF	13
#define NUMBITS_VOP_FCODE				3
#define	NUMBITS_VP_INTRA_DC_SWITCH_THR	3

#define READ_MARKER()	BitstreamSkip(bs, 1)
#define WRITE_MARKER()	BitstreamPutBit(bs, 1)

// resync-specific
#define NUMBITS_VP_RESYNC_MARKER  17
#define RESYNC_MARKER 1

// vop coding types 
// intra, prediction, backward, sprite, not_coded
#define I_VOP	0
#define P_VOP	1
#define B_VOP	2
#define S_VOP	3
#define N_VOP	4
/////////////////////

/////////////
typedef struct
{
	uint32_t bufa;
	uint32_t bufb;
	uint32_t buf;
	uint32_t pos;
	uint32_t *tail;
	uint32_t *start;
	uint32_t length;
}Bitstream;

//////////////////

///////////////////////
typedef struct 
{
	unsigned int width;
	unsigned int height;
	unsigned int time_inc_bits;
	unsigned int framerate;
}XVID_DEC_PARAM;

//#define BSWAP(a) _asm mov eax,a _asm bswap eax _asm mov a, eax

// header stuff
int BitstreamReadHeaders(Bitstream * bs,XVID_DEC_PARAM * param,int findvol);

/* initialise bitstream structure */

void BitstreamInit(Bitstream * bs,uint32_t length);
/* reads n bits from bitstream without changing the stream pos */

uint32_t BitstreamShowBits(Bitstream *bs,uint32_t bits);

/* skip n bits forward in bitstream */

void BitstreamSkip(Bitstream * bs,uint32_t bits);

/* move forward to the next byte boundary */

void BitstreamByteAlign(Bitstream *bs);


/* bitstream length (unit bits) */

uint32_t BitstreamPos(Bitstream * bs);

/*	flush the bitstream & return length (unit bytes)
	NOTE: assumes no futher bitstream functions will be called.
 */

uint32_t BitstreamLength(Bitstream * bs);
/* read n bits from bitstream */

uint32_t BitstreamGetBits(Bitstream * bs,uint32_t n);

/* read single bit from bitstream */

uint32_t BitstreamGetBit(Bitstream * bs);

int decoder_find_vol(uint32_t length,XVID_DEC_PARAM * param);
extern int parse_vovod (unsigned char *ConfigHex,unsigned int HexLen,unsigned *FrameRate,unsigned *TimeIncBits,unsigned *Width,unsigned *Height);//½âÎöconfig
void BitstreamForward(Bitstream * bs, uint32_t bits);

extern uint32_t fixed_vop_rate;
extern uint32_t *bitstream;


#endif /* _BITSTREAM_H_ */
