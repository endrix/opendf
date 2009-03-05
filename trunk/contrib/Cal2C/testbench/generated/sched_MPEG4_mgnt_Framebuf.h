#ifndef SCHED_MPEG4_MGNT_FRAMEBUF_H
#define SCHED_MPEG4_MGNT_FRAMEBUF_H

SC_MODULE(sched_MPEG4_mgnt_Framebuf) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > RA;
  sc_port<tlm::tlm_fifo_get_if<int> > WA;
  sc_port<tlm::tlm_fifo_get_if<int> > WD;
  sc_port<tlm::tlm_fifo_put_if<int> > RD;

  // Variable parameters


  SC_HAS_PROCESS(sched_MPEG4_mgnt_Framebuf);

  sched_MPEG4_mgnt_Framebuf(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
