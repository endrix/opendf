#ifndef SCHED_DC_RECONSTRUCTION_16X16_H
#define SCHED_DC_RECONSTRUCTION_16X16_H

#include "broadcaster.h"
#include "sched_MPEG4_algo_DCRaddressing_16x16.h"
#include "sched_MPEG4_algo_DCRinvpred_luma_16x16.h"

SC_MODULE(sched_DC_Reconstruction_16x16) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_get_if<int> > QFS_DC;
  sc_port<tlm::tlm_fifo_put_if<int> > PTR;
  sc_port<tlm::tlm_fifo_put_if<int> > AC_PRED_DIR;
  sc_port<tlm::tlm_fifo_put_if<int> > SIGNED;
  sc_port<tlm::tlm_fifo_put_if<int> > QF_DC;
  sc_port<tlm::tlm_fifo_put_if<int> > QUANT;

  // Sub-module instantiation
  broadcaster_input broadcaster_input1;
  sched_MPEG4_algo_DCRaddressing_16x16 addressing;
  sched_MPEG4_algo_DCRinvpred_luma_16x16 invpred;

  // Local FIFO channels
  tlm::tlm_fifo<int> broadcaster_input1_output_invpred_btype;
  tlm::tlm_fifo<int> broadcaster_input1_output_addressing_btype;
  tlm::tlm_fifo<int> addressing_A_invpred_A;
  tlm::tlm_fifo<int> addressing_B_invpred_B;
  tlm::tlm_fifo<int> addressing_C_invpred_C;

  SC_HAS_PROCESS(sched_DC_Reconstruction_16x16);

  sched_DC_Reconstruction_16x16(sc_module_name N) : sc_module(N),

    // Creates the modules
    broadcaster_input1("broadcaster_input1"),
    addressing("addressing"),
    invpred("invpred"),

    // Initializes FIFOs size
    broadcaster_input1_output_invpred_btype(-16),
    broadcaster_input1_output_addressing_btype(-16),
    addressing_A_invpred_A(-16),
    addressing_B_invpred_B(-16),
    addressing_C_invpred_C(-16)
  {
    // Connects FIFOs and ports
    addressing.A(addressing_A_invpred_A);
    addressing.B(addressing_B_invpred_B);
    addressing.C(addressing_C_invpred_C);
    addressing.btype(broadcaster_input1_output_addressing_btype);
    broadcaster_input1.input(btype);
    broadcaster_input1.output(broadcaster_input1_output_addressing_btype);
    broadcaster_input1.output(broadcaster_input1_output_invpred_btype);
    invpred.A(addressing_A_invpred_A);
    invpred.AC_PRED_DIR(AC_PRED_DIR);
    invpred.B(addressing_B_invpred_B);
    invpred.C(addressing_C_invpred_C);
    invpred.PTR(PTR);
    invpred.QFS_DC(QFS_DC);
    invpred.QF_DC(QF_DC);
    invpred.QUANT(QUANT);
    invpred.SIGNED(SIGNED);
    invpred.btype(broadcaster_input1_output_invpred_btype);
  }

};

#endif
