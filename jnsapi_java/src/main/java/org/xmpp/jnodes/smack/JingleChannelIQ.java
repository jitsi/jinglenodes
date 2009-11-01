package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.packet.IQ;

public class JingleChannelIQ extends IQ {

    public static final String NAME = "candidate";
    public static final String NAMESPACE = "http://jabber.org/protocol/jinglenodes";

    private Protocol protocol = Protocol.udp;
    private String host;
    private int porta = -1;
    private int portb = -1;
    private String id;

    public enum Protocol {
        udp, tcp
    }

    public JingleChannelIQ() {
        this.setType(Type.GET);
        this.setPacketID(IQ.nextID());
    }

    public String getChildElementXML() {
        final StringBuilder str = new StringBuilder();

        str.append("<").append(NAME).append(" xmlns='").append(NAMESPACE).append("' protocol='").append(protocol).append("' ");
        if (porta > 0 && portb > 0 && host != null) {
            str.append("host='").append(host).append("' ");
            str.append("porta='").append(porta).append("' ");
            str.append("portb='").append(portb).append("' ");
        }
        str.append("/>");

        return str.toString();
    }

    public boolean isRequest() {
        return Type.GET.equals(this.getType());
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public int getPortb() {
        return portb;
    }

    public void setPortb(int portb) {
        this.portb = portb;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPorta() {
        return porta;
    }

    public void setPorta(int porta) {
        this.porta = porta;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static IQ createEmptyResult(IQ iq) {
        return createIQ(iq.getPacketID(), iq.getFrom(), iq.getTo(), IQ.Type.RESULT);
    }

    public static IQ createEmptyError(IQ iq) {
        return createIQ(iq.getPacketID(), iq.getFrom(), iq.getTo(), IQ.Type.ERROR);
    }

    public static IQ createEmptyError() {
        return createIQ(null, null, null, IQ.Type.ERROR);
    }

    public static IQ createIQ(String ID, String to, String from, IQ.Type type) {
        IQ iqPacket = new IQ() {
            public String getChildElementXML() {
                return null;
            }
        };

        iqPacket.setPacketID(ID);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);

        return iqPacket;
    }
}
