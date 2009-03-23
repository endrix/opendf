#ifndef SCHED_MPEG4_ALGO_DCRADDRESSING_8X8_H
#define SCHED_MPEG4_ALGO_DCRADDRESSING_8X8_H

SC_MODULE(sched_MPEG4_algo_DCRaddressing_8x8) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > A;
  sc_port<tlm::tlm_fifo_put_if<int> > B;
  sc_port<tlm::tlm_fifo_put_if<int> > C;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_DCRaddressing_8x8);

  sched_MPEG4_algo_DCRaddressing_8x8(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
