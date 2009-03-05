#ifndef SCHED_BLOCKEXPAND_H
#define SCHED_BLOCKEXPAND_H

SC_MODULE(sched_BlockExpand) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > RUN;
  sc_port<tlm::tlm_fifo_get_if<int> > VALUE;
  sc_port<tlm::tlm_fifo_get_if<int> > LAST;
  sc_port<tlm::tlm_fifo_put_if<int> > out;

  // Variable parameters


  SC_HAS_PROCESS(sched_BlockExpand);

  sched_BlockExpand(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
