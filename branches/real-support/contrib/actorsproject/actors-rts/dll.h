#ifndef _DLL_H
#define _DLL_H

#include <stdio.h>
#include <stdlib.h>

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
}LIST;

extern void append_node(LIST *a,DLLIST *lnode);
extern void insert_node(LIST *a,DLLIST *lnode, DLLIST *after);
extern void remove_node(LIST *a,DLLIST *lnode);

#endif
