/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct MPEG4_algo_IAP_8x8_variables {
   int BUF_SIZE ;
   int acpred_flag ;
   int top ;
   int pred_ptr ;
   int count ;
   int ptr ;
   int buf[1968] ;
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
void MPEG4_algo_IAP_8x8_advance(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ) ;
void MPEG4_algo_IAP_8x8_copy(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                             int ac , int *QF_AC ) ;
void MPEG4_algo_IAP_8x8_newvop(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                               int s ) ;
void MPEG4_algo_IAP_8x8_skip(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                             int s ) ;
void MPEG4_algo_IAP_8x8_start(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                              int s , int p ) ;
void MPEG4_algo_IAP_8x8_advance(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ) 
{ int _if_2 ;

  {
  if (_actor_variables->ptr == 1) {
    _if_2 = _actor_variables->BUF_SIZE - 1;
  } else {
    _if_2 = _actor_variables->ptr - 1;
  }
  _actor_variables->ptr = _if_2;
}
}
void MPEG4_algo_IAP_8x8_copy(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                             int ac , int *QF_AC ) 
{ int pred ;
  int v ;
  int h ;
  int top_edge ;
  int left_edge ;
  int index ;
  int _if_10 ;

  {
  pred = ac;
  v = _actor_variables->count & 7;
  h = (_actor_variables->count >> 3) & 7;
  top_edge = h == 0;
  left_edge = v == 0;
  if (top_edge) {
    _if_10 = v;
  } else {
    _if_10 = h | 8;
  }
  index = _if_10;
  if (_actor_variables->acpred_flag && ((_actor_variables->top && top_edge) || (! _actor_variables->top && left_edge))) {
    pred += _actor_variables->buf[(_actor_variables->pred_ptr << 4) | index];
  }
  if (left_edge || top_edge) {
    _actor_variables->buf[(_actor_variables->ptr << 4) | index] = pred;
  }
  (_actor_variables->count) ++;
  *QF_AC = pred;
}
}
void MPEG4_algo_IAP_8x8_newvop(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                               int s ) 
{ 

  {
  _actor_variables->ptr = 1;
}
}
void MPEG4_algo_IAP_8x8_skip(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                             int s ) 
{ 

  {
  _actor_variables->count = 64;
}
}
void MPEG4_algo_IAP_8x8_start(struct MPEG4_algo_IAP_8x8_variables *_actor_variables ,
                              int s , int p ) 
{ 

  {
  _actor_variables->count = 1;
  _actor_variables->pred_ptr = p;
  _actor_variables->top = s == 2;
  _actor_variables->acpred_flag = s != 0;
}
}
