#ifndef SCHED_GEN_MGNT_DCSPLIT_H
#define SCHED_GEN_MGNT_DCSPLIT_H

SC_MODULE(sched_GEN_mgnt_DCSplit) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > in;
  sc_port<tlm::tlm_fifo_put_if<int> > DC;
  sc_port<tlm::tlm_fifo_put_if<int> > AC;

  // Variable parameters


  SC_HAS_PROCESS(sched_GEN_mgnt_DCSplit);

  sched_GEN_mgnt_DCSplit(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
