#ifndef SCHED_MPEG4_ALGO_INVERSEQUANT_H
#define SCHED_MPEG4_ALGO_INVERSEQUANT_H

SC_MODULE(sched_MPEG4_algo_Inversequant) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > DC;
  sc_port<tlm::tlm_fifo_get_if<int> > AC;
  sc_port<tlm::tlm_fifo_get_if<int> > QP;
  sc_port<tlm::tlm_fifo_put_if<int> > out;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_Inversequant);

  sched_MPEG4_algo_Inversequant(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
