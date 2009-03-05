#ifndef SCHED_DISPYUV_H
#define SCHED_DISPYUV_H

SC_MODULE(sched_DispYUV) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > B;

  // Variable parameters


  SC_HAS_PROCESS(sched_DispYUV);

  sched_DispYUV(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
