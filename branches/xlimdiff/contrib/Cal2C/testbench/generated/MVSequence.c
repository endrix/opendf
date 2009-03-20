/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MVSequence_variables {
   int BUF_SIZE ;
   int ptr_left ;
   int mbwidth ;
   int ptr_above ;
   int ptr_above_right ;
   int mbx ;
   int comp ;
   int right_edge ;
   int top_edge ;
   int ptr ;
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
int MVSequence_function_access(struct MVSequence_variables *_actor_variables , int mbptr ,
                               int c ) ;
int MVSequence_function_decrement(struct MVSequence_variables *_actor_variables ,
                                  int p ) ;
void MVSequence_geth(struct MVSequence_variables *_actor_variables , int h ) ;
void MVSequence_getw(struct MVSequence_variables *_actor_variables , int w ) ;
void MVSequence_read_dot_noPredict(struct MVSequence_variables *_actor_variables ,
                                   int cmd ) ;
void MVSequence_read_dot_predict_dot_y0(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) ;
void MVSequence_read_dot_predict_dot_y1(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) ;
void MVSequence_read_dot_predict_dot_y2(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) ;
void MVSequence_read_dot_predict_dot_y3(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) ;
void MVSequence_start(struct MVSequence_variables *_actor_variables , int cmd ) ;
int MVSequence_function_access(struct MVSequence_variables *_actor_variables , int mbptr ,
                               int c ) 
{ int res ;

  {
  res = (mbptr << 3) | (c & 3);
  return (res);
}
}
int MVSequence_function_decrement(struct MVSequence_variables *_actor_variables ,
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
void MVSequence_geth(struct MVSequence_variables *_actor_variables , int h ) 
{ 

  {

}
}
void MVSequence_getw(struct MVSequence_variables *_actor_variables , int w ) 
{ 

  {
  _actor_variables->mbwidth = w;
  _actor_variables->ptr = 1;
  _actor_variables->ptr_left = 2;
  _actor_variables->ptr_above = w + 1;
  _actor_variables->ptr_above_right = w;
}
}
void MVSequence_read_dot_noPredict(struct MVSequence_variables *_actor_variables ,
                                   int cmd ) 
{ int _call_3 ;
  int _call_4 ;
  int _call_5 ;
  int _call_6 ;

  {
  (_actor_variables->comp) ++;
  if (_actor_variables->comp == 6) {
    _actor_variables->comp = 0;
    (_actor_variables->mbx) ++;
    _call_3 = MVSequence_function_decrement(_actor_variables, _actor_variables->ptr);
    _actor_variables->ptr = _call_3;
    _call_4 = MVSequence_function_decrement(_actor_variables, _actor_variables->ptr_left);
    _actor_variables->ptr_left = _call_4;
    _call_5 = MVSequence_function_decrement(_actor_variables, _actor_variables->ptr_above);
    _actor_variables->ptr_above = _call_5;
    _call_6 = MVSequence_function_decrement(_actor_variables, _actor_variables->ptr_above_right);
    _actor_variables->ptr_above_right = _call_6;
    if (_actor_variables->right_edge) {
      _actor_variables->mbx = 0;
      _actor_variables->right_edge = 0;
      _actor_variables->top_edge = 0;
    } else {
      if (_actor_variables->mbx == _actor_variables->mbwidth - 1) {
        _actor_variables->right_edge = 1;
      }
    }
  }
}
}
void MVSequence_read_dot_predict_dot_y0(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) 
{ int pl ;
  int pa ;
  int par ;
  int _if_7 ;
  int _if_8 ;
  int _if_9 ;
  int _call_10 ;
  int _call_11 ;
  int _call_12 ;

  {
  if (_actor_variables->mbx == 0) {
    _if_7 = 0;
  } else {
    _if_7 = _actor_variables->ptr_left;
  }
  pl = _if_7;
  if (_actor_variables->top_edge) {
    _if_8 = 0;
  } else {
    _if_8 = _actor_variables->ptr_above;
  }
  pa = _if_8;
  if (_actor_variables->top_edge || _actor_variables->right_edge) {
    _if_9 = 0;
  } else {
    _if_9 = _actor_variables->ptr_above_right;
  }
  par = _if_9;
  (_actor_variables->comp) ++;
  _call_10 = MVSequence_function_access(_actor_variables, pl, 1);
  A[0] = _call_10;
  _call_11 = MVSequence_function_access(_actor_variables, pa, 2);
  A[1] = _call_11;
  _call_12 = MVSequence_function_access(_actor_variables, par, 2);
  A[2] = _call_12;
}
}
void MVSequence_read_dot_predict_dot_y1(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) 
{ int pa ;
  int par ;
  int _if_6 ;
  int _if_7 ;
  int _call_8 ;
  int _call_9 ;
  int _call_10 ;

  {
  if (_actor_variables->top_edge) {
    _if_6 = 0;
  } else {
    _if_6 = _actor_variables->ptr_above;
  }
  pa = _if_6;
  if (_actor_variables->top_edge || _actor_variables->right_edge) {
    _if_7 = 0;
  } else {
    _if_7 = _actor_variables->ptr_above_right;
  }
  par = _if_7;
  (_actor_variables->comp) ++;
  _call_8 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 0);
  A[0] = _call_8;
  _call_9 = MVSequence_function_access(_actor_variables, pa, 3);
  A[1] = _call_9;
  _call_10 = MVSequence_function_access(_actor_variables, par, 2);
  A[2] = _call_10;
}
}
void MVSequence_read_dot_predict_dot_y2(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) 
{ int pl ;
  int _if_5 ;
  int _call_6 ;
  int _call_7 ;
  int _call_8 ;

  {
  if (_actor_variables->mbx == 0) {
    _if_5 = 0;
  } else {
    _if_5 = _actor_variables->ptr_left;
  }
  pl = _if_5;
  (_actor_variables->comp) ++;
  _call_6 = MVSequence_function_access(_actor_variables, pl, 3);
  A[0] = _call_6;
  _call_7 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 0);
  A[1] = _call_7;
  _call_8 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 1);
  A[2] = _call_8;
}
}
void MVSequence_read_dot_predict_dot_y3(struct MVSequence_variables *_actor_variables ,
                                        int cmd , int A[3] ) 
{ int _call_4 ;
  int _call_5 ;
  int _call_6 ;

  {
  (_actor_variables->comp) ++;
  _call_4 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 2);
  A[0] = _call_4;
  _call_5 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 0);
  A[1] = _call_5;
  _call_6 = MVSequence_function_access(_actor_variables, _actor_variables->ptr, 1);
  A[2] = _call_6;
}
}
void MVSequence_start(struct MVSequence_variables *_actor_variables , int cmd ) 
{ 

  {
  _actor_variables->mbx = 0;
  _actor_variables->top_edge = 1;
  _actor_variables->right_edge = 0;
  _actor_variables->comp = 0;
}
}
