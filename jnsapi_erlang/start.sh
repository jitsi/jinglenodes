#!/bin/sh
cd `dirname $0`
exec erl -pa $PWD/ebin -s -run jn_component start $1 $2 $3 $4 $5 
