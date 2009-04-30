#include <systemc.h>
#include <tlm.h>
#include "sched_byte2bit.h"

#ifdef __cplusplus
extern "C" {
#endif
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct byte2bit_variables {
   int buf ;
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
void byte2bit_reload(struct byte2bit_variables *_actor_variables , int i ) ;
void byte2bit_shift(struct byte2bit_variables *_actor_variables , int *out ) ;

#ifdef __cplusplus
}
#endif

void sched_byte2bit::process() {
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

 
{ int _out_1 ;
  int _token_1 ;
  int _call_4 ;
  struct byte2bit_variables *_actor_variables ;

  {
  _actor_variables = (struct byte2bit_variables *)malloc(8);
  _actor_variables->count = 0;
  while (1) {
    if (_actor_variables->count == 0) {
      libcal_printf("action byte2bit_reload: get from in8\n");
      _call_4 = in8->get();
      _token_1 = _call_4;
      libcal_printf("action byte2bit_reload: got %i from in8\n", _token_1);
      byte2bit_reload(_actor_variables, _token_1);
    } else {
      if (_actor_variables->count != 0) {
        byte2bit_shift(_actor_variables, & _out_1);
        libcal_printf("action byte2bit_shift: put value %i to out\n", _out_1);
        out->put(_out_1);
        libcal_printf("action byte2bit_shift: put to out OK\n");
      }
    }
  }
}
}

}
