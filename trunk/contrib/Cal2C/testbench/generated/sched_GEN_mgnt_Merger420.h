#ifndef SCHED_GEN_MGNT_MERGER420_H
#define SCHED_GEN_MGNT_MERGER420_H

SC_MODULE(sched_GEN_mgnt_Merger420) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > Y;
  sc_port<tlm::tlm_fifo_get_if<int> > U;
  sc_port<tlm::tlm_fifo_get_if<int> > V;
  sc_port<tlm::tlm_fifo_put_if<int> > YUV;

  // Variable parameters


  SC_HAS_PROCESS(sched_GEN_mgnt_Merger420);

  sched_GEN_mgnt_Merger420(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
