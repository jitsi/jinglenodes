package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.IQ;
import org.xmlpull.v1.XmlPullParser;

public class JingleNodesProvider implements IQProvider {

    public static final String NAME = "candidate";
    public static final String NAMESPACE = "http://jabber.org/protocol/jinglenodes#channel";
    public static final String UDP = "udp";

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

    private void createUdpChannel(){

    }

}
