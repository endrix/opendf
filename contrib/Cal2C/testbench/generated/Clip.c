/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct Clip_variables {
   int sflag ;
   int count ;
};
int currentSystemTime(void) ;
int openFile(char *file_name ) ;
void picture_displayImage(void) ;
void picture_setPixel(int x , int y , int r , int g , int b ) ;
int readByte(int fd ) ;
int JFrame(char *title ) ;
int Picture(int width , int height ) ;
void Clip_limit_max(struct Clip_variables *_actor_variables , int i , int *O ) ;
void Clip_limit_min(struct Clip_variables *_actor_variables , int i , int *O ) ;
void Clip_limit_none(struct Clip_variables *_actor_variables , int i , int *O ) ;
void Clip_limit_zero(struct Clip_variables *_actor_variables , int i , int *O ) ;
void Clip_read_signed(struct Clip_variables *_actor_variables , int s ) ;
void Clip_limit_max(struct Clip_variables *_actor_variables , int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = 255;
}
}
void Clip_limit_min(struct Clip_variables *_actor_variables , int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = -255;
}
}
void Clip_limit_none(struct Clip_variables *_actor_variables , int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = i;
}
}
void Clip_limit_zero(struct Clip_variables *_actor_variables , int i , int *O ) 
{ 

  {
  (_actor_variables->count) --;
  *O = 0;
}
}
void Clip_read_signed(struct Clip_variables *_actor_variables , int s ) 
{ 

  {
  _actor_variables->sflag = s;
  _actor_variables->count = 63;
}
}
