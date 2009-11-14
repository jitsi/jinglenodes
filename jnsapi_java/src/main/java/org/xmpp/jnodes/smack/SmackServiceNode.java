package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.xmpp.jnodes.RelayChannel;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SmackServiceNode implements ConnectionListener, PacketListener {

    private final XMPPConnection connection;
    private final ConcurrentHashMap<String, RelayChannel> channels = new ConcurrentHashMap<String, RelayChannel>();
    private final ConcurrentHashMap<String, TrackerEntry> trackerEntries = new ConcurrentHashMap<String, TrackerEntry>();
    private long timeout = 60000;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final AtomicInteger ids = new AtomicInteger(0);

    static {
        ProviderManager.getInstance().addIQProvider(JingleChannelIQ.NAME, JingleChannelIQ.NAMESPACE, new JingleNodesProvider());
        ProviderManager.getInstance().addIQProvider(JingleTrackerIQ.NAME, JingleTrackerIQ.NAMESPACE, new JingleTrackerProvider());
    }

    public SmackServiceNode(final XMPPConnection connection, final long timeout) throws XMPPException {
        this.connection = connection;
        this.timeout = timeout;
        setup();
    }

    public SmackServiceNode(final String server, final int port, final long timeout) {
        final ConnectionConfiguration conf = new ConnectionConfiguration(server, port);
        conf.setSASLAuthenticationEnabled(true);
        conf.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
        connection = new XMPPConnection(conf);
        this.timeout = timeout;
    }

    public void connect(final String user, final String password) throws XMPPException {
        connect(user, password, false, Roster.SubscriptionMode.accept_all);
    }

    public void connect(final String user, final String password, final boolean tryCreateAccount, final Roster.SubscriptionMode mode) throws XMPPException {
        connection.connect();
        connection.addConnectionListener(this);
        if (tryCreateAccount) {
            try {
                connection.getAccountManager().createAccount(user, password);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // Do Nothing
                }
            } catch (final XMPPException e) {
                // Do Nothing as account may exists
            }
        }
        connection.login(user, password);
        connection.getRoster().setSubscriptionMode(mode);
        setup();
    }

    private void setup() {
        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                for (final RelayChannel c : channels.values()) {
                    final long current = System.currentTimeMillis();
                    final long da = current - c.getLastReceivedTimeA();
                    final long db = current - c.getLastReceivedTimeB();

                    if (da > timeout || db > timeout) {
                        removeChannel(c);
                    }
                }
            }
        }, timeout, timeout, TimeUnit.MILLISECONDS);

        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(JingleChannelIQ.NAMESPACE);
        connection.addPacketListener(this, new PacketFilter() {
            public boolean accept(Packet packet) {
                return packet instanceof JingleChannelIQ || packet instanceof JingleTrackerIQ;
            }
        });
    }

    public void connectionClosed() {
        closeAllChannels();
        executor.shutdownNow();
    }

    private void closeAllChannels() {
        for (final RelayChannel c : channels.values()) {
            removeChannel(c);
        }
    }

    private void removeChannel(final RelayChannel c) {
        channels.remove(c.getAttachment());
        c.close();
    }

    public void connectionClosedOnError(Exception e) {
        closeAllChannels();
    }

    public void reconnectingIn(int i) {

    }

    public void reconnectionSuccessful() {

    }

    public void reconnectionFailed(Exception e) {

    }

    protected IQ createUdpChannel(final JingleChannelIQ iq) {

        try {
            final RelayChannel rc = RelayChannel.createLocalRelayChannel();
            final int id = ids.incrementAndGet();
            final String sId = String.valueOf(id);
            rc.setAttachment(sId);

            channels.put(sId, rc);

            final JingleChannelIQ result = new JingleChannelIQ();
            result.setType(IQ.Type.RESULT);
            result.setTo(iq.getFrom());
            result.setFrom(iq.getTo());
            result.setPacketID(iq.getPacketID());
            result.setHost(rc.getIp());
            result.setPorta(rc.getPortA());
            result.setPortb(rc.getPortB());
            result.setId(sId);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return JingleChannelIQ.createEmptyError();
        }

    }

    public void processPacket(final Packet packet) {

        System.out.println("Received: " + packet.toXML());
        if (packet instanceof JingleChannelIQ) {
            final JingleChannelIQ request = (JingleChannelIQ) packet;
            if (request.isRequest()) {
                connection.sendPacket(createUdpChannel(request));
            }
        } else if (packet instanceof JingleTrackerIQ) {
            final JingleTrackerIQ iq = (JingleTrackerIQ) packet;
            if (iq.isRequest()) {
                final JingleTrackerIQ result = createKnownNodes();
                result.setPacketID(packet.getPacketID());
                result.setFrom(packet.getTo());
                result.setTo(packet.getFrom());
                connection.sendPacket(result);
            }
        }

    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public static JingleChannelIQ getChannel(final XMPPConnection xmppConnection, final String serviceNode) {
        if (xmppConnection == null || !xmppConnection.isConnected()) {
            return null;
        }

        final JingleChannelIQ iq = new JingleChannelIQ();
        iq.setFrom(xmppConnection.getUser());
        iq.setTo(serviceNode);

        PacketCollector collector = xmppConnection.createPacketCollector(new PacketIDFilter(iq.getPacketID()));
        xmppConnection.sendPacket(iq);
        JingleChannelIQ result = (JingleChannelIQ) collector.nextResult(Math.round(SmackConfiguration.getPacketReplyTimeout() * 1.5));
        collector.cancel();

        return result;
    }

    public static JingleTrackerIQ getServices(final XMPPConnection xmppConnection, final String serviceNode) {
        if (xmppConnection == null || !xmppConnection.isConnected()) {
            return null;
        }

        final JingleTrackerIQ iq = new JingleTrackerIQ();
        iq.setFrom(xmppConnection.getUser());
        iq.setTo(serviceNode);

        PacketCollector collector = xmppConnection.createPacketCollector(new PacketIDFilter(iq.getPacketID()));
        xmppConnection.sendPacket(iq);
        JingleTrackerIQ result = (JingleTrackerIQ) collector.nextResult(Math.round(SmackConfiguration.getPacketReplyTimeout() * 1.5));
        collector.cancel();

        return result;
    }

    private static void deepSearch(final XMPPConnection xmppConnection, final int maxEntries, final String startPoint, final MappedNodes mappedNodes, final int maxDepth, final JingleChannelIQ.Protocol protocol, final ConcurrentHashMap<String, String> visited) {
        if (xmppConnection == null || !xmppConnection.isConnected()) {
            return;
        }
        if (mappedNodes.getRelayEntries().size() > maxEntries || maxDepth <= 0) {
            return;
        }
        if (startPoint.equals(xmppConnection.getUser())) {
            return;
        }

        JingleTrackerIQ result = getServices(xmppConnection, startPoint);
        visited.put(startPoint, startPoint);
        if (result != null && result.getType().equals(IQ.Type.RESULT)) {
            for (final TrackerEntry entry : result.getEntries()) {
                if (entry.getType().equals(TrackerEntry.Type.tracker)) {
                    mappedNodes.getTrackerEntries().put(entry.getJid(), entry);
                    deepSearch(xmppConnection, maxEntries, entry.getJid(), mappedNodes, maxDepth - 1, protocol, visited);
                } else if (entry.getType().equals(TrackerEntry.Type.relay)) {
                    if (protocol == null || protocol.equals(entry.getProtocol())) {
                        mappedNodes.getRelayEntries().put(entry.getJid(), entry);
                    }
                }
            }
        }
    }

    public static MappedNodes searchServices(final XMPPConnection xmppConnection, final int maxEntries, final int maxDepth, final JingleChannelIQ.Protocol protocol) {
        return searchServices(new ConcurrentHashMap<String, String>(), xmppConnection, maxEntries, maxDepth, protocol);
    }

    private static MappedNodes searchServices(final ConcurrentHashMap<String, String> visited, final XMPPConnection xmppConnection, final int maxEntries, final int maxDepth, final JingleChannelIQ.Protocol protocol) {
        if (xmppConnection == null || !xmppConnection.isConnected()) {
            return null;
        }

        final MappedNodes mappedNodes = new MappedNodes();

        // Request to Server
        deepSearch(xmppConnection, maxEntries, xmppConnection.getHost(), mappedNodes, maxDepth - 1, protocol, visited);

        // Request to Buddies
        for (final RosterEntry re : xmppConnection.getRoster().getEntries()) {
            for (final Iterator<Presence> i = xmppConnection.getRoster().getPresences(re.getUser()); i.hasNext();) {
                final Presence presence = i.next();
                if (presence.isAvailable()) {
                    deepSearch(xmppConnection, maxEntries, presence.getFrom(), mappedNodes, maxDepth - 1, protocol, visited);
                }
            }
        }

        return mappedNodes;
    }

    public static class MappedNodes {
        final ConcurrentHashMap<String, TrackerEntry> relayEntries = new ConcurrentHashMap<String, TrackerEntry>();
        final ConcurrentHashMap<String, TrackerEntry> trackerEntries = new ConcurrentHashMap<String, TrackerEntry>();

        public ConcurrentHashMap<String, TrackerEntry> getRelayEntries() {
            return relayEntries;
        }

        public ConcurrentHashMap<String, TrackerEntry> getTrackerEntries() {
            return trackerEntries;
        }
    }

    ConcurrentHashMap<String, RelayChannel> getChannels() {
        return channels;
    }

    public JingleTrackerIQ createKnownNodes() {

        final JingleTrackerIQ iq = new JingleTrackerIQ();
        iq.setType(IQ.Type.RESULT);

        for (final TrackerEntry entry : trackerEntries.values()) {
            if (!entry.getPolicy().equals(TrackerEntry.Policy._roster)) {
                iq.addEntry(entry);
            }
        }

        return iq;
    }

    public void addTrackerEntry(final TrackerEntry entry) {
        trackerEntries.put(entry.getJid(), entry);
    }

    public void addEntries(final MappedNodes entries) {
        for (final TrackerEntry t : entries.getRelayEntries().values()) {
            addTrackerEntry(t);
        }
        for (final TrackerEntry t : entries.getTrackerEntries().values()) {
            addTrackerEntry(t);
        }
    }
}
