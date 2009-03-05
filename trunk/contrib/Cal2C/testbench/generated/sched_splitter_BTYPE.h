#ifndef SCHED_SPLITTER_BTYPE_H
#define SCHED_SPLITTER_BTYPE_H

SC_MODULE(sched_splitter_BTYPE) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > Y;
  sc_port<tlm::tlm_fifo_put_if<int> > U;
  sc_port<tlm::tlm_fifo_put_if<int> > V;

  // Variable parameters


  SC_HAS_PROCESS(sched_splitter_BTYPE);

  sched_splitter_BTYPE(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
