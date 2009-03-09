#ifndef SCHED_FREAD_H
#define SCHED_FREAD_H

SC_MODULE(sched_fread) {
  // FIFOs
  sc_port<tlm::tlm_fifo_put_if<int> > O;

  // Variable parameters


  SC_HAS_PROCESS(sched_fread);

  sched_fread(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
