package org.xmpp.jnodes.jingle;

import java.util.List;

public interface RawUdpTransportProvider {
    public void setupTransport(final List<JingleContent> contents);
}
