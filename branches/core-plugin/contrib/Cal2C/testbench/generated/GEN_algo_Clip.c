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
void GEN_algo_Clip_limit_dot_max(struct GEN_algo_Clip_variables *_actor_variables ,
                                 int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = 255;
}
}
void GEN_algo_Clip_limit_dot_min(struct GEN_algo_Clip_variables *_actor_variables ,
                                 int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = -255;
}
}
void GEN_algo_Clip_limit_dot_none(struct GEN_algo_Clip_variables *_actor_variables ,
                                  int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = i;
}
}
void GEN_algo_Clip_limit_dot_zero(struct GEN_algo_Clip_variables *_actor_variables ,
                                  int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = 0;
}
}
void GEN_algo_Clip_read_signed(struct GEN_algo_Clip_variables *_actor_variables ,
                               int s ) 
{ 

  {
  _actor_variables->sflag = s;
  _actor_variables->count = 63;
}
}
