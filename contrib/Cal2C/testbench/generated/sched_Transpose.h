#ifndef SCHED_TRANSPOSE_H
#define SCHED_TRANSPOSE_H

SC_MODULE(sched_Transpose) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > In;
  sc_port<tlm::tlm_fifo_put_if<int> > Out;

  // Parameters

  SC_HAS_PROCESS(sched_Transpose);

  sched_Transpose(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
