#ifndef SCHED_MVRECONSTRUCT_H
#define SCHED_MVRECONSTRUCT_H

SC_MODULE(sched_MVReconstruct) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_get_if<int> > MVIN;
  sc_port<tlm::tlm_fifo_get_if<int> > A;
  sc_port<tlm::tlm_fifo_put_if<int> > MV;

  // Variable parameters


  SC_HAS_PROCESS(sched_MVReconstruct);

  sched_MVReconstruct(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
