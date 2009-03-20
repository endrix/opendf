#include <systemc.h>
#include <tlm.h>
#include "sched_fread.h"

#ifdef __cplusplus
extern "C" {
#endif
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct fread_variables {
   int fd ;
   int nextc ;
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
void fread_untagged_action_0(struct fread_variables *_actor_variables , int *O ) ;

#ifdef __cplusplus
}
#endif

void sched_fread::process() {
/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

 
{ int _out_1 ;
  struct fread_variables *_actor_variables ;
  int _call_4 ;
  int _call_5 ;

  {
  _actor_variables = (struct fread_variables *)malloc(8);
  _call_4 = openFile("data/foreman_qcif_30.bit");
  _actor_variables->fd = _call_4;
  _call_5 = readByte(_actor_variables->fd);
  _actor_variables->nextc = _call_5;
  while (1) {
    if (_actor_variables->nextc >= 0) {
      fread_untagged_action_0(_actor_variables, & _out_1);
      libcal_printf("action fread_untagged_action_0: put value %i to O\n", _out_1);
      O->put(_out_1);
      libcal_printf("action fread_untagged_action_0: put to O OK\n");
    }
  }
}
}

}
