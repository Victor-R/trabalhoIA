#! /bin/bash

. functions.sh

processArgs $*

# Delete old logs
rm -f $LOGDIR/*.log

#startGIS
startKernel
startSims --nogui

echo "Start your agents"
waitFor $LOGDIR/kernel-out.log "Kernel has shut down" 30

kill $PIDS
