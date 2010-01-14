package org.xmpp.jnodes.jingle;

import java.util.ArrayList;
import java.util.List;

public class JingleDescription extends JingleElement {

    @Skip
    public static final String elementName = "description";
    public static final String xmlns = "urn:xmpp:tmp:jingle:apps:rtp";
    String media;
    final List<JinglePayloadType> payloads = new ArrayList<JinglePayloadType>();

    public void addPayloadtype(final JinglePayloadType payloadType) {
        payloads.add(payloadType);
    }

    public List<JinglePayloadType> getPayloads() {
        return payloads;
    }

    public String getMedia() {
        return media;
    }

    public String getElementName() {
        return elementName;
    }

    public void setMedia(String media) {
        this.media = media;
    }
}
