package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.xmlpull.v1.XmlPullParser;
import org.xmpp.jnodes.RelayChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

public class JingleNodesProvider implements IQProvider {

    public static final String NAME = "candidate";
    public static final String NAMESPACE = "http://jabber.org/protocol/jinglenodes#channel";
    public static final String UDP = "udp";
    private final ConcurrentHashMap<String, RelayChannel> channels = new ConcurrentHashMap<String, RelayChannel>();

    private final AtomicInteger ids = new AtomicInteger(0);

    public IQ parseIQ(final XmlPullParser parser) throws Exception {

        IQ iq = null;

        boolean done = false;
        int eventType;
        String elementName;
        String namespace;

        while (!done) {

            eventType = parser.getEventType();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG) {
                if (elementName.equals(NAME) && namespace.equals(NAMESPACE)) {

                    final String protocol = parser.getAttributeValue("protocol", UDP).toLowerCase();

                    if (protocol.equals(UDP)) {

                    }

                }
            } else if (eventType == XmlPullParser.END_TAG) {
                done = true;
            }

        }

        return iq;
    }

    private IQ createUdpChannel(final IQ iq) {

        try {
            final RelayChannel rc = RelayChannel.createLocalRelayChannel();

            final IQ result = new IQ() {
                public String getChildElementXML() {
                    final StringBuilder str = new StringBuilder();
                    str.append("<").append(NAME).append(" xmlns='").append(NAMESPACE).append("' protocol='").append(UDP).append("' id='").append(ids.incrementAndGet()).append("' ");
                    str.append("ip='").append(rc.getIp()).append("' ");
                    str.append("porta='").append(rc.getPortA()).append("' portb='").append(rc.getPortB()).append("'/>");
                    return str.toString();
                }
            };

            result.setPacketID(iq.getPacketID());
            result.setFrom(iq.getTo());
            result.setTo(iq.getFrom());
            result.setType(IQ.Type.RESULT);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return JingleChannelRequest.createEmptyError(iq);
        }

    }

}
