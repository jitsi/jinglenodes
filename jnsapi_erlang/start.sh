#!/bin/sh
##echo "Usage: ./start.sh jn.localhost secret localhost 8888 192.168.20.189 60000 localhost 2 60 10000 60000"
echo $PWD
CMD="erl -sname JNLocal -pa $PWD/ebin -s app_jn_component permanent 1" ##-detached -noinput -noshell"
export HEART_COMMAND=$CMD
exec $CMD
