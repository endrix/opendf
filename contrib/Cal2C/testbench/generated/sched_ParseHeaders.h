#ifndef SCHED_PARSEHEADERS_H
#define SCHED_PARSEHEADERS_H

SC_MODULE(sched_ParseHeaders) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > bits;
  sc_port<tlm::tlm_fifo_put_if<int> > btype;
  sc_port<tlm::tlm_fifo_put_if<int> > MV;
  sc_port<tlm::tlm_fifo_put_if<int> > RUN;
  sc_port<tlm::tlm_fifo_put_if<int> > VALUE;
  sc_port<tlm::tlm_fifo_put_if<int> > LAST;

  // Variable parameters


  SC_HAS_PROCESS(sched_ParseHeaders);

  sched_ParseHeaders(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
