#ifndef SCHED_MPEG4_ALGO_IS_H
#define SCHED_MPEG4_ALGO_IS_H

SC_MODULE(sched_MPEG4_algo_IS) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > AC_PRED_DIR;
  sc_port<tlm::tlm_fifo_get_if<int> > QFS_AC;
  sc_port<tlm::tlm_fifo_put_if<int> > PQF_AC;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_IS);

  sched_MPEG4_algo_IS(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
