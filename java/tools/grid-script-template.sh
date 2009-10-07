#!/bin/sh

# Set the job name
#$ -N %NAME%

# Use /bin/sh as our shell
#$ -S /bin/sh

# Make sure that the .err and .out file arrive in the
# working directory
#$ -cwd

# Make the $PATH environment variable available to grid job
#$ -v PATH

# Use the current environment
#$ -V

# Set stdout and stderr filenames
#$ -o %NAME%.out
#$ -e %NAME%.err

# Set memory requirements
#$ -l mem_free=%MEM%M
#$ -l mem_token=%MEM%M
#$ -l swap_free=%MEM%M

# TODO: Add an optional processor requirement (num_proc?) 

$JAVA_HOME/bin/java -Xmx%MEM%m -server -XX:+UseParallelGC -XX:+UseParallelOldGC -cp %NAME%.jar:%CP_JARS% %ROOT-CLASS% %PARAMS% $*