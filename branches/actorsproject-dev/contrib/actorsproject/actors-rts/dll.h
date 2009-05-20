#ifndef _DLL_H
#define _DLL_H

#include <semaphore.h>

/* make the header usable from C++ */
#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

typedef struct _dllist {
	struct _dllist *next;
	struct _dllist *prev;

	void *obj;
}DLLIST;

typedef struct _list {
	DLLIST *head;
	DLLIST *tail;
	int numNodes;
	int	lid;
	sem_t lock;

	pthread_mutex_t	mt;
	pthread_cond_t	cv; 
}LIST;

extern void append_node(LIST *a,DLLIST *lnode);
extern void insert_node(LIST *a,DLLIST *lnode, DLLIST *after);
extern void remove_node(LIST *a,DLLIST *lnode);
extern DLLIST *pop_node(LIST *a);
extern void push_node(LIST *a,DLLIST *n);
extern int get_node(LIST *a, DLLIST *lnode);

#ifdef __cplusplus
}
#endif

#endif
