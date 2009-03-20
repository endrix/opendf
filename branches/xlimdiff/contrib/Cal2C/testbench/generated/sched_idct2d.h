#ifndef SCHED_IDCT2D_H
#define SCHED_IDCT2D_H

#include "broadcaster.h"
#include "sched_GEN_124_algo_Idct1d.h"
#include "sched_GEN_algo_Clip.h"
#include "sched_GEN_algo_Transpose.h"

SC_MODULE(sched_idct2d) {
  // Input and output ports
  sc_port<tlm::tlm_fifo_get_if<int> > _in_;
  sc_port<tlm::tlm_fifo_get_if<int> > _cal_signed;
  sc_port<tlm::tlm_fifo_put_if<int> > out;

  // Sub-module instantiation
  sched_GEN_124_algo_Idct1d row;
  sched_GEN_algo_Transpose transpose;
  sched_GEN_124_algo_Idct1d column;
  sched_GEN_algo_Transpose retranspose;
  sched_GEN_algo_Clip clip;

  // Local FIFO channels
  tlm::tlm_fifo<int> row_Y_transpose_X;
  tlm::tlm_fifo<int> transpose_Y_column_X;
  tlm::tlm_fifo<int> column_Y_retranspose_X;
  tlm::tlm_fifo<int> retranspose_Y_clip_I;

  SC_HAS_PROCESS(sched_idct2d);

  sched_idct2d(sc_module_name N) : sc_module(N),

    // Creates the modules
    row("row", true),
    transpose("transpose"),
    column("column", false),
    retranspose("retranspose"),
    clip("clip"),

    // Initializes FIFOs size
    row_Y_transpose_X(-16),
    transpose_Y_column_X(-16),
    column_Y_retranspose_X(-16),
    retranspose_Y_clip_I(-16)
  {
    // Connects FIFOs and ports
    clip.I(retranspose_Y_clip_I);
    clip.O(out);
    clip.SIGNED(_cal_signed);
    column.X(transpose_Y_column_X);
    column.Y(column_Y_retranspose_X);
    retranspose.X(column_Y_retranspose_X);
    retranspose.Y(retranspose_Y_clip_I);
    row.X(_in_);
    row.Y(row_Y_transpose_X);
    transpose.X(row_Y_transpose_X);
    transpose.Y(transpose_Y_column_X);
  }

};

#endif
