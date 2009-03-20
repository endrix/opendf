#include <stdio.h>
#include <stdlib.h>
#include "dll.h"


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
