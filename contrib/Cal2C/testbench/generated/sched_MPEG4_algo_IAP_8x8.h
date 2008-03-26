#ifndef SCHED_MPEG4_ALGO_IAP_8X8_H
#define SCHED_MPEG4_ALGO_IAP_8X8_H

SC_MODULE(sched_MPEG4_algo_IAP_8x8) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > PQF_AC;
  sc_port<tlm::tlm_fifo_get_if<int> > PTR;
  sc_port<tlm::tlm_fifo_get_if<int> > AC_PRED_DIR;
  sc_port<tlm::tlm_fifo_put_if<int> > QF_AC;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_IAP_8x8);

  sched_MPEG4_algo_IAP_8x8(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
