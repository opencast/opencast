#!/bin/bash

#
# Stop felix
#
# Kill the currently running server (there's gotta be a better way!)
MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ -z "$MATTERHORN_PID" ]; then
  echo "Matterhorn already stopped"
  exit 1
fi

kill $MATTERHORN_PID

sleep 5

MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ ! -z "$MATTERHORN_PID" ]; then
  echo "Hard killing since felix ($MATTERHORN_PID) seems unresponsive to regular kill"
  
  kill -9 $MATTERHORN_PID
fi

