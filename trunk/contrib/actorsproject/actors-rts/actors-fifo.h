#define FIFO_NAME_3(f,t) f##_##t
#define FIFO_NAME_2(f,t) FIFO_NAME_3(f, t)
#define FIFO_NAME(f) FIFO_NAME_2(f, FIFO_TYPE)

static inline unsigned FIFO_NAME(pinAvailIn)(const LocalInputPort *p) 
{
  return p->available;
}

static inline unsigned FIFO_NAME(pinAvailOut)(const LocalOutputPort *p) 
{
  return p->available;
}

static inline void FIFO_NAME(pinWrite)(LocalOutputPort *p, FIFO_TYPE token) 
{
  assert(FIFO_NAME(pinAvailOut)(p) > 0);
  ((FIFO_TYPE*)p->buffer)[p->pos] = token;
  p->pos++;
  if (p->pos >= 0) { p->pos = -(p->capacity); }
  p->available--;
}

static inline void FIFO_NAME(pinWriteRepeat)(LocalOutputPort *p,
					     FIFO_TYPE *buf,
					     int n) 
{

  assert(FIFO_NAME(pinAvailOut)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }

/*
  int i;

  assert(FIFO_NAME(pinAvailOut)(p) >= n);
  for (i = 0 ; i < n ; i++) {
    ((FIFO_TYPE*)p->buffer)[p->pos] = buf[i];
    p->pos++;
    if (p->pos >= 0) { p->pos = -(p->capacity); }
    p->available--;
  }
*/
}

static inline FIFO_TYPE FIFO_NAME(pinRead)(LocalInputPort *p) 
{
  FIFO_TYPE result;

  assert(FIFO_NAME(pinAvailIn)(p) > 0);
  result = ((FIFO_TYPE*)p->buffer)[p->pos];
  p->pos++;
  if (p->pos >= 0) { p->pos = -(p->capacity); }
  p->available--;
  return result;
}

static inline void FIFO_NAME(pinReadRepeat)(LocalInputPort *p,
					    FIFO_TYPE *buf,
					    int n) 
{

  assert(FIFO_NAME(pinAvailIn)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }
}

static inline FIFO_TYPE FIFO_NAME(pinPeekFront)(const LocalInputPort *p)
{
  assert(FIFO_NAME(pinAvailIn)(p) > 0);
  return ((FIFO_TYPE*)p->buffer)[p->pos];
}

static inline FIFO_TYPE FIFO_NAME(pinPeek)(const LocalInputPort *p, 
                                           int offset) {
  assert(offset>=0 && FIFO_NAME(pinAvailIn)(p) >= offset);

  /* p->pos ranges from -capacity to -1, so should offset */
  offset+=p->pos;
  if (offset>=0) {
    offset-=p->capacity; /* wrap-around */
  }
  return ((FIFO_TYPE*)p->buffer)[offset];
}

#ifdef READ_BYTES
static inline unsigned pinAvailIn_bytes(const LocalInputPort *p, int bytes) 
{
  return p->available/bytes;
}

static inline unsigned pinAvailOut_bytes(const LocalOutputPort *p, int bytes) 
{
  return p->available/bytes;
}
static inline void pinRead_bytes(LocalInputPort *p, void *buf, int bytes)
{
  int n = bytes;  
  assert(FIFO_NAME(pinAvailIn)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }
}

static inline void pinReadRepeat_bytes(LocalInputPort *p, void *buf, int tokens, int bytes)
{
  int n = bytes*tokens;  
  assert(FIFO_NAME(pinAvailIn)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(buf, &((FIFO_TYPE*)p->buffer)[p->pos], 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }
}

static inline void pinWrite_bytes(LocalInputPort *p, void *buf, int bytes)
{
  int n = bytes;  
  assert(FIFO_NAME(pinAvailOut)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }
}

static inline void pinWriteRepeat_bytes(LocalInputPort *p, void *buf, int tokens, int bytes)
{
  int n = bytes*tokens;  
  assert(FIFO_NAME(pinAvailOut)(p) >= n);
  p->available -= n;
  if (p->pos + n >= 0) {
    // Buffer wrap
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   -(p->pos * sizeof(FIFO_TYPE)));
    buf += -(p->pos);
    n -= -(p->pos);
    p->pos = -(p->capacity);
  }
  if (n) {
    memcpy(&((FIFO_TYPE*)p->buffer)[p->pos], buf, 
	   n * sizeof(FIFO_TYPE));
    p->pos += n;
  }
}

static inline FIFO_TYPE pinPeekFront_bytes(const LocalInputPort *p)
{
  assert(FIFO_NAME(pinAvailIn)(p) > 0);
  return ((FIFO_TYPE*)p->buffer)[p->pos];
}

static inline FIFO_TYPE pinPeek_bytes(const LocalInputPort *p, int bytes, 
                                           int offset) {
  int offset2 = bytes*offset;  
  assert(offset2>=0 && FIFO_NAME(pinAvailIn)(p) >= offset2);

  /* p->pos ranges from -capacity to -1, so should offset */
  offset2+=p->pos;
  if (offset2>=0) {
    offset2-=p->capacity; /* wrap-around */
  }
  return ((FIFO_TYPE*)p->buffer)[offset2];
}

static inline FIFO_TYPE pinPeekRepeat_bytes(const LocalInputPort *p, int tokens, int bytes, 
                                           int offset) {
  int offset2 = bytes*(tokens + offset);  
  assert(offset2>=0 && FIFO_NAME(pinAvailIn)(p) >= offset2);

  /* p->pos ranges from -capacity to -1, so should offset */
  offset2+=p->pos;
  if (offset2>=0) {
    offset2-=p->capacity; /* wrap-around */
  }
  return ((FIFO_TYPE*)p->buffer)[offset2];
}
#endif
