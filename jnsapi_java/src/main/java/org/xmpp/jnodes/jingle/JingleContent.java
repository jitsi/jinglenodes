package org.xmpp.jnodes.jingle;

public class JingleContent extends JingleElement {

    public static class Creator {
        final public static String INITIATOR = "initiator";
        final public static String RESPONDER = "responder";
    }

    String creator;
    String name;
    JingleDescription description;
    JingleRawUdpTransport transport;
    @Skip
    public static final String elementName = "content";

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JingleDescription getDescription() {
        return description;
    }

    public void setDescription(JingleDescription description) {
        this.description = description;
    }

    public JingleRawUdpTransport getTransport() {
        return transport;
    }

    public void setTransport(JingleRawUdpTransport transport) {
        this.transport = transport;
    }

    public String getElementName() {
        return elementName;
    }

}
