#!/bin/sh
echo $PWD
CMD="erl -sname JNLocal -pa $PWD/ebin -s app_jn_component -detached -noinput -noshell"
export HEART_COMMAND=$CMD
exec $CMD
