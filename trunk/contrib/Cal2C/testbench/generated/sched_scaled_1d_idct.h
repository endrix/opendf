#ifndef SCHED_SCALED_1D_IDCT_H
#define SCHED_SCALED_1D_IDCT_H

SC_MODULE(sched_scaled_1d_idct) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > In;
  sc_port<tlm::tlm_fifo_put_if<int> > Out;

  // Parameters

  SC_HAS_PROCESS(sched_scaled_1d_idct);

  sched_scaled_1d_idct(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
