#ifndef SCHED_MVSEQUENCE_H
#define SCHED_MVSEQUENCE_H

SC_MODULE(sched_MVSequence) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > A;

  // Variable parameters


  SC_HAS_PROCESS(sched_MVSequence);

  sched_MVSequence(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
