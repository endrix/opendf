#ifndef SCHED_GEN_ALGO_TRANSPOSE_H
#define SCHED_GEN_ALGO_TRANSPOSE_H

SC_MODULE(sched_GEN_algo_Transpose) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > X;
  sc_port<tlm::tlm_fifo_put_if<int> > Y;

  // Variable parameters


  SC_HAS_PROCESS(sched_GEN_algo_Transpose);

  sched_GEN_algo_Transpose(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
