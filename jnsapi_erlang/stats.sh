#!/bin/bash
HOSTNAME=`hostname`
##`hostname`
erl_call -n JNLocal@$HOSTNAME -a 'jn_component get_stats'
