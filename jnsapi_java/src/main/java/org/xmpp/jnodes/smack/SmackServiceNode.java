package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.xmpp.jnodes.RelayChannel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;

public class SmackServiceNode implements ConnectionListener, PacketListener {

    private final XMPPConnection connection;
    private final ConcurrentHashMap<String, RelayChannel> channels = new ConcurrentHashMap<String, RelayChannel>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private final AtomicInteger ids = new AtomicInteger(0);

    static {
        ProviderManager.getInstance().addIQProvider(JingleChannelIQ.NAME, JingleChannelIQ.NAMESPACE, new JingleNodesProvider());
    }

    public SmackServiceNode(final String server, final int port, final long timeout) {
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

        final ConnectionConfiguration conf = new ConnectionConfiguration(server, port);
        conf.setSASLAuthenticationEnabled(false);
        conf.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        connection = new XMPPConnection(conf);
    }

    public void connect(final String user, final String password) throws XMPPException {
        connect(user, password, false);
    }

    public void connect(final String user, final String password, final boolean tryCreateAccount) throws XMPPException {
        connection.connect();
        connection.addConnectionListener(this);
        if (tryCreateAccount) {
            try {
                connection.getAccountManager().createAccount(user, password);
            } catch (final XMPPException e) {
                // Do Nothing as account may exists
            }
        }
        connection.login(user, password);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(JingleChannelIQ.NAMESPACE);
        connection.addPacketListener(this, new PacketFilter() {
            public boolean accept(Packet packet) {
                return packet instanceof JingleChannelIQ;
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
        c.close();
        channels.remove(c.getAttachment());
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
        }

    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public static JingleChannelIQ getChannel(final XMPPConnection xmppConnection, final String serviceNode) {
        if (xmppConnection == null || !xmppConnection.isConnected()) return null;

        final JingleChannelIQ iq = new JingleChannelIQ();
        iq.setFrom(xmppConnection.getUser());
        iq.setTo(serviceNode);

        PacketCollector collector = xmppConnection.createPacketCollector(new PacketIDFilter(iq.getPacketID()));
        xmppConnection.sendPacket(iq);
        JingleChannelIQ result = (JingleChannelIQ) collector.nextResult(Math.round(SmackConfiguration.getPacketReplyTimeout() * 1.5));
        collector.cancel();

        return result;
    }

    ConcurrentHashMap<String, RelayChannel> getChannels() {
        return channels;
    }
}
