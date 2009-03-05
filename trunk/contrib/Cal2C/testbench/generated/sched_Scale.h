#ifndef SCHED_SCALE_H
#define SCHED_SCALE_H

SC_MODULE(sched_Scale) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > SIn;
  sc_port<tlm::tlm_fifo_put_if<int> > SOut;

  // Parameters

  SC_HAS_PROCESS(sched_Scale);

  sched_Scale(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
