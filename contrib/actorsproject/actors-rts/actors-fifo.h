#define FIFO_NAME_3(f,t) f##_##t
#define FIFO_NAME_2(f,t) FIFO_NAME_3(f, t)
#define FIFO_NAME(f) FIFO_NAME_2(f, FIFO_TYPE)

static inline unsigned FIFO_NAME(pinAvailIn)(const LocalInputPort *p) {
  return p->available;
}

static inline unsigned FIFO_NAME(pinAvailOut)(const LocalOutputPort *p) {
  return p->available;
}

static inline void FIFO_NAME(pinWrite)(LocalOutputPort *p, FIFO_TYPE token) {
#ifdef DEBUG
  assert(FIFO_NAME(pinAvailOut)(p) > 0);
#endif
  ((FIFO_TYPE*)p->buffer)[p->pos] = token;
//  printf("pinWrite[%p] = %d (%d)\n", &((FIFO_TYPE*)p->buffer)[p->pos], (int)token, p->pos);
  p->pos++;
  if (p->pos >= 0) { p->pos = -(p->capacity); }
  p->available--;
}

static inline FIFO_TYPE FIFO_NAME(pinRead)(LocalInputPort *p) {
  FIFO_TYPE result;
#ifdef DEBUG
  assert(FIFO_NAME(pinAvailIn)(p) > 0);
#endif
  result = ((FIFO_TYPE*)p->buffer)[p->pos];
//  printf("pinRead[%p] = %d (%d)\n", &((FIFO_TYPE*)p->buffer)[p->pos], (int)result, p->pos);
  p->pos++;
  if (p->pos >= 0) { p->pos = -(p->capacity); }
  p->available--;
  return result;
}

static inline FIFO_TYPE FIFO_NAME(pinPeekFront)(LocalInputPort *p) {
#ifdef DEBUG
  assert(FIFO_NAME(pinAvailIn)(p) > 0);
#endif
  return ((FIFO_TYPE*)p->buffer)[p->pos];
}
