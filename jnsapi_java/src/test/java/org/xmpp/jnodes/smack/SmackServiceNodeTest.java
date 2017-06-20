package org.xmpp.jnodes.smack;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.Ignore;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmpp.jnodes.RelayChannelTest;
import org.xmpp.jnodes.smack.SmackServiceNode.MappedNodes;

import junit.framework.TestCase;

public class SmackServiceNodeTest {
    private AbstractXMPPConnection getTcpConnection(String server, int port, int timeout) throws SmackException, IOException, XMPPException, InterruptedException {
        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration
            .builder()
            .setHost(server)
            .setXmppDomain(server)
            .setPort(port)
            .setConnectTimeout(timeout)
            .setCustomX509TrustManager(new X509TrustManager() {
                // these are unit tests, we really don't care
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }
            })
            .setDebuggerEnabled(true)
            .setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        AbstractXMPPConnection connection = new XMPPTCPConnection(configBuilder.build());
        return connection;
    }

    @Test
    @Ignore("Meant to be run manually")
    public void testConnect() throws Exception {

        final String server = "localhost";
        final int port = 5222;
        final Localpart user1 = Localpart.from("user1");
        final String pass1 = "user1";
        final Localpart user2 = Localpart.from("user2");
        final String pass2 = "user2";
        final Localpart user3 = Localpart.from("user3");
        final String pass3 = "user3";
        final int timeout = 1250;

        final SmackServiceNode ssn1 = new SmackServiceNode(getTcpConnection(server, port, timeout), timeout);

        final SmackServiceNode ssn2 = new SmackServiceNode(getTcpConnection(server, port, timeout), timeout);

        final SmackServiceNode ssn3 = new SmackServiceNode(getTcpConnection(server, port, timeout), timeout);

        ssn3.connect(user3, pass3, false, Roster.SubscriptionMode.accept_all);
        ssn2.connect(user2, pass2, false, Roster.SubscriptionMode.accept_all);
        ssn1.connect(user1, pass1, false, Roster.SubscriptionMode.accept_all);

        Roster.getInstanceFor(ssn1.getConnection()).createEntry(ssn2.getConnection().getUser().asBareJid(), "test", new String[]{});
        Roster.getInstanceFor(ssn2.getConnection()).createEntry(ssn3.getConnection().getUser().asBareJid(), "test", new String[]{});
        Roster.getInstanceFor(ssn3.getConnection()).createEntry(ssn1.getConnection().getUser().asBareJid(), "test", new String[]{});

        ssn3.getConnection().sendStanza(new Presence(Presence.Type.available));
        ssn2.getConnection().sendStanza(new Presence(Presence.Type.available));
        ssn1.getConnection().sendStanza(new Presence(Presence.Type.available));

        Thread.sleep(250);

        for (int j = 0; j < 1; j++) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssn1.getConnection(), ssn2.getConnection().getUser());

            assertNotNull(iq);
            assertTrue(ssn2.getChannels().size() > 0);

            for (int i = 0; i < 1; i++) {
                assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getLocalport(), iq.getRemoteport()));
            }
        }

        assertTrue(ssn2.getChannels().size() > 0);

        Thread.sleep(timeout * 2);

        assertEquals(0, ssn2.getChannels().size());

        for (int j = 0; j < 2; j++) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssn2.getConnection(), ssn1.getConnection().getUser());

            assertNotNull(iq);
            assertTrue(ssn1.getChannels().size() > 0);

            for (int i = 0; i < 1; i++) {
                assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getLocalport(), iq.getRemoteport()));
            }
        }

        assertTrue(ssn1.getChannels().size() > 0);

        Thread.sleep(timeout * 2);

        assertEquals(0, ssn1.getChannels().size());

        // Tracker System Test
        final int pub = 5;
        final int unk = 3;
        final int ros = 2;

        Thread.sleep(500);

        for (int i = 0; i < pub; i++) {
            ssn3.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, JidCreate.from("p" + String.valueOf(i)), JingleChannelIQ.UDP));
        }
        for (int i = 0; i < unk; i++) {
            ssn3.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, JidCreate.from("d" + String.valueOf(i)), JingleChannelIQ.UDP));
        }
        for (int i = 0; i < ros; i++) {
            ssn3.addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._roster, JidCreate.from("r" + String.valueOf(i)), JingleChannelIQ.UDP));
        }

        Thread.sleep(200);
        SmackServiceNode.MappedNodes ma = SmackServiceNode.searchServices(ssn2.getConnection(), 10, 10, 50, JingleChannelIQ.UDP, true);
        ssn2.addEntries(ma);

        Thread.sleep(500);

        assertEquals(pub + unk + 1, ma.getRelayEntries().size());

        SmackServiceNode.MappedNodes mb = SmackServiceNode.searchServices(ssn1.getConnection(), 10, 10, 50, JingleChannelIQ.UDP, true);

        Thread.sleep(500);

        assertEquals(pub + unk + 1, mb.getRelayEntries().size());

        System.out.println("Preferred Relay: " + ssn2.getPreferedRelay().getJid());

        ssn1.getConnection().disconnect();
        ssn2.getConnection().disconnect();
        ssn3.getConnection().disconnect();

        Thread.sleep(1500);
    }

    @Test
    public void testTrackerEntry() throws XmppStringprepException {
        TrackerEntry entry = new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, JidCreate.from("node"), JingleChannelIQ.UDP);

        assertEquals("public", entry.getPolicy().toString());
        assertEquals(TrackerEntry.Policy.valueOf("_public"), entry.getPolicy());

        JingleTrackerIQ iq = new JingleTrackerIQ();

        for (int i = 0; i < 10; i++) {
            iq.addEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, JidCreate.from("u" + String.valueOf(i)), JingleChannelIQ.UDP));
        }

        System.out.println(iq.getChildElementXML());
    }

    @Test
    @Ignore("Meant to be ran manually")
    public void testDeepSearch() throws Exception {
        final String server = "localhost";
        final int port = 5222;
        final int timeout = 6000;
        final String pre = "user";
        final int users = 7;

        final List<SmackServiceNode> ssns = new ArrayList<SmackServiceNode>();

        for (int i = 1; i <= users; i++) {
            final SmackServiceNode ssn = new SmackServiceNode(getTcpConnection(server, port, timeout), timeout);
            ssn.connect(Localpart.from(pre + i), pre + i, true, Roster.SubscriptionMode.accept_all);
            ssns.add(ssn);
            System.out.println("Connected " + pre + i);
        }

        Thread.sleep(250);

        for (int i = 0; i < users - 1; i++) {
            Roster.getInstanceFor(ssns.get(i).getConnection()).createEntry(ssns.get(i + 1).getConnection().getUser().asBareJid(), "test", new String[]{});
            ssns.get(i + 1).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.UDP));
            ssns.get(i).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.tracker, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.UDP));
        }

        Thread.sleep(200);

        SmackServiceNode.MappedNodes ma = SmackServiceNode.searchServices(ssns.get(0).getConnection(), users * 2, users, users * 2, null, true);
        Thread.sleep(200);

        assertTrue(ma.getRelayEntries().size() >= users - 1);
        assertTrue(ma.getTrackerEntries().size() >= users - 2);

        for (final TrackerEntry entry : ma.getRelayEntries().values()) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssns.get(0).getConnection(), entry.getJid());

            assertNotNull(iq);

            assertEquals(IQ.Type.result, iq.getType());

            assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getLocalport(), iq.getRemoteport()));
        }

        for (final SmackServiceNode sn : ssns) {
            sn.getConnection().disconnect();
        }

        Thread.sleep(500);
    }

    @Test
    @Ignore("Meant to be ran manually")
    public void testDeepASyncSearch() throws InterruptedException, XMPPException, IOException, SmackException, ExecutionException {
        final String server = "localhost";
        final int port = 5222;
        final int timeout = 6000;
        final String pre = "user";
        final int users = 7;

        final List<SmackServiceNode> ssns = new ArrayList<SmackServiceNode>();

        for (int i = 1; i <= users; i++) {
            final SmackServiceNode ssn = new SmackServiceNode(getTcpConnection(server, port, timeout), timeout);
            ssn.connect(Localpart.from(pre + i), pre + i, true, Roster.SubscriptionMode.accept_all);
            ssns.add(ssn);
            System.out.println("Connected " + pre + i);
        }

        Thread.sleep(250);

        for (int i = 0; i < users - 1; i++) {
            Roster.getInstanceFor(ssns.get(i).getConnection()).createEntry(ssns.get(i + 1).getConnection().getUser().asBareJid(), "test", new String[]{});
            ssns.get(i + 1).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.relay, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.UDP));
            ssns.get(i).addTrackerEntry(new TrackerEntry(TrackerEntry.Type.tracker, TrackerEntry.Policy._public, ssns.get(i + 1).getConnection().getUser(), JingleChannelIQ.UDP));
        }

        Thread.sleep(200);

        Future<MappedNodes> f = SmackServiceNode.aSyncSearchServices(ssns.get(0).getConnection(), users * 2, users, users * 2, null, true);
        MappedNodes ma = f.get();

        assertTrue(ma.getRelayEntries().size() >= users - 1);
        assertTrue(ma.getTrackerEntries().size() >= users - 2);

        for (final TrackerEntry entry : ma.getRelayEntries().values()) {
            JingleChannelIQ iq = SmackServiceNode.getChannel(ssns.get(0).getConnection(), entry.getJid());

            assertNotNull(iq);

            assertEquals(IQ.Type.result, iq.getType());

            assertTrue(RelayChannelTest.testDatagramChannelsExternal(iq.getLocalport(), iq.getRemoteport()));
        }

        for (final SmackServiceNode sn : ssns) {
            sn.getConnection().disconnect();
        }

        Thread.sleep(500);
    }

}