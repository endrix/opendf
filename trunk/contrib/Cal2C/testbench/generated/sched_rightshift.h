#ifndef SCHED_RIGHTSHIFT_H
#define SCHED_RIGHTSHIFT_H

SC_MODULE(sched_rightshift) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > In;
  sc_port<tlm::tlm_fifo_put_if<int> > Out;

  // Parameters

  SC_HAS_PROCESS(sched_rightshift);

  sched_rightshift(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
