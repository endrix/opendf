#ifndef SCHED_MPEG4_ALGO_INTERPOLATION_HALFPEL_H
#define SCHED_MPEG4_ALGO_INTERPOLATION_HALFPEL_H

SC_MODULE(sched_MPEG4_algo_Interpolation_halfpel) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > RD;
  sc_port<tlm::tlm_fifo_get_if<int> > halfpel;
  sc_port<tlm::tlm_fifo_put_if<int> > MOT;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_Interpolation_halfpel);

  sched_MPEG4_algo_Interpolation_halfpel(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
