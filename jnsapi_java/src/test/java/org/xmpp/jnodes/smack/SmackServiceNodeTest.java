package org.xmpp.jnodes.smack;

import junit.framework.TestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Ignore;
import org.xmpp.jnodes.RelayChannelTest;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class SmackServiceNodeTest extends TestCase {

    @Ignore("Meant to be ran manually")
    public void testConnect() throws InterruptedException, XMPPException, IOException {

        final String server = "localhost";
        final int port = 5222;
        final String user1 = "user1";
        final String pass1 = "user1";
        final String user2 = "user2";
        final String pass2 = "user2";
        final String user3 = "user3";
        final String pass3 = "user3";
        final int timeout = 1250;

        final SmackServiceNode ssn1 = new SmackServiceNode(server, port, timeout);

        final SmackServiceNode ssn2 = new SmackServiceNode(server, port, timeout);

        final SmackServiceNode ssn3 = new SmackServiceNode(server, port, timeout);

        ssn3.connect(user3, pass3, true);
        ssn2.connect(user2, pass2, true);
        ssn1.connect(user1, pass1, true);

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
        final int pub = 5;
        final int unk = 3;
        final int ros = 2;

        ssn1.getConnection().getRoster().createEntry(ssn2.getConnection().getUser(), "test", new String[]{});
        ssn2.getConnection().getRoster().createEntry(ssn3.getConnection().getUser(), "test", new String[]{});
        ssn3.getConnection().getRoster().createEntry(ssn1.getConnection().getUser(), "test", new String[]{});

        Thread.sleep(500);

        for (int i = 0; i < pub; i++) {
            ssn1.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, "p" + String.valueOf(i), JingleChannelIQ.Protocol.udp));
        }
        for (int i = 0; i < unk; i++) {
            ssn1.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._unknown, "u" + String.valueOf(i), JingleChannelIQ.Protocol.udp));
        }
        for (int i = 0; i < ros; i++) {
            ssn1.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._roster, "r" + String.valueOf(i), JingleChannelIQ.Protocol.udp));
        }

        Thread.sleep(200);
        JingleTrackerIQ iq = SmackServiceNode.getServices(ssn2.getConnection(), ssn1.getConnection().getUser());

        Thread.sleep(500);

        assertEquals(iq.getEntries().size(), pub + unk);

        SmackServiceNode.MappedNodes ma = SmackServiceNode.searchServices(ssn2.getConnection(), 10, 10);

        Thread.sleep(500);

        assertEquals(ma.getRelayEntries().size(), pub + unk);

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

    @Ignore("Meant to be ran manually")
    public void testDeepSearch() throws InterruptedException, XMPPException, IOException {

        final String server = "localhost";
        final int port = 5222;
        final int timeout = 5000;
        final String pre = "ds";
        final int users = 10;

        final List<SmackServiceNode> ssns = new ArrayList<SmackServiceNode>();

        for (int i = 0; i < users; i++) {
            final SmackServiceNode ssn = new SmackServiceNode(server, port, timeout);
            ssn.connect(pre + i, pre + i, true);
            ssns.add(ssn);
        }

        Thread.sleep(250);

        for (int i = 0; i < users - 1; i++) {
            ssns.get(i).getConnection().getRoster().createEntry(ssns.get(i + 1).getConnection().getUser(), "test", new String[]{});
            ssns.get(i + 1).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.Protocol.udp));
            ssns.get(i).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.tracker, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.Protocol.udp));
        }

        Thread.sleep(200);

        SmackServiceNode.MappedNodes ma = SmackServiceNode.searchServices(ssns.get(0).getConnection(), users, users);
        Thread.sleep(200);

        assertEquals(ma.getRelayEntries().size(), users - 1);
        assertEquals(ma.getTrackerEntries().size(), users - 2);

        for (final TrackerEntry entry : ma.getRelayEntries().values()) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssns.get(0).getConnection(), entry.getJid());

            assertTrue(iq != null);

            assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getPorta(), iq.getPortb()));
        }

        for (final SmackServiceNode sn : ssns) {
            sn.getConnection().disconnect();
        }

    }

}