package org.xmpp.jnodes.jingle;

import org.xmpp.jnodes.smack.JingleChannelIQ;
import org.jivesoftware.smack.XMPPConnection;

import java.util.concurrent.ConcurrentHashMap;

public class JingleSession {

    private final JingleIQ sessionInitiate;
    private JingleIQ sessionInfo;
    private JingleIQ sessionAccept;
    private JingleIQ sessionTerminate;
    private JingleChannelIQ jingleChannelIQ;
    private final boolean outbound;
    private final ConcurrentHashMap<JingleSessionListener, JingleSessionListener> sessionListeners = new ConcurrentHashMap<JingleSessionListener, JingleSessionListener>();

    final private MediaProvider mediaProvider;
    final private RawUdpTransportProvider rawUdpTransportProvider;
    final private XMPPConnection connection;

    public JingleSession(final JingleIQ sessionInitiate, final boolean outbound, final MediaProvider mediaProvider, final RawUdpTransportProvider rawUdpTransportProvider, final XMPPConnection connection) {
        if (sessionInitiate == null) {
            throw new IllegalArgumentException("SessionInitiate cannot be null");
        }
        this.sessionInitiate = sessionInitiate;
        this.outbound = outbound;
        this.mediaProvider = mediaProvider;
        this.rawUdpTransportProvider = rawUdpTransportProvider;
        this.connection = connection;
    }

    public boolean isAccepted() {
        return sessionAccept != null;
    }

    public boolean isTerminated() {
        return sessionTerminate != null;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public JingleIQ getSessionInitiate() {
        return sessionInitiate;
    }

    public JingleIQ getSessionInfo() {
        return sessionInfo;
    }

    public JingleIQ getSessionAccept() {
        return sessionAccept;
    }

    public JingleIQ getSessionTerminate() {
        return sessionTerminate;
    }

    public JingleChannelIQ getJingleChannelIQ() {
        return jingleChannelIQ;
    }

    public void setJingleChannelIQ(JingleChannelIQ jingleChannelIQ) {
        this.jingleChannelIQ = jingleChannelIQ;
    }

    public void addJingleSessionListener(final JingleSessionListener listener) {
        sessionListeners.put(listener, listener);
    }

    public void processJingleIQ(final JingleIQ iq) {
        if (isSession(iq)) {
            final String sa = iq.getJingle().getAction();
            if (Jingle.Action.sessionInfo.equals(sa)) {
                sessionInfo = iq;
            } else if (Jingle.Action.sessionAccept.equals(sa)) {
                sessionAccept = iq;
            } else if (Jingle.Action.sessionTerminate.equals(sa)) {
                sessionTerminate = iq;
            }
            for (final JingleSessionListener sl : sessionListeners.values()) {
                sl.stateChanged(iq);
            }
        }
    }

    public boolean isSession(final JingleIQ iq) {
        if (iq.getJingle().getSid().equals(sessionInitiate.getJingle().getSid())) {
            if (iq.getJingle().getInitiator().equals(sessionInitiate.getJingle().getInitiator())) {
                return true;
            }
        }
        return false;
    }

    public void ring() {
        if (!outbound && sessionInfo == null) {
            final Jingle j = new Jingle();
            j.setAction(Jingle.Action.sessionInfo);
            j.setInitiator(sessionInitiate.getJingle().getInitiator());
            j.setResponder(connection.getUser());
            j.setRinging(new JingleInfoRing());
            final JingleIQ iq = new JingleIQ(j);
            iq.setFrom(connection.getUser());
            iq.setTo(sessionInitiate.getFrom());
            connection.sendPacket(iq);
        }
    }

    public void accept() {
        if (!outbound && sessionAccept == null) {

            final Jingle j = new Jingle();
            j.setAction(Jingle.Action.sessionAccept);
            j.setInitiator(sessionInitiate.getJingle().getInitiator());
            j.setResponder(connection.getUser());

            j.getContents().clear();
            for (final JingleContent content : mediaProvider.getJingleContents()) {
                content.setCreator(JingleContent.Creator.INITIATOR);
                content.setName(connection.getUser());
                j.addContent(content);
            }
            rawUdpTransportProvider.setupTransport(j.getContents());

            final JingleIQ accept = new JingleIQ(j);
            connection.sendPacket(accept);
        }
    }
}
