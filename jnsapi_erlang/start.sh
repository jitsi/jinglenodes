#!/bin/sh
#erl -name JingleNodes -pa $PWD/ebin -s app_jn_component -detached -noinput -noshell -env ERL_MAX_PORTS 65000
erl -name JingleNodes -pa deps/*/ebin -pa ebin/ -s app_jn_component
