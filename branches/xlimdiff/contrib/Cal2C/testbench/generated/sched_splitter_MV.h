#ifndef SCHED_SPLITTER_MV_H
#define SCHED_SPLITTER_MV_H

SC_MODULE(sched_splitter_MV) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > MV;
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > Y;
  sc_port<tlm::tlm_fifo_put_if<int> > U;
  sc_port<tlm::tlm_fifo_put_if<int> > V;

  // Variable parameters


  SC_HAS_PROCESS(sched_splitter_MV);

  sched_splitter_MV(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
