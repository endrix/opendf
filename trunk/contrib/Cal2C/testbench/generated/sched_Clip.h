#ifndef SCHED_CLIP_H
#define SCHED_CLIP_H

SC_MODULE(sched_Clip) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > I;
  sc_port<tlm::tlm_fifo_get_if<int> > SIGNED;
  sc_port<tlm::tlm_fifo_put_if<int> > O;

  // Parameters

  SC_HAS_PROCESS(sched_Clip);

  sched_Clip(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
