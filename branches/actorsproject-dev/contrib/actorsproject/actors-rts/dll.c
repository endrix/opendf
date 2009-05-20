#include <stdio.h>
#include <stdlib.h>
#include "dll.h"
#include "circbuf.h"


#define LOCK(_ic)			EnterCriticalSection(&(_ic->lock))
#define UNLOCK(_ic)			LeaveCriticalSection(&(_ic->lock))

void append_node(LIST *a,DLLIST *lnode) {
	if(a->head == NULL) {
		a->head = lnode;
		lnode->prev = NULL;
	} else {
		a->tail->next = lnode;
		lnode->prev = a->tail;
	}

	a->tail = lnode;
	lnode->next = NULL;
	a->numNodes++;	
}

void insert_node(LIST *a,DLLIST *lnode, DLLIST *after) {
	lnode->next = after->next;
	lnode->prev = after;

	if(after->next != NULL)
		after->next->prev = lnode;
	else
		a->tail = lnode;

		after->next = lnode;
		a->numNodes++;
}

void remove_node(LIST *a,DLLIST *lnode) {
	if(lnode->prev == NULL)
		a->head = lnode->next;
	else
		lnode->prev->next = lnode->next;

	if(lnode->next == NULL)
		a->tail = lnode->prev;
	else
		lnode->next->prev = lnode->prev;

	a->numNodes--;
}

DLLIST *pop_node(LIST *a)
{
	DLLIST *lnode;
	LOCK(a);
	lnode = a->head;
	if(lnode){
		a->head = lnode->next;
		a->numNodes--;
	}
	UNLOCK(a);

	return lnode;
}

int find_node2(LIST *a, DLLIST *lnode)
{
	DLLIST *n;

	for (n=a->head; n; n=n->next)
	{
		if(lnode == n)
			break;
	}
	if(n)
		return 1;
	else
		return 0;
}

int find_node(LIST *a, DLLIST *lnode)
{
	int ret;

	LOCK(a);
	ret = find_node2(a,lnode);
	UNLOCK(a);

	return ret;
}

void push_node(LIST *a,DLLIST *n)
{
	LOCK(a);
	if(find_node2(a,n)){
		UNLOCK(a);
		return;
	}
	append_node(a,n);
	UNLOCK(a);
}

int get_node(LIST *a, DLLIST *lnode)
{
	LOCK(a);
	if(!find_node2(a,lnode)){
		UNLOCK(a);
		return 0;
	}	
	remove_node(a,lnode);
	UNLOCK(a);

	return 1;
}