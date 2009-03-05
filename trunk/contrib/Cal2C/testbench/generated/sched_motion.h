#ifndef SCHED_MOTION_H
#define SCHED_MOTION_H

#include "broadcaster.h"
#include "sched_MPEG4_algo_Add.h"
#include "sched_MPEG4_algo_Interpolation_halfpel.h"
#include "sched_MPEG4_mgnt_Address.h"
#include "sched_MPEG4_mgnt_Framebuf.h"

SC_MODULE(sched_motion) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > MV;
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_get_if<int> > TEX;
  sc_port<tlm::tlm_fifo_put_if<int> > VID;

  // Sub-module instantiation
  broadcaster_input broadcaster_input1;
  broadcaster_output broadcaster_output1;
  sched_MPEG4_mgnt_Address address;
  sched_MPEG4_mgnt_Framebuf buffer;
  sched_MPEG4_algo_Interpolation_halfpel interpolation;
  sched_MPEG4_algo_Add add;

  // Local FIFO channels
  tlm::tlm_fifo<int> broadcaster_input1_output_address_btype;
  tlm::tlm_fifo<int> broadcaster_input1_output_add_btype;
  tlm::tlm_fifo<int> address_WA_buffer_WA;
  tlm::tlm_fifo<int> address_RA_buffer_RA;
  tlm::tlm_fifo<int> address_halfpel_interpolation_halfpel;
  tlm::tlm_fifo<int> buffer_RD_interpolation_RD;
  tlm::tlm_fifo<int> interpolation_MOT_add_MOT;
  tlm::tlm_fifo<int> add_VID_buffer_WD;

  SC_HAS_PROCESS(sched_motion);

  sched_motion(sc_module_name N, int LAYOUT) : sc_module(N),

    // Creates the modules
    broadcaster_input1("broadcaster_input1"),
    broadcaster_output1("broadcaster_output1"),
    address("address", LAYOUT),
    buffer("buffer"),
    interpolation("interpolation"),
    add("add"),

    // Initializes FIFOs size
    broadcaster_input1_output_address_btype(-16),
    broadcaster_input1_output_add_btype(-16),
    address_WA_buffer_WA(-16),
    address_RA_buffer_RA(-16),
    address_halfpel_interpolation_halfpel(-16),
    buffer_RD_interpolation_RD(-16),
    interpolation_MOT_add_MOT(-16),
    add_VID_buffer_WD(-16)
  {
    // Connects FIFOs and ports
    add.MOT(interpolation_MOT_add_MOT);
    add.TEX(TEX);
    add.VID(broadcaster_output1.input);
    broadcaster_output1.output(VID);
    broadcaster_output1.output(add_VID_buffer_WD);
    add.btype(broadcaster_input1_output_add_btype);
    address.MV(MV);
    address.RA(address_RA_buffer_RA);
    address.WA(address_WA_buffer_WA);
    address.btype(broadcaster_input1_output_address_btype);
    address.halfpel(address_halfpel_interpolation_halfpel);
    broadcaster_input1.input(btype);
    broadcaster_input1.output(broadcaster_input1_output_add_btype);
    broadcaster_input1.output(broadcaster_input1_output_address_btype);
    buffer.RA(address_RA_buffer_RA);
    buffer.RD(buffer_RD_interpolation_RD);
    buffer.WA(address_WA_buffer_WA);
    buffer.WD(add_VID_buffer_WD);
    interpolation.MOT(interpolation_MOT_add_MOT);
    interpolation.RD(buffer_RD_interpolation_RD);
    interpolation.halfpel(address_halfpel_interpolation_halfpel);
  }

};

#endif
