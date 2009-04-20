/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MPEG4_algo_Interpolation_halfpel_variables {
   int x ;
   int y ;
   int round ;
   int flags ;
   int d0 ;
   int d1 ;
   int d2 ;
   int d3 ;
   int d4 ;
   int d5 ;
   int d6 ;
   int d7 ;
   int d8 ;
   int d9 ;
   int _CAL_tokenMonitor ;
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
int MPEG4_algo_Interpolation_halfpel_done(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ) ;
int MPEG4_algo_Interpolation_halfpel_function_compensate(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                                         int p00 , int p10 , int p01 ,
                                                         int p11 ) ;
void MPEG4_algo_Interpolation_halfpel_other(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                            int d , int *MOT ) ;
void MPEG4_algo_Interpolation_halfpel_row_col_0(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                                int d ) ;
void MPEG4_algo_Interpolation_halfpel_start(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                            int f ) ;
int MPEG4_algo_Interpolation_halfpel_done(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ) 
{ int res ;

  {

}
}
int MPEG4_algo_Interpolation_halfpel_function_compensate(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                                         int p00 , int p10 , int p01 ,
                                                         int p11 ) 
{ int res ;
  int _if_7 ;
  int _if_8 ;
  int _if_9 ;

  {
  if (_actor_variables->flags == 0) {
    _if_9 = p00;
  } else {
    if (_actor_variables->flags == 1) {
      _if_8 = (((p00 + p01) + 1) - _actor_variables->round) >> 1;
    } else {
      if (_actor_variables->flags == 2) {
        _if_7 = (((p00 + p10) + 1) - _actor_variables->round) >> 1;
      } else {
        _if_7 = (((((p00 + p10) + p01) + p11) + 2) - _actor_variables->round) >> 2;
      }
      _if_8 = _if_7;
    }
    _if_9 = _if_8;
  }
  res = _if_9;
  return (res);
}
}
void MPEG4_algo_Interpolation_halfpel_other(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                            int d , int *MOT ) 
{ int p ;
  int _call_5 ;

  {
  _call_5 = MPEG4_algo_Interpolation_halfpel_function_compensate(_actor_variables,
                                                                 _actor_variables->d9,
                                                                 _actor_variables->d8,
                                                                 _actor_variables->d0,
                                                                 d);
  p = _call_5;
  _actor_variables->d9 = _actor_variables->d8;
  _actor_variables->d8 = _actor_variables->d7;
  _actor_variables->d7 = _actor_variables->d6;
  _actor_variables->d6 = _actor_variables->d5;
  _actor_variables->d5 = _actor_variables->d4;
  _actor_variables->d4 = _actor_variables->d3;
  _actor_variables->d3 = _actor_variables->d2;
  _actor_variables->d2 = _actor_variables->d1;
  _actor_variables->d1 = _actor_variables->d0;
  _actor_variables->d0 = d;
  (_actor_variables->x) ++;
  if (_actor_variables->x >= 9) {
    _actor_variables->x = 0;
    (_actor_variables->y) ++;
  }
  *MOT = p;
}
}
void MPEG4_algo_Interpolation_halfpel_row_col_0(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                                int d ) 
{ 

  {
  _actor_variables->d9 = _actor_variables->d8;
  _actor_variables->d8 = _actor_variables->d7;
  _actor_variables->d7 = _actor_variables->d6;
  _actor_variables->d6 = _actor_variables->d5;
  _actor_variables->d5 = _actor_variables->d4;
  _actor_variables->d4 = _actor_variables->d3;
  _actor_variables->d3 = _actor_variables->d2;
  _actor_variables->d2 = _actor_variables->d1;
  _actor_variables->d1 = _actor_variables->d0;
  _actor_variables->d0 = d;
  (_actor_variables->x) ++;
  if (_actor_variables->x >= 9) {
    _actor_variables->x = 0;
    (_actor_variables->y) ++;
  }
}
}
void MPEG4_algo_Interpolation_halfpel_start(struct MPEG4_algo_Interpolation_halfpel_variables *_actor_variables ,
                                            int f ) 
{ 

  {
  _actor_variables->x = 0;
  _actor_variables->y = 0;
  _actor_variables->flags = f >> 1;
  _actor_variables->round = f & 1;
}
}