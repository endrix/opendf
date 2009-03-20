#ifndef SCHED_PARSER_H
#define SCHED_PARSER_H

#include "broadcaster.h"
#include "sched_BlockExpand.h"
#include "sched_MVReconstruct.h"
#include "sched_MVSequence.h"
#include "sched_ParseHeaders.h"
#include "sched_splitter_420_B.h"
#include "sched_splitter_BTYPE.h"
#include "sched_splitter_MV.h"

SC_MODULE(sched_parser) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > BITS;
  sc_port<tlm::tlm_fifo_put_if<int> > BTYPE_Y;
  sc_port<tlm::tlm_fifo_put_if<int> > BTYPE_U;
  sc_port<tlm::tlm_fifo_put_if<int> > BTYPE_V;
  sc_port<tlm::tlm_fifo_put_if<int> > MV_Y;
  sc_port<tlm::tlm_fifo_put_if<int> > MV_U;
  sc_port<tlm::tlm_fifo_put_if<int> > MV_V;
  sc_port<tlm::tlm_fifo_put_if<int> > B_Y;
  sc_port<tlm::tlm_fifo_put_if<int> > B_U;
  sc_port<tlm::tlm_fifo_put_if<int> > B_V;

  // Sub-module instantiation
  broadcaster_output broadcaster_output1;
  sched_ParseHeaders parseheaders;
  sched_MVSequence mvseq;
  sched_BlockExpand blkexp;
  sched_MVReconstruct mvrecon;
  sched_splitter_BTYPE splitter_BTYPE;
  sched_splitter_MV splitter_MV;
  sched_splitter_420_B splitter_420_B;

  // Local FIFO channels
  tlm::tlm_fifo<int> parseheaders_btype_splitter_BTYPE_btype;
  tlm::tlm_fifo<int> parseheaders_btype_mvseq_btype;
  tlm::tlm_fifo<int> parseheaders_btype_mvrecon_btype;
  tlm::tlm_fifo<int> parseheaders_MV_mvrecon_MVIN;
  tlm::tlm_fifo<int> parseheaders_RUN_blkexp_RUN;
  tlm::tlm_fifo<int> parseheaders_VALUE_blkexp_VALUE;
  tlm::tlm_fifo<int> parseheaders_LAST_blkexp_LAST;
  tlm::tlm_fifo<int> mvseq_A_mvrecon_A;
  tlm::tlm_fifo<int> blkexp_out_splitter_420_B_B;
  tlm::tlm_fifo<int> parseheaders_btype_splitter_420_B_btype;
  tlm::tlm_fifo<int> mvrecon_MV_splitter_MV_MV;
  tlm::tlm_fifo<int> parseheaders_btype_splitter_MV_btype;

  SC_HAS_PROCESS(sched_parser);

  sched_parser(sc_module_name N) : sc_module(N),

    // Creates the modules
    broadcaster_output1("broadcaster_output1"),
    parseheaders("parseheaders"),
    mvseq("mvseq"),
    blkexp("blkexp"),
    mvrecon("mvrecon"),
    splitter_BTYPE("splitter_BTYPE"),
    splitter_MV("splitter_MV"),
    splitter_420_B("splitter_420_B"),

    // Initializes FIFOs size
    parseheaders_btype_splitter_BTYPE_btype(-16),
    parseheaders_btype_mvseq_btype(-16),
    parseheaders_btype_mvrecon_btype(-16),
    parseheaders_MV_mvrecon_MVIN(-16),
    parseheaders_RUN_blkexp_RUN(-16),
    parseheaders_VALUE_blkexp_VALUE(-16),
    parseheaders_LAST_blkexp_LAST(-16),
    mvseq_A_mvrecon_A(-16),
    blkexp_out_splitter_420_B_B(-16),
    parseheaders_btype_splitter_420_B_btype(-16),
    mvrecon_MV_splitter_MV_MV(-16),
    parseheaders_btype_splitter_MV_btype(-16)
  {
    // Connects FIFOs and ports
    blkexp.LAST(parseheaders_LAST_blkexp_LAST);
    blkexp.RUN(parseheaders_RUN_blkexp_RUN);
    blkexp.VALUE(parseheaders_VALUE_blkexp_VALUE);
    blkexp.out(blkexp_out_splitter_420_B_B);
    mvrecon.A(mvseq_A_mvrecon_A);
    mvrecon.MV(mvrecon_MV_splitter_MV_MV);
    mvrecon.MVIN(parseheaders_MV_mvrecon_MVIN);
    mvrecon.btype(parseheaders_btype_mvrecon_btype);
    mvseq.A(mvseq_A_mvrecon_A);
    mvseq.btype(parseheaders_btype_mvseq_btype);
    parseheaders.LAST(parseheaders_LAST_blkexp_LAST);
    parseheaders.MV(parseheaders_MV_mvrecon_MVIN);
    parseheaders.RUN(parseheaders_RUN_blkexp_RUN);
    parseheaders.VALUE(parseheaders_VALUE_blkexp_VALUE);
    parseheaders.bits(BITS);
    parseheaders.btype(broadcaster_output1.input);
    broadcaster_output1.output(parseheaders_btype_mvrecon_btype);
    broadcaster_output1.output(parseheaders_btype_mvseq_btype);
    broadcaster_output1.output(parseheaders_btype_splitter_420_B_btype);
    broadcaster_output1.output(parseheaders_btype_splitter_BTYPE_btype);
    broadcaster_output1.output(parseheaders_btype_splitter_MV_btype);
    splitter_420_B.B(blkexp_out_splitter_420_B_B);
    splitter_420_B.U(B_U);
    splitter_420_B.V(B_V);
    splitter_420_B.Y(B_Y);
    splitter_420_B.btype(parseheaders_btype_splitter_420_B_btype);
    splitter_BTYPE.U(BTYPE_U);
    splitter_BTYPE.V(BTYPE_V);
    splitter_BTYPE.Y(BTYPE_Y);
    splitter_BTYPE.btype(parseheaders_btype_splitter_BTYPE_btype);
    splitter_MV.MV(mvrecon_MV_splitter_MV_MV);
    splitter_MV.U(MV_U);
    splitter_MV.V(MV_V);
    splitter_MV.Y(MV_Y);
    splitter_MV.btype(parseheaders_btype_splitter_MV_btype);
  }

};

#endif
