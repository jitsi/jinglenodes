package org.xmpp.jnodes.jingle;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.filter.PacketFilter;

import java.util.concurrent.ConcurrentHashMap;

public class JingleSessionManager implements PacketListener {

    final private XMPPConnection connection;
    final private ConcurrentHashMap<String, JingleSession> sessions = new ConcurrentHashMap<String, JingleSession>();
    final private ConcurrentHashMap<IncomingSessionListener, IncomingSessionListener> listeners = new ConcurrentHashMap<IncomingSessionListener, IncomingSessionListener>();
    final private MediaProvider mediaProvider;
    final private RawUdpTransportProvider rawUdpTransportProvider;

    public JingleSessionManager(XMPPConnection connection, MediaProvider mediaProvider, RawUdpTransportProvider rawUdpTransportProvider) {
        this.connection = connection;
        this.mediaProvider = mediaProvider;
        this.rawUdpTransportProvider = rawUdpTransportProvider;
        connection.addPacketListener(this, new PacketFilter() {
            public boolean accept(final Packet packet) {
                return packet instanceof JingleIQ;
            }
        });
    }

    public void addIncomingSessionListener(final IncomingSessionListener incomingSessionListener) {
        listeners.put(incomingSessionListener, incomingSessionListener);
    }

    private String getID(final JingleIQ iq) {
        return iq.getJingle().getInitiator() + iq.getJingle().getSid();
    }

    public void processPacket(final Packet packet) {
        final JingleIQ iq = (JingleIQ) packet;

        if (Jingle.Action.sessionInitiate.equals(iq.getJingle().getAction())) {
            final JingleSession js = new JingleSession(iq, false, mediaProvider, rawUdpTransportProvider, connection);
            sessions.put(getID(iq), js);
            for (final IncomingSessionListener l : listeners.values()) {
                l.sessionReceived(js);
            }
        } else {
            final JingleSession js = sessions.get(getID(iq));
            if (js != null) {
                js.processJingleIQ(iq);
            }
        }
    }
}
