#ifndef SCHED_SPLITTER_420_B_H
#define SCHED_SPLITTER_420_B_H

SC_MODULE(sched_splitter_420_B) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > B;
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > Y;
  sc_port<tlm::tlm_fifo_put_if<int> > U;
  sc_port<tlm::tlm_fifo_put_if<int> > V;

  // Variable parameters


  SC_HAS_PROCESS(sched_splitter_420_B);

  sched_splitter_420_B(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
