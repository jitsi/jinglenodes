package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.packet.IQ;

public class Candidate extends IQ {

    private String id;
    private String ip;
    private int port;
    private String protocol;
    private Type type;
    private int maxKbps = 120;

    public enum Type {
        relay
    }

    @Override
    public String getChildElementXML() {
        final StringBuilder str = new StringBuilder();

        str.append("<candidate \n" +
                " id=''\n" +
                " ip=''\n" +
                " port=''\n" +
                " protocol=''\n" +
                " maxkbps=''\n" +
                " type=''/>");

        return str.toString();
    }
}
