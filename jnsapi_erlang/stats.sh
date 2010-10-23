#!/bin/bash
HOSTNAME=`hostname`
/usr/bin/erl_call -n JingleNodes@$HOSTNAME -a 'jn_component get_stats'
