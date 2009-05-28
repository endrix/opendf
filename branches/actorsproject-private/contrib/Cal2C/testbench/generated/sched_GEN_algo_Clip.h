#ifndef SCHED_GEN_ALGO_CLIP_H
#define SCHED_GEN_ALGO_CLIP_H

SC_MODULE(sched_GEN_algo_Clip) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > I;
  sc_port<tlm::tlm_fifo_get_if<int> > SIGNED;
  sc_port<tlm::tlm_fifo_put_if<int> > O;

  // Variable parameters


  SC_HAS_PROCESS(sched_GEN_algo_Clip);

  sched_GEN_algo_Clip(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
