/* Generated by CIL v. 1.3.6 */
/* print_CIL_Input is false */

struct DispYUV_variables {
   int e ;
   int x ;
   int y ;
   int compare ;
   int fcount ;
   int ysize ;
   int lastTime ;
   int frame ;
   int picture ;
   int uvwidth ;
   int fd ;
   int uvsize ;
   int nextc ;
   int yframe[25344] ;
   int uframe[6336] ;
   int vframe[6336] ;
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
int DispYUV_function_Nuv(struct DispYUV_variables *_actor_variables , int Ny , int n ) ;
void DispYUV_function_readFrame(struct DispYUV_variables *_actor_variables ) ;
void DispYUV_untagged_action_0(struct DispYUV_variables *_actor_variables , int macroblock[384] ) ;
int DispYUV_function_Nuv(struct DispYUV_variables *_actor_variables , int Ny , int n ) 
{ int res ;
  int _if_5 ;
  int _if_6 ;

  {
  if (n == 0 || n == 2) {
    _if_5 = 0;
  } else {
    _if_5 = 4;
  }
  if (n == 0 || n == 1) {
    _if_6 = 0;
  } else {
    _if_6 = 32;
  }
  res = ((((Ny >> 1) & 3) + _if_5) + 8 * (Ny >> 4)) + _if_6;
  return (res);
}
}
void DispYUV_function_readFrame(struct DispYUV_variables *_actor_variables ) 
{ int i ;
  int _call_3 ;
  int _call_4 ;
  int _call_5 ;

  {
  i = 0;
  while (i < _actor_variables->ysize) {
    _actor_variables->yframe[i] = _actor_variables->nextc;
    _call_3 = readByte(_actor_variables->fd);
    _actor_variables->nextc = _call_3;
    i ++;
  }
  i = 0;
  while (i < _actor_variables->uvsize) {
    _actor_variables->uframe[i] = _actor_variables->nextc;
    _call_4 = readByte(_actor_variables->fd);
    _actor_variables->nextc = _call_4;
    i ++;
  }
  i = 0;
  while (i < _actor_variables->uvsize) {
    _actor_variables->vframe[i] = _actor_variables->nextc;
    _call_5 = readByte(_actor_variables->fd);
    _actor_variables->nextc = _call_5;
    i ++;
  }
  _actor_variables->x = 0;
  _actor_variables->y = 0;
}
}
void DispYUV_untagged_action_0(struct DispYUV_variables *_actor_variables , int macroblock[384] ) 
{ int y0[64] ;
  int y1[64] ;
  int y2[64] ;
  int y3[64] ;
  int u[64] ;
  int v[64] ;
  int n ;
  int xx ;
  int yy ;
  int py0 ;
  int py1 ;
  int py2 ;
  int py3 ;
  int pu ;
  int pv ;
  int ref ;
  int uvx ;
  int uvy ;
  int r ;
  int g ;
  int b ;
  int t ;
  int tu ;
  int tv ;
  int nuv ;
  int thisTime ;
  int i ;
  int _call_30 ;
  int _call_31 ;
  int _call_32 ;
  int _call_33 ;
  int _call_34 ;

  {
  i = 0;
  while (i < 64) {
    y0[i] = macroblock[i];
    i ++;
  }
  i = 0;
  while (i < 64) {
    y1[i] = macroblock[64 + i];
    i ++;
  }
  i = 0;
  while (i < 64) {
    y2[i] = macroblock[128 + i];
    i ++;
  }
  i = 0;
  while (i < 64) {
    y3[i] = macroblock[192 + i];
    i ++;
  }
  i = 0;
  while (i < 64) {
    u[i] = macroblock[256 + i];
    i ++;
  }
  i = 0;
  while (i < 64) {
    v[i] = macroblock[320 + i];
    i ++;
  }
  n = 0;
  while (n < 64) {
    xx = n & 7;
    yy = n >> 3;
    uvx = (_actor_variables->x >> 1) + xx;
    uvy = (_actor_variables->y >> 1) + yy;
    xx += _actor_variables->x;
    yy += _actor_variables->y;
    py0 = y0[n];
    py1 = y1[n];
    py2 = y2[n];
    py3 = y3[n];
    pu = u[n];
    pv = v[n];
    t = 76306 * (py0 - 16) + 32768;
    _call_30 = DispYUV_function_Nuv(_actor_variables, n, 0);
    nuv = _call_30;
    tu = u[nuv];
    tv = v[nuv];
    r = (t + 104597 * (tv - 128)) >> 16;
    g = (t - (25675 * (tu - 128) + 53279 * (tv - 128))) >> 16;
    b = (t + 132201 * (tu - 128)) >> 16;
    picture_setPixel(yy, xx, r, g, b);
    t = 76306 * (py1 - 16) + 32768;
    _call_31 = DispYUV_function_Nuv(_actor_variables, n, 1);
    nuv = _call_31;
    tu = u[nuv];
    tv = v[nuv];
    r = (t + 104597 * (tv - 128)) >> 16;
    g = (t - (25675 * (tu - 128) + 53279 * (tv - 128))) >> 16;
    b = (t + 132201 * (tu - 128)) >> 16;
    picture_setPixel(yy, xx + 8, r, g, b);
    t = 76306 * (py2 - 16) + 32768;
    _call_32 = DispYUV_function_Nuv(_actor_variables, n, 2);
    nuv = _call_32;
    tu = u[nuv];
    tv = v[nuv];
    r = (t + 104597 * (tv - 128)) >> 16;
    g = (t - (25675 * (tu - 128) + 53279 * (tv - 128))) >> 16;
    b = (t + 132201 * (tu - 128)) >> 16;
    picture_setPixel(yy + 8, xx, r, g, b);
    t = 76306 * (py3 - 16) + 32768;
    _call_33 = DispYUV_function_Nuv(_actor_variables, n, 3);
    nuv = _call_33;
    tu = u[nuv];
    tv = v[nuv];
    r = (t + 104597 * (tv - 128)) >> 16;
    g = (t - (25675 * (tu - 128) + 53279 * (tv - 128))) >> 16;
    b = (t + 132201 * (tu - 128)) >> 16;
    picture_setPixel(yy + 8, xx + 8, r, g, b);
    if (_actor_variables->compare == 1) {
      ref = _actor_variables->yframe[yy * 176 + xx];
      if (py0 != ref) {
        (_actor_variables->e) ++;
      }
      ref = _actor_variables->yframe[(yy * 176 + xx) + 8];
      if (py1 != ref) {
        (_actor_variables->e) ++;
      }
      ref = _actor_variables->yframe[(yy + 8) * 176 + xx];
      if (py2 != ref) {
        (_actor_variables->e) ++;
      }
      ref = _actor_variables->yframe[((yy + 8) * 176 + xx) + 8];
      if (py3 != ref) {
        (_actor_variables->e) ++;
      }
      ref = _actor_variables->uframe[uvy * _actor_variables->uvwidth + uvx];
      if (pu != ref) {
        (_actor_variables->e) ++;
      }
      ref = _actor_variables->vframe[uvy * _actor_variables->uvwidth + uvx];
      if (pv != ref) {
        (_actor_variables->e) ++;
      }
    }
    n ++;
  }
  picture_displayImage();
  _actor_variables->x += 16;
  if (_actor_variables->x >= 176) {
    _actor_variables->x = 0;
    _actor_variables->y += 16;
    if (_actor_variables->y >= 144) {
      if (_actor_variables->compare == 1) {
        _call_34 = currentSystemTime();
        thisTime = _call_34;
        _actor_variables->lastTime = thisTime;
      }
      _actor_variables->e = 0;
      (_actor_variables->fcount) ++;
      _actor_variables->y = 0;
      DispYUV_function_readFrame(_actor_variables);
    }
  }
}
}
