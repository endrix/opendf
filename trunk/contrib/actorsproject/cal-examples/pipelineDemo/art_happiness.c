/*
 * Actor happiness (ActorClass_art_happiness)
 */

#include <stdio.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <fcntl.h>

#include "actors-rts.h"

#define OUT0_Out ART_OUTPUT(0)
#define OUT1_Out ART_OUTPUT(1)
typedef struct {
  AbstractActorInstance base;
} ActorInstance_happiness_0;


ART_ACTION_CONTEXT(0,2)

ART_ACTION(happiness_0_a0_actionAtLine_5,ActorInstance_happiness_0);
ART_ACTION_SCHEDULER(happiness_0_action_scheduler);
static void happiness_0_constructor(AbstractActorInstance*);


static const PortDescription outputPortDescriptions[]={
  {"Out", sizeof(int32_t)},
  {"Quit", sizeof(int32_t)},
};

static const int portRate_1[] = {
  1
};

static const ActionDescription actionDescriptions[] = {
  {"actionAtLine_5", 0, portRate_1}
};

ActorClass ActorClass_art_happiness = INIT_ActorClass(
  "happiness",
  ActorInstance_happiness_0,
  happiness_0_constructor,
  0, /* no setParam */
  happiness_0_action_scheduler,
  0, /* no destructor */
  0, 0,
  2, outputPortDescriptions,
  1, actionDescriptions
);

int kbhit(void)
{
	struct termios oldt, newt;
	int ch;
	int oldf;

	tcgetattr(STDIN_FILENO, &oldt);
	newt = oldt;
	newt.c_lflag &= ~(ICANON | ECHO);
	tcsetattr(STDIN_FILENO, TCSANOW, &newt);
	oldf = fcntl(STDIN_FILENO, F_GETFL, 0);
	fcntl(STDIN_FILENO, F_SETFL, oldf | O_NONBLOCK);

	ch = getchar();
    tcsetattr(STDIN_FILENO, TCSANOW, &oldt);
	fcntl(STDIN_FILENO, F_SETFL, oldf);

	if(ch != EOF)
	{
		ungetc(ch, stdin);
		return 1;
	}
	return 0;
}

int32_t get_happiness_value()
{
	char        ch;
	int         retval=-1;
	if(kbhit()){
		ch=getchar();
		if(ch=='y')
			retval=1;		
		else if(ch=='n')
			retval=0;
		else if (ch=='q')
		  retval=2;
		else{
			printf("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");
			printf("Are you happy? ");
		}
	}
	if(retval>=0)
		printf("\n");

	return retval;
}
static int terminate;
ART_ACTION(happiness_0_a0_actionAtLine_5,ActorInstance_happiness_0) {
  ART_ACTION_ENTER(happiness_0_a0_actionAtLine_5,0);
  int32_t happiness;
  happiness=get_happiness_value();
  if(happiness==2)
    terminate=1;
  else if(happiness>=0){
  	pinWrite_int32_t(OUT0_Out, happiness);
  }
  ART_ACTION_EXIT(happiness_0_a0_actionAtLine_5,0);
}

static const int exitcode_block_Out_1[] = {
  EXITCODE_BLOCK(1), 0, 1
};

ART_ACTION_SCHEDULER(happiness_0_action_scheduler) {
  ActorInstance_happiness_0 *thisActor=(ActorInstance_happiness_0*) pBase;
  const int *exitCode=EXIT_CODE_YIELD;
  ART_ACTION_SCHEDULER_ENTER(0,1);
  ART_ACTION_SCHEDULER_LOOP {
    ART_ACTION_SCHEDULER_LOOP_TOP;
    if ((1)) {
      int32_t t6;
      t6=pinAvailOut_int32_t(OUT0_Out);
      if ((t6>=(1))) {
        ART_FIRE_ACTION(happiness_0_a0_actionAtLine_5);
      }
  if(terminate){
    pinWrite_int32_t(OUT1_Out, 1);
    exitCode=EXITCODE_TERMINATE; goto action_scheduler_exit;
  }
      //else {
        exitCode=exitcode_block_Out_1; goto action_scheduler_exit;
      //}
    }
    else {
      exitCode=EXITCODE_TERMINATE; goto action_scheduler_exit;
    }
    ART_ACTION_SCHEDULER_LOOP_BOTTOM;
  }
  action_scheduler_exit:
  ART_ACTION_SCHEDULER_EXIT(0,2);
  return exitCode;
}

static void happiness_0_constructor(AbstractActorInstance *pBase) {
}
