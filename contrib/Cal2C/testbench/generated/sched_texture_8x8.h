#ifndef SCHED_TEXTURE_8X8_H
#define SCHED_TEXTURE_8X8_H

#include "broadcaster.h"
#include "sched_DC_Reconstruction_8x8.h"
#include "sched_GEN_mgnt_DCSplit.h"
#include "sched_MPEG4_algo_IAP_8x8.h"
#include "sched_MPEG4_algo_IS.h"
#include "sched_MPEG4_algo_Inversequant.h"
#include "sched_idct2d.h"

SC_MODULE(sched_texture_8x8) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_get_if<int> > QFS;
  sc_port<tlm::tlm_fifo_put_if<int> > f;

  // Sub-module instantiation
  broadcaster_output broadcaster_output1;
  sched_GEN_mgnt_DCSplit DCsplit;
  sched_DC_Reconstruction_8x8 DCRecontruction;
  sched_MPEG4_algo_IS IS;
  sched_MPEG4_algo_IAP_8x8 IAP;
  sched_MPEG4_algo_Inversequant IQ;
  sched_idct2d idct2d;

  // Local FIFO channels
  tlm::tlm_fifo<int> DCsplit_DC_DCRecontruction_QFS_DC;
  tlm::tlm_fifo<int> DCsplit_AC_IS_QFS_AC;
  tlm::tlm_fifo<int> IS_PQF_AC_IAP_PQF_AC;
  tlm::tlm_fifo<int> IAP_QF_AC_IQ_AC;
  tlm::tlm_fifo<int> IQ_out_idct2d__in_;
  tlm::tlm_fifo<int> DCRecontruction_SIGNED_idct2d__cal_signed;
  tlm::tlm_fifo<int> DCRecontruction_QUANT_IQ_QP;
  tlm::tlm_fifo<int> DCRecontruction_QF_DC_IQ_DC;
  tlm::tlm_fifo<int> DCRecontruction_PTR_IAP_PTR;
  tlm::tlm_fifo<int> DCRecontruction_AC_PRED_DIR_IAP_AC_PRED_DIR;
  tlm::tlm_fifo<int> DCRecontruction_AC_PRED_DIR_IS_AC_PRED_DIR;

  SC_HAS_PROCESS(sched_texture_8x8);

  sched_texture_8x8(sc_module_name N) : sc_module(N),

    // Creates the modules
    broadcaster_output1("broadcaster_output1"),
    DCsplit("DCsplit"),
    DCRecontruction("DCRecontruction"),
    IS("IS"),
    IAP("IAP"),
    IQ("IQ"),
    idct2d("idct2d"),

    // Initializes FIFOs size
    DCsplit_DC_DCRecontruction_QFS_DC(-16),
    DCsplit_AC_IS_QFS_AC(-16),
    IS_PQF_AC_IAP_PQF_AC(-16),
    IAP_QF_AC_IQ_AC(-16),
    IQ_out_idct2d__in_(-16),
    DCRecontruction_SIGNED_idct2d__cal_signed(-16),
    DCRecontruction_QUANT_IQ_QP(-16),
    DCRecontruction_QF_DC_IQ_DC(-16),
    DCRecontruction_PTR_IAP_PTR(-16),
    DCRecontruction_AC_PRED_DIR_IAP_AC_PRED_DIR(-16),
    DCRecontruction_AC_PRED_DIR_IS_AC_PRED_DIR(-16)
  {
    // Connects FIFOs and ports
    DCRecontruction.AC_PRED_DIR(broadcaster_output1.input);
    broadcaster_output1.output(DCRecontruction_AC_PRED_DIR_IAP_AC_PRED_DIR);
    broadcaster_output1.output(DCRecontruction_AC_PRED_DIR_IS_AC_PRED_DIR);
    DCRecontruction.PTR(DCRecontruction_PTR_IAP_PTR);
    DCRecontruction.QFS_DC(DCsplit_DC_DCRecontruction_QFS_DC);
    DCRecontruction.QF_DC(DCRecontruction_QF_DC_IQ_DC);
    DCRecontruction.QUANT(DCRecontruction_QUANT_IQ_QP);
    DCRecontruction.SIGNED(DCRecontruction_SIGNED_idct2d__cal_signed);
    DCRecontruction.btype(btype);
    DCsplit.AC(DCsplit_AC_IS_QFS_AC);
    DCsplit.DC(DCsplit_DC_DCRecontruction_QFS_DC);
    DCsplit.in(QFS);
    IAP.AC_PRED_DIR(DCRecontruction_AC_PRED_DIR_IAP_AC_PRED_DIR);
    IAP.PQF_AC(IS_PQF_AC_IAP_PQF_AC);
    IAP.PTR(DCRecontruction_PTR_IAP_PTR);
    IAP.QF_AC(IAP_QF_AC_IQ_AC);
    IQ.AC(IAP_QF_AC_IQ_AC);
    IQ.DC(DCRecontruction_QF_DC_IQ_DC);
    IQ.QP(DCRecontruction_QUANT_IQ_QP);
    IQ.out(IQ_out_idct2d__in_);
    IS.AC_PRED_DIR(DCRecontruction_AC_PRED_DIR_IS_AC_PRED_DIR);
    IS.PQF_AC(IS_PQF_AC_IAP_PQF_AC);
    IS.QFS_AC(DCsplit_AC_IS_QFS_AC);
    idct2d._cal_signed(DCRecontruction_SIGNED_idct2d__cal_signed);
    idct2d._in_(IQ_out_idct2d__in_);
    idct2d.out(f);
  }

};

#endif
