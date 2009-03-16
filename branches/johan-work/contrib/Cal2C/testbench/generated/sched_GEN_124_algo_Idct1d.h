#ifndef SCHED_GEN_124_ALGO_IDCT1D_H
#define SCHED_GEN_124_ALGO_IDCT1D_H

SC_MODULE(sched_GEN_124_algo_Idct1d) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > X;
  sc_port<tlm::tlm_fifo_put_if<int> > Y;

  // Variable parameters
  int m_row;


  SC_HAS_PROCESS(sched_GEN_124_algo_Idct1d);

  sched_GEN_124_algo_Idct1d(sc_module_name N, int ROW) : sc_module(N), m_row(ROW) {
    SC_THREAD(process);
  }

  void process();

};

#endif
