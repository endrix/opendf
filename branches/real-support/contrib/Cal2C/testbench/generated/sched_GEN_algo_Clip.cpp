#include <systemc.h>
#include <tlm.h>
#include "sched_GEN_algo_Clip.h"

#ifdef __cplusplus
extern "C" {
#endif
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct GEN_algo_Clip_variables {
   int sflag ;
   int count ;
};
void libcal_printf(char const   *format  , ...) ;
// To enable traces, change 1 by 0 in the #if below
#if 1
#define libcal_printf //
#endif
int currentSystemTime(void) ;
int openFile(char *file_name ) ;
void picture_displayImage(void) ;
void picture_setPixel(int x , int y , int r , int g , int b ) ;
int readByte(int fd ) ;
int JFrame(char *title ) ;
int Picture(int width , int height ) ;
void GEN_algo_Clip_limit_dot_max(struct GEN_algo_Clip_variables *_actor_variables ,
                                 int i , int *O ) ;
void GEN_algo_Clip_limit_dot_min(struct GEN_algo_Clip_variables *_actor_variables ,
                                 int i , int *O ) ;
void GEN_algo_Clip_limit_dot_none(struct GEN_algo_Clip_variables *_actor_variables ,
                                  int i , int *O ) ;
void GEN_algo_Clip_limit_dot_zero(struct GEN_algo_Clip_variables *_actor_variables ,
                                  int i , int *O ) ;
void GEN_algo_Clip_read_signed(struct GEN_algo_Clip_variables *_actor_variables ,
                               int s ) ;

#ifdef __cplusplus
}
#endif

void sched_GEN_algo_Clip::process() {
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

 
{ int _token_1 ;
  int _out_1 ;
  int _token_2 ;
  int _out_2 ;
  int _token_3 ;
  int _out_3 ;
  int _token_4 ;
  int _out_4 ;
  int _token_5 ;
  int _call_11 ;
  int _call_12 ;
  int _call_13 ;
  int _call_14 ;
  int _call_15 ;
  struct GEN_algo_Clip_variables *_actor_variables ;

  {
  _actor_variables = (struct GEN_algo_Clip_variables *)malloc(8);
  _actor_variables->count = -1;
  while (1) {
    if (_actor_variables->count < 0) {
      libcal_printf("action GEN_algo_Clip_read_signed: get from SIGNED\n");
      _call_11 = SIGNED->get();
      _token_5 = _call_11;
      libcal_printf("action GEN_algo_Clip_read_signed: got %i from SIGNED\n", _token_5);
      GEN_algo_Clip_read_signed(_actor_variables, _token_5);
    } else {
      if (I->peek() > 255) {
        libcal_printf("action GEN_algo_Clip_limit_dot_max: get from I\n");
        _call_12 = I->get();
        _token_4 = _call_12;
        libcal_printf("action GEN_algo_Clip_limit_dot_max: got %i from I\n", _token_4);
        GEN_algo_Clip_limit_dot_max(_actor_variables, _token_4, & _out_4);
        libcal_printf("action GEN_algo_Clip_limit_dot_max: put value %i to O\n", _out_4);
        O->put(_out_4);
        libcal_printf("action GEN_algo_Clip_limit_dot_max: put to O OK\n");
      } else {
        if (! _actor_variables->sflag && I->peek() < 0) {
          libcal_printf("action GEN_algo_Clip_limit_dot_zero: get from I\n");
          _call_13 = I->get();
          _token_3 = _call_13;
          libcal_printf("action GEN_algo_Clip_limit_dot_zero: got %i from I\n", _token_3);
          GEN_algo_Clip_limit_dot_zero(_actor_variables, _token_3, & _out_3);
          libcal_printf("action GEN_algo_Clip_limit_dot_zero: put value %i to O\n",
                        _out_3);
          O->put(_out_3);
          libcal_printf("action GEN_algo_Clip_limit_dot_zero: put to O OK\n");
        } else {
          if (I->peek() < - 255) {
            libcal_printf("action GEN_algo_Clip_limit_dot_min: get from I\n");
            _call_14 = I->get();
            _token_2 = _call_14;
            libcal_printf("action GEN_algo_Clip_limit_dot_min: got %i from I\n", _token_2);
            GEN_algo_Clip_limit_dot_min(_actor_variables, _token_2, & _out_2);
            libcal_printf("action GEN_algo_Clip_limit_dot_min: put value %i to O\n",
                          _out_2);
            O->put(_out_2);
            libcal_printf("action GEN_algo_Clip_limit_dot_min: put to O OK\n");
          } else {
            libcal_printf("action GEN_algo_Clip_limit_dot_none: get from I\n");
            _call_15 = I->get();
            _token_1 = _call_15;
            libcal_printf("action GEN_algo_Clip_limit_dot_none: got %i from I\n",
                          _token_1);
            GEN_algo_Clip_limit_dot_none(_actor_variables, _token_1, & _out_1);
            libcal_printf("action GEN_algo_Clip_limit_dot_none: put value %i to O\n",
                          _out_1);
            O->put(_out_1);
            libcal_printf("action GEN_algo_Clip_limit_dot_none: put to O OK\n");
          }
        }
      }
    }
  }
}
}

}
