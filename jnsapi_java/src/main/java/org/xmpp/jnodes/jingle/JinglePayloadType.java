package org.xmpp.jnodes.jingle;

public class JinglePayloadType extends JingleElement {

    String id;
    String name;
    String clockrate;
    @Skip
    final public static String elementName = "payload-type";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClockrate() {
        return clockrate;
    }

    public void setClockrate(String clockrate) {
        this.clockrate = clockrate;
    }

    public String getElementName() {
        return elementName;
    }

}
