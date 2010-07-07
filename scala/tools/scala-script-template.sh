#!/bin/bash

INSTALL_DIR=`dirname $0`
JAVA_OPTS="-Xmx%MEM%m -server -XX:+UseParallelGC -XX:+UseParallelOldGC"

# Lots of special handling for cygwin, to expand unix-like file references 
#   to their full paths and pass those into the java VM appropriately
if [ $OSTYPE = "cygwin" ]; then

  # Using an array here is kludgy, but allows us to manipulate the commandline 
  #   parameters individually and then pass them on to java. Using $@ doesn't 
  #   allow changing the parameters passed in.
  # Note that it does limit us to a fixed number of parameters

  INSTALL_DIR=`cygpath -w $INSTALL_DIR`

  for (( i = 1 ; i <= $# ; i++ ))
    do
    eval ARGS[$i]=\${$i}
    if [ `expr match "${ARGS[$i]}" '-.*'` == 0 ]; then
        CYGARG=`cygpath -w ${ARGS[$i]}`
        ARGS[$i]=$CYGARG
    fi
  done

  scala -cp $INSTALL_DIR/%JAR% %ROOTCLASS% \
      ${ARGS[1]} ${ARGS[2]} ${ARGS[3]} ${ARGS[4]} ${ARGS[5]} ${ARGS[6]} ${ARGS[7]} \
      ${ARGS[8]} ${ARGS[9]} ${ARGS[10]} ${ARGS[11]} ${ARGS[12]} ${ARGS[13]} ${ARGS[14]} \
      ${ARGS[15]} ${ARGS[16]} ${ARGS[17]} ${ARGS[18]} ${ARGS[19]} ${ARGS[20]} ${ARGS[21]} \
      ${ARGS[22]} ${ARGS[23]} ${ARGS[24]} ${ARGS[25]} ${ARGS[26]} ${ARGS[27]} ${ARGS[28]} \
      ${ARGS[29]} ${ARGS[30]} ${ARGS[31]} ${ARGS[32]} ${ARGS[33]} ${ARGS[34]} ${ARGS[35]} \
      ${ARGS[36]} ${ARGS[37]} ${ARGS[38]} ${ARGS[39]} ${ARGS[40]} ${ARGS[41]} ${ARGS[42]} \
      ${ARGS[43]} ${ARGS[44]} ${ARGS[45]} ${ARGS[46]} ${ARGS[47]} ${ARGS[48]} ${ARGS[49]}
else
  # Everything's simple if we're not in Cygwin  
  scala -cp $INSTALL_DIR/%JAR% %ROOTCLASS% $@
fi


