/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MPEG4_algo_Inversequant_variables {
   int round ;
   int count ;
   int quant ;
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
void MPEG4_algo_Inversequant_ac(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                int i , int *out ) ;
int MPEG4_algo_Inversequant_done(struct MPEG4_algo_Inversequant_variables *_actor_variables ) ;
int MPEG4_algo_Inversequant_function_abs(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                         int x ) ;
int MPEG4_algo_Inversequant_function_saturate(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                              int x ) ;
void MPEG4_algo_Inversequant_get_qp(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                    int q , int i , int *out ) ;
void MPEG4_algo_Inversequant_ac(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                int i , int *out ) 
{ int v ;
  int o ;
  int _call_6 ;
  int _if_7 ;
  int _if_8 ;
  int _call_9 ;

  {
  _call_6 = MPEG4_algo_Inversequant_function_abs(_actor_variables, i);
  v = _actor_variables->quant * ((_call_6 << 1) + 1) - _actor_variables->round;
  if (i == 0) {
    _if_8 = 0;
  } else {
    if (i < 0) {
      _if_7 = - v;
    } else {
      _if_7 = v;
    }
    _if_8 = _if_7;
  }
  o = _if_8;
  (_actor_variables->count) ++;
  _call_9 = MPEG4_algo_Inversequant_function_saturate(_actor_variables, o);
  *out = _call_9;
}
}
int MPEG4_algo_Inversequant_done(struct MPEG4_algo_Inversequant_variables *_actor_variables ) 
{ int res ;

  {

}
}
int MPEG4_algo_Inversequant_function_abs(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                         int x ) 
{ int res ;
  int _if_4 ;

  {
  if (x < 0) {
    _if_4 = - x;
  } else {
    _if_4 = x;
  }
  res = _if_4;
  return (res);
}
}
int MPEG4_algo_Inversequant_function_saturate(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                              int x ) 
{ int minus ;
  int plus ;
  int res ;
  int _if_6 ;
  int _if_7 ;

  {
  minus = x < -2048;
  plus = x > 2047;
  if (minus) {
    _if_7 = -2048;
  } else {
    if (plus) {
      _if_6 = 2047;
    } else {
      _if_6 = x;
    }
    _if_7 = _if_6;
  }
  res = _if_7;
  return (res);
}
}
void MPEG4_algo_Inversequant_get_qp(struct MPEG4_algo_Inversequant_variables *_actor_variables ,
                                    int q , int i , int *out ) 
{ 

  {
  _actor_variables->quant = q;
  _actor_variables->round = (q & 1) ^ 1;
  _actor_variables->count = 0;
  *out = i;
}
}
