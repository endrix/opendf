#ifndef SCHED_MPEG4_ALGO_DCRINVPRED_CHROMA_8X8_H
#define SCHED_MPEG4_ALGO_DCRINVPRED_CHROMA_8X8_H

SC_MODULE(sched_MPEG4_algo_DCRinvpred_chroma_8x8) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_get_if<int> > A;
  sc_port<tlm::tlm_fifo_get_if<int> > B;
  sc_port<tlm::tlm_fifo_get_if<int> > C;
  sc_port<tlm::tlm_fifo_get_if<int> > QFS_DC;
  sc_port<tlm::tlm_fifo_put_if<int> > QF_DC;
  sc_port<tlm::tlm_fifo_put_if<int> > PTR;
  sc_port<tlm::tlm_fifo_put_if<int> > AC_PRED_DIR;
  sc_port<tlm::tlm_fifo_put_if<int> > SIGNED;
  sc_port<tlm::tlm_fifo_put_if<int> > QUANT;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_DCRinvpred_chroma_8x8);

  sched_MPEG4_algo_DCRinvpred_chroma_8x8(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
