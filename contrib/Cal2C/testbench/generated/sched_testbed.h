#ifndef SCHED_TESTBED_H
#define SCHED_TESTBED_H

#include "broadcaster.h"
#include "sched_DispYUV.h"
#include "sched_decoder.h"
#include "sched_fread.h"

SC_MODULE(sched_testbed) {
  // Input and output ports


  // Sub-module instantiation
  sched_decoder decoder;
  sched_fread source;
  sched_DispYUV display;

  // Local FIFO channels
  tlm::tlm_fifo<int> source_O_decoder_bits;
  tlm::tlm_fifo<int> decoder_VID_display_B;

  SC_HAS_PROCESS(sched_testbed);

  sched_testbed(sc_module_name N) : sc_module(N),

    // Creates the modules
    decoder("decoder"),
    source("source"),
    display("display"),

    // Initializes FIFOs size
    source_O_decoder_bits(100),
    decoder_VID_display_B(-16)
  {
    // Connects FIFOs and ports
    decoder.VID(decoder_VID_display_B);
    decoder.bits(source_O_decoder_bits);
    display.B(decoder_VID_display_B);
    source.O(source_O_decoder_bits);
  }

};

#endif
