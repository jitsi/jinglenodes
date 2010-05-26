ERL          ?= erl
EBIN_DIRS    := $(wildcard deps/*/ebin)
SRV	     = $(DESTDIR)/srv/jn_component
CFG	     ?= jn_component.cfg

all: erl

erl:
	@$(ERL) -pa $(EBIN_DIRS) -noinput +B \
	  -eval 'case make:all() of up_to_date -> halt(0); error -> halt(1) end.'

docs:
	@erl -noshell -run edoc_run application '$(APP)' '"."' '[]'

clean: 
	@echo "removing:"
	@rm -fv $(DESTDIR)/srv/jn_component/ebin/*.beam $(DESTDIR)/srv/jn_component/ebin/*.app

install: erl
	@echo "Installing"
	
	if test -d $(SRV); then echo "Existing Dir"; else mkdir -p $(SRV); fi
	if test -d $(SRV)/ebin; then echo "Existing Dir"; else mkdir $(SRV)/ebin; fi
	if test -d $(SRV)/src; then echo "Existing Dir"; else mkdir $(SRV)/src; fi	
	cp *.sh $(SRV)
	cp ebin/* $(SRV)/ebin
	cp src/*.app $(SRV)/ebin
	cp src/* $(SRV)/src
	if test -e $(SRV)/$(CFG); then echo "Config File Preserved."; else cp $(CFG) $(SRV); fi	
	@echo "----------------------------------------------------------"
	@echo "Installed at $(SRV)"
	@echo "----------------------------------------------------------"
