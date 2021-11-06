package org.xmpp.jnodes.smack;

import java.io.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.xml.*;
import org.jivesoftware.smack.xml.XmlPullParser.*;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class JingleTrackerProvider extends IqProvider<JingleTrackerIQ>
{
    @Override
    public JingleTrackerIQ parse(final XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
        throws XmlPullParserException, IOException
    {
        JingleTrackerIQ iq = new JingleTrackerIQ();

        boolean done = false;
        Event eventType;
        String elementName;

        while (!done) {

            eventType = parser.getEventType();
            elementName = parser.getName();

            if (eventType == Event.START_ELEMENT) {
                final TrackerEntry.Type type;
                if (elementName.equals(TrackerEntry.Type.relay.toString())) {
                    type = TrackerEntry.Type.relay;
                } else if (elementName.equals(TrackerEntry.Type.tracker.toString())) {
                    type = TrackerEntry.Type.tracker;
                } else {
                    parser.next();
                    continue;
                }

                final String protocol = parser.getAttributeValue(null, "protocol");
                final TrackerEntry.Policy policy = TrackerEntry.Policy.valueOf("_" + parser.getAttributeValue(null, "policy"));
                final Jid address = JidCreate.from(parser.getAttributeValue(null, "address"));
                final String verified = parser.getAttributeValue(null, "verified");

                if (address != null && address.length() > 0) {
                    final TrackerEntry entry = new TrackerEntry(type, policy, address, protocol);
                    if (verified != null && verified.equals("true")) {
                        entry.setVerified(true);
                    }
                    iq.addEntry(entry);
                }

            } else if (eventType == Event.END_ELEMENT) {
                if (elementName.equals(JingleTrackerIQ.NAME)) {
                    done = true;
                }
            }
            if (!done) {
                parser.next();
            }
        }

        return iq;
    }
}
