package org.xmpp.jnodes.jingle;

public interface JingleSessionListener {
    public void stateChanged(final JingleIQ iq);
}
