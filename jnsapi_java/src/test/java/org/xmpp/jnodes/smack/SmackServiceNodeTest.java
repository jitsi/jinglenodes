package org.xmpp.jnodes.smack;

import junit.framework.TestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Ignore;
import org.xmpp.jnodes.RelayChannelTest;

import java.io.IOException;

public class SmackServiceNodeTest extends TestCase {

    @Ignore("Meant to be ran manually")
    public void testConnect() throws InterruptedException, XMPPException, IOException {

        XMPPConnection.DEBUG_ENABLED = true;

        final String server = "localhost";
        final int port = 5222;
        final String user1 = "user1";
        final String pass1 = "user1";
        final String user2 = "user2";
        final String pass2 = "user2";
        final int timeout = 1000;

        final SmackServiceNode ssn1 = new SmackServiceNode(server, port, timeout);

        final SmackServiceNode ssn2 = new SmackServiceNode(server, port, timeout);

        ssn2.connect(user2, pass2);
        ssn1.connect(user1, pass1);

        Thread.sleep(250);

        for (int j = 0; j < 2; j++) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssn1.getConnection(), ssn2.getConnection().getUser());

            assertTrue(iq != null);
            assertTrue(ssn2.getChannels().size() > 0);

            for (int i = 0; i < 1; i++) {
                assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getPorta(), iq.getPortb()));
            }
        }

        assertTrue(ssn2.getChannels().size() > 0);

        Thread.sleep(timeout * 2);

        assertEquals(ssn2.getChannels().size(), 0);

        for (int j = 0; j < 2; j++) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssn2.getConnection(), ssn1.getConnection().getUser());

            assertTrue(iq != null);
            assertTrue(ssn1.getChannels().size() > 0);

            for (int i = 0; i < 1; i++) {
                assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getPorta(), iq.getPortb()));
            }
        }

        assertTrue(ssn1.getChannels().size() > 0);

        Thread.sleep(timeout * 2);

        assertEquals(ssn1.getChannels().size(), 0);

        // Tracker System Test

        JingleTrackerIQ iq = SmackServiceNode.getServices(ssn2.getConnection(), ssn1.getConnection().getUser());

        System.out.println(iq.toXML());

        Thread.sleep(1000000000);


        ssn1.getConnection().disconnect();
        ssn2.getConnection().disconnect();

    }

    public void testTrackerEntry() {
        TrackerEntry entry = new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, "node", JingleChannelIQ.Protocol.udp);

        assertEquals(entry.getPolicy().toString(), "public");
        assertEquals(entry.getPolicy(), TrackerEntry.Policy.valueOf("_public"));

        JingleTrackerIQ iq = new JingleTrackerIQ();

        for (int i = 0; i < 10; i++) {
            iq.addEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, "u" + String.valueOf(i), JingleChannelIQ.Protocol.udp));
        }

        System.out.println(iq.getChildElementXML());

    }

}