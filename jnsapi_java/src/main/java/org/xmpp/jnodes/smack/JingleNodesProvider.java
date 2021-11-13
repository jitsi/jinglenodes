package org.xmpp.jnodes.smack;

import java.io.*;
import java.util.IllegalFormatException;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.xml.*;
import org.jivesoftware.smack.xml.XmlPullParser.*;

public class JingleNodesProvider extends IqProvider<JingleChannelIQ>
{
    @Override
    public JingleChannelIQ parse(final XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException
    {

        JingleChannelIQ iq = null;

        boolean done = false;
        Event eventType;
        String elementName;
        String namespace;

        while (!done) {

            eventType = parser.getEventType();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == Event.START_ELEMENT) {
                if (elementName.equals(JingleChannelIQ.ELEMENT) && namespace.equals(JingleChannelIQ.NAMESPACE)) {

                    final String protocol = parser.getAttributeValue(null, "protocol");
                    final String porta = parser.getAttributeValue(null, "localport");
                    final String portb = parser.getAttributeValue(null, "remoteport");
                    final String host = parser.getAttributeValue(null, "host");

                    try {
                        iq = new JingleChannelIQ();
                        iq.setProtocol(protocol == null ? JingleChannelIQ.UDP : protocol);
                        if (host != null)
                            iq.setHost(host);
                        if (porta != null)
                            iq.setLocalport(Integer.parseInt(porta));
                        if (portb != null)
                            iq.setRemoteport(Integer.parseInt(portb));
                    } catch (final IllegalFormatException | NumberFormatException e) {
                        e.printStackTrace();
                    }

                }
            } else if (eventType == Event.END_ELEMENT) {
                done = true;
            }
            if (!done)
                parser.next();
        }

        return iq;
    }
}
