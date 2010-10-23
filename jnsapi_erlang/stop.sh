#!/bin/bash

HOSTNAME=`hostname`
erl_call -n JingleNodes@$HOSTNAME -v -a 'init stop []'

