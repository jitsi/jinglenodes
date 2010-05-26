
ERLCFLAGS += -pa .

if ENABLE_HIPE
    ERLCFLAGS+=+native
endif


SOURCES = \
	mod_jinglenodes.erl \
	udp_relay.erl
SOURCES_BEAMS = $(SOURCES:.erl=.beam)

beamdir = $(libdir)/ejabberd/ebin
beam_DATA = $(SOURCES_BEAMS)

EXTRA_DIST = $(SOURCES)
CLEANFILES = $(SOURCES_BEAMS)


%.beam: %.erl
	$(ERLC) -W $(ERLCFLAGS) $<

