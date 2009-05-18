#
# Author: Rob Esser
# Date:   March-2009
#
# make file to build PseudoInterpreter
#


#input directories

SRCDIR=src

# output directories
CLASSDIR=bin/


ECHO=		/bin/echo
FIND=		/bin/find

JAVA=java 
JAVAC=javac -g -d ../${CLASSDIR}

JAR=		 jar
JARFILE=	 debugger.jar
JAROPTS=	 -cfm


JAVAFILES=		$(shell cd ${SRCDIR}; ${FIND} . -name "*.java")
SRCFILES=		$(shell  ${FIND} ${SRCDIR} -name "*.java")

.PHONY:	all
all:	${JARFILE} ${SRCFILES}

${CLASSDIR}PseudoInterpreter.class: ${SRCFILES}
	mkdir ${CLASSDIR}; cd ${SRCDIR}; $(JAVAC) ${JAVAFILES} 

${JARFILE}:	${CLASSDIR}PseudoInterpreter.class
	rm -rf ${CLASSDIR}META-INF
	${ECHO} "Main-class: PseudoInterpreter" > ${CLASSDIR}jar-manifest
	cd ${CLASSDIR}; ${JAR} ${JAROPTS} ../${JARFILE} jar-manifest *
#	cp ${JARFILE} ../../trunk/contrib/eclipse/OpendfPlugin


.PHONY:	clean
clean:
	rm -rf ${CLASSDIR}
	rm -rf ${JARFILE}


