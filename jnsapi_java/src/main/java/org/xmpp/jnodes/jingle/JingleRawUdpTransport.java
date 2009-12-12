package org.xmpp.jnodes.jingle;

import java.util.ArrayList;
import java.util.List;

public class JingleRawUdpTransport extends JingleElement {

    public static final String xmlns = "urn:xmpp:tmp:jingle:transports:raw-udp";
    List<JingleRawUdpCandidate> candidates = new ArrayList<JingleRawUdpCandidate>();
    @Skip
    final public static String elementName = "transport";

    final public String getElementName() {
        return elementName;
    }

    final public void addCandidate(final JingleRawUdpCandidate candidate) {
        candidates.add(candidate);
    }

    final public List<JingleRawUdpCandidate> getCandidates() {
        return candidates;
    }
}
