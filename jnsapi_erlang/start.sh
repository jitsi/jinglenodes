#!/bin/sh
echo "Usage: ./start.sh jn.localhost secret localhost 8888 192.168.20.189 60000 localhost 2 60 10000 60000"
CMD="erl -sname JNLocal -pa $PWD/ebin -heart -run jn_component start $1 $2 $3 $4 $5 $6 $7 $8 $9 ${10} ${11} -detached -noinput -noshell"
export HEART_COMMAND=$CMD
exec $CMD
