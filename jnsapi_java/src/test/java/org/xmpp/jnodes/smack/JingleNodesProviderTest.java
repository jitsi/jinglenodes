package org.xmpp.jnodes.smack;

import junit.framework.TestCase;

public class JingleNodesProviderTest extends TestCase {

    public void testRequestCreation() {

        final JingleChannelIQ request = new JingleChannelIQ();
        request.setPacketID("abc");
        request.setFrom("linus@jn.com");
        request.setTo("bill@jn.com");

        assertEquals(request.toXML(), "<iq id=\"abc\" to=\"bill@jn.com\" from=\"linus@jn.com\" type=\"get\"><candidate xmlns='http://jabber.org/protocol/jinglenodes#channel' protocol='udp'/></iq>");

    }

}
