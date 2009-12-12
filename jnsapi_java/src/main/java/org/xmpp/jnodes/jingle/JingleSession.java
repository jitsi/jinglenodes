package org.xmpp.jnodes.jingle;

import org.xmpp.jnodes.smack.JingleChannelIQ;

public class JingleSession {

    private final JingleIQ sessionInitiate;
    private JingleIQ sessionInfo;
    private JingleIQ sessionAccept;
    private JingleIQ sessionTerminate;

    private JingleChannelIQ jingleChannelIQ;

    private final boolean outbound;

    public JingleSession(final JingleIQ sessionInitiate, final boolean outbound) {
        if (sessionInitiate == null) {
            throw new IllegalArgumentException("SessionInitiate cannot be null");
        }
        this.sessionInitiate = sessionInitiate;
        this.outbound = outbound;
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

}
