/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct splitter_MV_variables;
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
void splitter_MV_B1(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) ;
void splitter_MV_B2(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) ;
void splitter_MV_B3(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) ;
void splitter_MV_B4(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) ;
void splitter_MV_B5(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int U[2] ) ;
void splitter_MV_B6(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int V[2] ) ;
void splitter_MV_newvop(struct splitter_MV_variables *_actor_variables , int cmd ) ;
void splitter_MV_skip(struct splitter_MV_variables *_actor_variables , int btype[2] ) ;
void splitter_MV_skipbtype(struct splitter_MV_variables *_actor_variables , int btype ) ;
void splitter_MV_B1(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) 
{ 

  {
  Y[0] = mv[0];
  Y[1] = mv[1];
}
}
void splitter_MV_B2(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) 
{ 

  {
  Y[0] = mv[0];
  Y[1] = mv[1];
}
}
void splitter_MV_B3(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) 
{ 

  {
  Y[0] = mv[0];
  Y[1] = mv[1];
}
}
void splitter_MV_B4(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int Y[2] ) 
{ 

  {
  Y[0] = mv[0];
  Y[1] = mv[1];
}
}
void splitter_MV_B5(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int U[2] ) 
{ 

  {
  U[0] = mv[0];
  U[1] = mv[1];
}
}
void splitter_MV_B6(struct splitter_MV_variables *_actor_variables , int mv[2] , int btype ,
                    int V[2] ) 
{ 

  {
  V[0] = mv[0];
  V[1] = mv[1];
}
}
void splitter_MV_newvop(struct splitter_MV_variables *_actor_variables , int cmd ) 
{ 

  {

}
}
void splitter_MV_skip(struct splitter_MV_variables *_actor_variables , int btype[2] ) 
{ 

  {

}
}
void splitter_MV_skipbtype(struct splitter_MV_variables *_actor_variables , int btype ) 
{ 

  {

}
}
