#!/bin/sh
exec erl -pa $PWD/ebin -run jn_component start $1 $2 $3 $4 $5 $6 -noshell 
