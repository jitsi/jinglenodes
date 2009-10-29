package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
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
        connection = new XMPPConnection(conf);
    }

    public void connect(final String user, final String password) throws XMPPException {
        connection.connect();
        connection.addConnectionListener(this);
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

    protected IQ createUdpChannel() {

        try {
            final RelayChannel rc = RelayChannel.createLocalRelayChannel();
            final int id = ids.incrementAndGet();
            final String sId = String.valueOf(id);

            rc.setAttachment(sId);
            final IQ result = new IQ() {
                public String getChildElementXML() {
                    final StringBuilder str = new StringBuilder();
                    str.append("<").append(JingleChannelIQ.NAME).append(" xmlns='").append(JingleChannelIQ.NAMESPACE).append("' protocol='").append(JingleChannelIQ.Protocol.udp).append("' id='").append(sId).append("' ");
                    str.append("ip='").append(rc.getIp()).append("' ");
                    str.append("porta='").append(rc.getPortA()).append("' portb='").append(rc.getPortB()).append("'/>");
                    return str.toString();
                }
            };

            result.setType(IQ.Type.RESULT);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return JingleChannelIQ.createEmptyError();
        }

    }

    public void processPacket(final Packet packet) {

        System.out.println("Received: " + packet.toXML());

    }

    public XMPPConnection getConnection() {
        return connection;
    }
}
