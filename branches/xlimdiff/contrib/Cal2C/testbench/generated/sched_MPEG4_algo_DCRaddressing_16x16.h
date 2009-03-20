#ifndef SCHED_MPEG4_ALGO_DCRADDRESSING_16X16_H
#define SCHED_MPEG4_ALGO_DCRADDRESSING_16X16_H

SC_MODULE(sched_MPEG4_algo_DCRaddressing_16x16) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > A;
  sc_port<tlm::tlm_fifo_put_if<int> > B;
  sc_port<tlm::tlm_fifo_put_if<int> > C;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_DCRaddressing_16x16);

  sched_MPEG4_algo_DCRaddressing_16x16(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
