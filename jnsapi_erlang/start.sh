#!/bin/sh
echo "Usage: ./start.sh jn.localhost secret localhost 8888 192.168.20.189 60000 localhost,localhost 2 60" 
exec erl -pa $PWD/ebin -run jn_component start $1 $2 $3 $4 $5 $6 $7 $8 $9 -detached -noinput -noshell
