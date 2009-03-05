#ifndef SCHED_MPEG4_MGNT_ADDRESS_H
#define SCHED_MPEG4_MGNT_ADDRESS_H

SC_MODULE(sched_MPEG4_mgnt_Address) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > MV;
  sc_port<tlm::tlm_fifo_get_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > RA;
  sc_port<tlm::tlm_fifo_put_if<int> > WA;
  sc_port<tlm::tlm_fifo_put_if<int> > halfpel;

  // Variable parameters
  int m_layout;


  SC_HAS_PROCESS(sched_MPEG4_mgnt_Address);

  sched_MPEG4_mgnt_Address(sc_module_name N, int LAYOUT) : sc_module(N), m_layout(LAYOUT) {
    SC_THREAD(process);
  }

  void process();

};

#endif
