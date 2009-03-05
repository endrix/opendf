#ifndef SCHED_DECODER_H
#define SCHED_DECODER_H

#include "broadcaster.h"
#include "sched_GEN_mgnt_Merger420.h"
#include "sched_byte2bit.h"
#include "sched_motion.h"
#include "sched_parser.h"
#include "sched_texture_16x16.h"
#include "sched_texture_8x8.h"

SC_MODULE(sched_decoder) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > bits;
  sc_port<tlm::tlm_fifo_put_if<int> > VID;

  // Sub-module instantiation
  broadcaster_output broadcaster_output1;
  broadcaster_output broadcaster_output2;
  broadcaster_output broadcaster_output3;
  sched_byte2bit serialize;
  sched_parser parser;
  sched_texture_16x16 texture_Y;
  sched_texture_8x8 texture_U;
  sched_texture_8x8 texture_V;
  sched_motion motion_Y;
  sched_motion motion_U;
  sched_motion motion_V;
  sched_GEN_mgnt_Merger420 GEN_mgnt_Merger420;

  // Local FIFO channels
  tlm::tlm_fifo<int> serialize_out_parser_BITS;
  tlm::tlm_fifo<int> parser_MV_Y_motion_Y_MV;
  tlm::tlm_fifo<int> parser_BTYPE_Y_motion_Y_btype;
  tlm::tlm_fifo<int> parser_BTYPE_Y_texture_Y_btype;
  tlm::tlm_fifo<int> parser_B_Y_texture_Y_QFS;
  tlm::tlm_fifo<int> texture_Y_f_motion_Y_TEX;
  tlm::tlm_fifo<int> parser_MV_U_motion_U_MV;
  tlm::tlm_fifo<int> parser_BTYPE_U_motion_U_btype;
  tlm::tlm_fifo<int> parser_BTYPE_U_texture_U_btype;
  tlm::tlm_fifo<int> parser_B_U_texture_U_QFS;
  tlm::tlm_fifo<int> texture_U_f_motion_U_TEX;
  tlm::tlm_fifo<int> parser_MV_V_motion_V_MV;
  tlm::tlm_fifo<int> parser_BTYPE_V_motion_V_btype;
  tlm::tlm_fifo<int> parser_BTYPE_V_texture_V_btype;
  tlm::tlm_fifo<int> parser_B_V_texture_V_QFS;
  tlm::tlm_fifo<int> texture_V_f_motion_V_TEX;
  tlm::tlm_fifo<int> motion_Y_VID_GEN_mgnt_Merger420_Y;
  tlm::tlm_fifo<int> motion_U_VID_GEN_mgnt_Merger420_U;
  tlm::tlm_fifo<int> motion_V_VID_GEN_mgnt_Merger420_V;

  SC_HAS_PROCESS(sched_decoder);

  sched_decoder(sc_module_name N) : sc_module(N),

    // Creates the modules
    broadcaster_output1("broadcaster_output1"),
    broadcaster_output2("broadcaster_output2"),
    broadcaster_output3("broadcaster_output3"),
    serialize("serialize"),
    parser("parser"),
    texture_Y("texture_Y"),
    texture_U("texture_U"),
    texture_V("texture_V"),
    motion_Y("motion_Y", 1),
    motion_U("motion_U", 0),
    motion_V("motion_V", 0),
    GEN_mgnt_Merger420("GEN_mgnt_Merger420"),

    // Initializes FIFOs size
    serialize_out_parser_BITS(-16),
    parser_MV_Y_motion_Y_MV(-16),
    parser_BTYPE_Y_motion_Y_btype(-16),
    parser_BTYPE_Y_texture_Y_btype(-16),
    parser_B_Y_texture_Y_QFS(-16),
    texture_Y_f_motion_Y_TEX(-16),
    parser_MV_U_motion_U_MV(-16),
    parser_BTYPE_U_motion_U_btype(-16),
    parser_BTYPE_U_texture_U_btype(-16),
    parser_B_U_texture_U_QFS(-16),
    texture_U_f_motion_U_TEX(-16),
    parser_MV_V_motion_V_MV(-16),
    parser_BTYPE_V_motion_V_btype(-16),
    parser_BTYPE_V_texture_V_btype(-16),
    parser_B_V_texture_V_QFS(-16),
    texture_V_f_motion_V_TEX(-16),
    motion_Y_VID_GEN_mgnt_Merger420_Y(-16),
    motion_U_VID_GEN_mgnt_Merger420_U(-16),
    motion_V_VID_GEN_mgnt_Merger420_V(-16)
  {
    // Connects FIFOs and ports
    GEN_mgnt_Merger420.U(motion_U_VID_GEN_mgnt_Merger420_U);
    GEN_mgnt_Merger420.V(motion_V_VID_GEN_mgnt_Merger420_V);
    GEN_mgnt_Merger420.Y(motion_Y_VID_GEN_mgnt_Merger420_Y);
    GEN_mgnt_Merger420.YUV(VID);
    motion_U.MV(parser_MV_U_motion_U_MV);
    motion_U.TEX(texture_U_f_motion_U_TEX);
    motion_U.VID(motion_U_VID_GEN_mgnt_Merger420_U);
    motion_U.btype(parser_BTYPE_U_motion_U_btype);
    motion_V.MV(parser_MV_V_motion_V_MV);
    motion_V.TEX(texture_V_f_motion_V_TEX);
    motion_V.VID(motion_V_VID_GEN_mgnt_Merger420_V);
    motion_V.btype(parser_BTYPE_V_motion_V_btype);
    motion_Y.MV(parser_MV_Y_motion_Y_MV);
    motion_Y.TEX(texture_Y_f_motion_Y_TEX);
    motion_Y.VID(motion_Y_VID_GEN_mgnt_Merger420_Y);
    motion_Y.btype(parser_BTYPE_Y_motion_Y_btype);
    parser.BITS(serialize_out_parser_BITS);
    parser.BTYPE_U(broadcaster_output1.input);
    broadcaster_output1.output(parser_BTYPE_U_motion_U_btype);
    broadcaster_output1.output(parser_BTYPE_U_texture_U_btype);
    parser.BTYPE_V(broadcaster_output2.input);
    broadcaster_output2.output(parser_BTYPE_V_motion_V_btype);
    broadcaster_output2.output(parser_BTYPE_V_texture_V_btype);
    parser.BTYPE_Y(broadcaster_output3.input);
    broadcaster_output3.output(parser_BTYPE_Y_motion_Y_btype);
    broadcaster_output3.output(parser_BTYPE_Y_texture_Y_btype);
    parser.B_U(parser_B_U_texture_U_QFS);
    parser.B_V(parser_B_V_texture_V_QFS);
    parser.B_Y(parser_B_Y_texture_Y_QFS);
    parser.MV_U(parser_MV_U_motion_U_MV);
    parser.MV_V(parser_MV_V_motion_V_MV);
    parser.MV_Y(parser_MV_Y_motion_Y_MV);
    serialize.in8(bits);
    serialize.out(serialize_out_parser_BITS);
    texture_U.QFS(parser_B_U_texture_U_QFS);
    texture_U.btype(parser_BTYPE_U_texture_U_btype);
    texture_U.f(texture_U_f_motion_U_TEX);
    texture_V.QFS(parser_B_V_texture_V_QFS);
    texture_V.btype(parser_BTYPE_V_texture_V_btype);
    texture_V.f(texture_V_f_motion_V_TEX);
    texture_Y.QFS(parser_B_Y_texture_Y_QFS);
    texture_Y.btype(parser_BTYPE_Y_texture_Y_btype);
    texture_Y.f(texture_Y_f_motion_Y_TEX);
  }

};

#endif
