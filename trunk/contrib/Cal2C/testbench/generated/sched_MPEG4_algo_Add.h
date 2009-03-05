#ifndef SCHED_MPEG4_ALGO_ADD_H
#define SCHED_MPEG4_ALGO_ADD_H

SC_MODULE(sched_MPEG4_algo_Add) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > MOT;
  sc_port<tlm::tlm_fifo_get_if<int> > TEX;
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > VID;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_algo_Add);

  sched_MPEG4_algo_Add(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
