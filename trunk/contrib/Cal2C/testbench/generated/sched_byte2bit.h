#ifndef SCHED_BYTE2BIT_H
#define SCHED_BYTE2BIT_H

SC_MODULE(sched_byte2bit) {
  // FIFOs
  sc_port<tlm::tlm_fifo_get_if<int> > in8;
  sc_port<tlm::tlm_fifo_put_if<int> > out;

  // Variable parameters


  SC_HAS_PROCESS(sched_byte2bit);

  sched_byte2bit(sc_module_name N) : sc_module(N) {
    SC_THREAD(process);
  }

  void process();

};

#endif
