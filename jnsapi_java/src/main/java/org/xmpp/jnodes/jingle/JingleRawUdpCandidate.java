package org.xmpp.jnodes.jingle;

public class JingleRawUdpCandidate extends JingleElement {

    String ip;
    String port;
    String type;
    @Skip
    final public static String elementName = "candidate";

    public JingleRawUdpCandidate() {
    }

    public JingleRawUdpCandidate(String ip, String port, String type) {
        this.ip = ip;
        this.port = port;
        this.type = type;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setType(String type) {
        this.type = type;
    }

    final public String getElementName() {
        return elementName;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getType() {
        return type;
    }
}
