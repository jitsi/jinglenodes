package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.packet.IQ;

public class JingleChannelRequest extends IQ {    
    public String getChildElementXML() {
        return "";
    }

    public static IQ createEmptyResult(IQ iq) {
        return createIQ(iq.getPacketID(), iq.getFrom(), iq.getTo(), IQ.Type.RESULT);
    }

    public static IQ createEmptyError(IQ iq) {
        return createIQ(iq.getPacketID(), iq.getFrom(), iq.getTo(), IQ.Type.ERROR);
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
