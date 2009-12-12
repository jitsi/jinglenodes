package org.xmpp.jnodes.jingle;

public class JingleInfoRing extends JingleElement {
    final static public String xmlns = "urn:xmpp:jingle:apps:rtp:info";

    @Skip
    public static final String elementName = "ringing";

    public String getElementName() {
        return elementName;
    }
}
