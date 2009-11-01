package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.xmlpull.v1.XmlPullParser;
import org.xmpp.jnodes.RelayChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.IllegalFormatException;
import java.io.IOException;

public class JingleNodesProvider implements IQProvider {

    public JingleChannelIQ parseIQ(final XmlPullParser parser) throws Exception {

        JingleChannelIQ iq = null;

        boolean done = false;
        int eventType;
        String elementName;
        String namespace;

        while (!done) {

            eventType = parser.getEventType();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG) {
                if (elementName.equals(JingleChannelIQ.NAME) && namespace.equals(JingleChannelIQ.NAMESPACE)) {

                    final String protocol = parser.getAttributeValue(null, "protocol");
                    final String porta = parser.getAttributeValue(null, "porta");
                    final String portb = parser.getAttributeValue(null, "portb");
                    final String host = parser.getAttributeValue(null, "host");

                    try {
                        iq = new JingleChannelIQ();
                        iq.setProtocol(protocol == null ? JingleChannelIQ.Protocol.udp : JingleChannelIQ.Protocol.valueOf(protocol));
                        if (host != null)
                            iq.setHost(host);
                        if (porta != null)
                            iq.setPorta(Integer.valueOf(porta));
                        if (portb != null)
                            iq.setPortb(Integer.valueOf(portb));
                    } catch (final IllegalFormatException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                }
            } else if (eventType == XmlPullParser.END_TAG) {
                done = true;
            }
            if (!done)
                parser.next();
        }

        return iq;
    }
}
