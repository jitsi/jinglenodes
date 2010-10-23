ERL          ?= erl
EBIN_DIRS    := $(wildcard deps/*/ebin)

all: erl

erl:
	if test -d ebin; then echo "Existing Dir"; else mkdir ebin; fi
	@$(ERL) -pa $(EBIN_DIRS) -noinput +B \
	  -eval 'case make:all() of up_to_date -> halt(0); error -> halt(1) end.'
	cp ./src/*.app ./ebin/
clean: 
	@echo "removing:"
	@rm -fv ./ebin/*
