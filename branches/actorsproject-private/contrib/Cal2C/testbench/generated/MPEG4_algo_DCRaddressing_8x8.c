/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MPEG4_algo_DCRaddressing_8x8_variables {
   int BUF_SIZE ;
   int ptr_left ;
   int mbwidth ;
   int ptr_above ;
   int left_edge ;
   int mbx ;
   int ptr_above_left ;
   int top_edge ;
   int ptr ;
   int coded[123] ;
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
void MPEG4_algo_DCRaddressing_8x8_advance(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ) ;
int MPEG4_algo_DCRaddressing_8x8_function_decrement(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                    int p ) ;
void MPEG4_algo_DCRaddressing_8x8_geth(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                       int h ) ;
void MPEG4_algo_DCRaddressing_8x8_getw(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                       int w ) ;
void MPEG4_algo_DCRaddressing_8x8_predict(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                          int *A , int *B , int *C ) ;
void MPEG4_algo_DCRaddressing_8x8_read_dot_intra(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                 int type ) ;
void MPEG4_algo_DCRaddressing_8x8_read_dot_other(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                 int type ) ;
void MPEG4_algo_DCRaddressing_8x8_start(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                        int cmd ) ;
void MPEG4_algo_DCRaddressing_8x8_advance(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ) 
{ int _call_2 ;
  int _call_3 ;
  int _call_4 ;
  int _call_5 ;

  {
  (_actor_variables->mbx) ++;
  _actor_variables->left_edge = 0;
  if (_actor_variables->mbx == _actor_variables->mbwidth) {
    _actor_variables->mbx = 0;
    _actor_variables->top_edge = 0;
    _actor_variables->left_edge = 1;
  }
  _call_2 = MPEG4_algo_DCRaddressing_8x8_function_decrement(_actor_variables, _actor_variables->ptr);
  _actor_variables->ptr = _call_2;
  _call_3 = MPEG4_algo_DCRaddressing_8x8_function_decrement(_actor_variables, _actor_variables->ptr_left);
  _actor_variables->ptr_left = _call_3;
  _call_4 = MPEG4_algo_DCRaddressing_8x8_function_decrement(_actor_variables, _actor_variables->ptr_above);
  _actor_variables->ptr_above = _call_4;
  _call_5 = MPEG4_algo_DCRaddressing_8x8_function_decrement(_actor_variables, _actor_variables->ptr_above_left);
  _actor_variables->ptr_above_left = _call_5;
}
}
int MPEG4_algo_DCRaddressing_8x8_function_decrement(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                    int p ) 
{ int res ;
  int _if_4 ;

  {
  if (p == 1) {
    _if_4 = _actor_variables->BUF_SIZE - 1;
  } else {
    _if_4 = p - 1;
  }
  res = _if_4;
  return (res);
}
}
void MPEG4_algo_DCRaddressing_8x8_geth(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                       int h ) 
{ 

  {

}
}
void MPEG4_algo_DCRaddressing_8x8_getw(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                       int w ) 
{ 

  {
  _actor_variables->mbwidth = w;
  _actor_variables->ptr = 1;
  _actor_variables->ptr_left = 2;
  _actor_variables->ptr_above = 1 + w;
  _actor_variables->ptr_above_left = 2 + w;
}
}
void MPEG4_algo_DCRaddressing_8x8_predict(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                          int *A , int *B , int *C ) 
{ int a ;
  int b ;
  int c ;

  {
  a = 0;
  b = 0;
  c = 0;
  if (! _actor_variables->left_edge) {
    a = _actor_variables->ptr_left;
    if (! _actor_variables->coded[a]) {
      a = 0;
    }
    if (! _actor_variables->top_edge) {
      b = _actor_variables->ptr_above_left;
      if (! _actor_variables->coded[b]) {
        b = 0;
      }
    }
  }
  if (! _actor_variables->top_edge) {
    c = _actor_variables->ptr_above;
    if (! _actor_variables->coded[c]) {
      c = 0;
    }
  }
  *A = a;
  *B = b;
  *C = c;
}
}
void MPEG4_algo_DCRaddressing_8x8_read_dot_intra(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                 int type ) 
{ 

  {
  _actor_variables->coded[_actor_variables->ptr] = 1;
}
}
void MPEG4_algo_DCRaddressing_8x8_read_dot_other(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                                 int type ) 
{ 

  {
  _actor_variables->coded[_actor_variables->ptr] = 0;
}
}
void MPEG4_algo_DCRaddressing_8x8_start(struct MPEG4_algo_DCRaddressing_8x8_variables *_actor_variables ,
                                        int cmd ) 
{ 

  {
  _actor_variables->mbx = 0;
  _actor_variables->top_edge = 1;
  _actor_variables->left_edge = 1;
}
}