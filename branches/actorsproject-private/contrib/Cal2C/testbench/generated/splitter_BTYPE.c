/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct splitter_BTYPE_variables;
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
void splitter_BTYPE_cmd_dot_newVop(struct splitter_BTYPE_variables *_actor_variables ,
                                   int cmd , int *Y , int *U , int *V ) ;
void splitter_BTYPE_cmd_dot_split(struct splitter_BTYPE_variables *_actor_variables ,
                                  int list[6] , int Y[4] , int *U , int *V ) ;
void splitter_BTYPE_skip(struct splitter_BTYPE_variables *_actor_variables , int cmd[2] ,
                         int Y[2] , int U[2] , int V[2] ) ;
void splitter_BTYPE_cmd_dot_newVop(struct splitter_BTYPE_variables *_actor_variables ,
                                   int cmd , int *Y , int *U , int *V ) 
{ 

  {
  *Y = cmd;
  *U = cmd;
  *V = cmd;
}
}
void splitter_BTYPE_cmd_dot_split(struct splitter_BTYPE_variables *_actor_variables ,
                                  int list[6] , int Y[4] , int *U , int *V ) 
{ 

  {
  Y[0] = list[0];
  Y[1] = list[1];
  Y[2] = list[2];
  Y[3] = list[3];
  *U = list[4];
  *V = list[5];
}
}
void splitter_BTYPE_skip(struct splitter_BTYPE_variables *_actor_variables , int cmd[2] ,
                         int Y[2] , int U[2] , int V[2] ) 
{ 

  {
  Y[0] = cmd[0];
  Y[1] = cmd[1];
  U[0] = cmd[0];
  U[1] = cmd[1];
  V[0] = cmd[0];
  V[1] = cmd[1];
}
}
