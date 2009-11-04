/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MPEG4_algo_IS_variables {
   int BUF_SIZE ;
   int addr ;
   int Scanmode[192] ;
   int add_buf ;
   int half ;
   int count ;
   int buf[128] ;
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
void MPEG4_algo_IS_done(struct MPEG4_algo_IS_variables *_actor_variables ) ;
int MPEG4_algo_IS_function_ra(struct MPEG4_algo_IS_variables *_actor_variables , int addr ) ;
int MPEG4_algo_IS_function_wa(struct MPEG4_algo_IS_variables *_actor_variables ) ;
void MPEG4_algo_IS_read_only(struct MPEG4_algo_IS_variables *_actor_variables , int ac ) ;
void MPEG4_algo_IS_read_write(struct MPEG4_algo_IS_variables *_actor_variables , int ac ,
                              int *PQF_AC ) ;
void MPEG4_algo_IS_skip(struct MPEG4_algo_IS_variables *_actor_variables , int i ) ;
void MPEG4_algo_IS_start(struct MPEG4_algo_IS_variables *_actor_variables , int i ) ;
void MPEG4_algo_IS_write_only(struct MPEG4_algo_IS_variables *_actor_variables , int *PQF_AC ) ;
void MPEG4_algo_IS_done(struct MPEG4_algo_IS_variables *_actor_variables ) 
{ int _if_2 ;
  int _if_3 ;

  {
  _actor_variables->count = 1;
  _actor_variables->half = ! _actor_variables->half;
  if (_actor_variables->add_buf == 0) {
    _if_3 = 0;
  } else {
    if (_actor_variables->add_buf == 1) {
      _if_2 = 64;
    } else {
      _if_2 = 128;
    }
    _if_3 = _if_2;
  }
  _actor_variables->addr = _if_3;
}
}
int MPEG4_algo_IS_function_ra(struct MPEG4_algo_IS_variables *_actor_variables , int addr ) 
{ int res ;
  int _if_4 ;

  {
  if (_actor_variables->half) {
    _if_4 = 0;
  } else {
    _if_4 = 64;
  }
  res = (addr & 63) | _if_4;
  return (res);
}
}
int MPEG4_algo_IS_function_wa(struct MPEG4_algo_IS_variables *_actor_variables ) 
{ int res ;
  int _if_3 ;

  {
  if (_actor_variables->half) {
    _if_3 = 64;
  } else {
    _if_3 = 0;
  }
  res = (_actor_variables->count & 63) | _if_3;
  return (res);
}
}
void MPEG4_algo_IS_read_only(struct MPEG4_algo_IS_variables *_actor_variables , int ac ) 
{ int _call_3 ;

  {
  _call_3 = MPEG4_algo_IS_function_wa(_actor_variables);
  _actor_variables->buf[_call_3] = ac;
  (_actor_variables->count) ++;
}
}
void MPEG4_algo_IS_read_write(struct MPEG4_algo_IS_variables *_actor_variables , int ac ,
                              int *PQF_AC ) 
{ int _call_4 ;
  int _call_5 ;

  {
  _call_4 = MPEG4_algo_IS_function_wa(_actor_variables);
  _actor_variables->buf[_call_4] = ac;
  (_actor_variables->count) ++;
  (_actor_variables->addr) ++;
  _call_5 = MPEG4_algo_IS_function_ra(_actor_variables, _actor_variables->Scanmode[_actor_variables->addr]);
  *PQF_AC = _actor_variables->buf[_call_5];
}
}
void MPEG4_algo_IS_skip(struct MPEG4_algo_IS_variables *_actor_variables , int i ) 
{ 

  {

}
}
void MPEG4_algo_IS_start(struct MPEG4_algo_IS_variables *_actor_variables , int i ) 
{ 

  {
  _actor_variables->add_buf = i;
}
}
void MPEG4_algo_IS_write_only(struct MPEG4_algo_IS_variables *_actor_variables , int *PQF_AC ) 
{ int _call_3 ;

  {
  (_actor_variables->addr) ++;
  (_actor_variables->count) ++;
  _call_3 = MPEG4_algo_IS_function_ra(_actor_variables, _actor_variables->Scanmode[_actor_variables->addr]);
  *PQF_AC = _actor_variables->buf[_call_3];
}
}