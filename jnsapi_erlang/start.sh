#!/bin/sh
erl -sname JingleNodes -pa $PWD/ebin -s app_jn_component -detached -noinput -noshell -env ERL_MAX_PORTS 65000
