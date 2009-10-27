package org.xmpp.jnodes.smack;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.provider.ProviderManager;

public class SmackServiceNode {

    private final XMPPConnection connection;

    public SmackServiceNode(final String server, final int port) {
        final ConnectionConfiguration conf = new ConnectionConfiguration(server, port);
        connection = new XMPPConnection(conf);
        ProviderManager.getInstance().addIQProvider(JingleNodesProvider.NAME, JingleNodesProvider.NAMESPACE, new JingleNodesProvider());
    }

    public void connect(final String user, final String password) throws XMPPException {
        connection.connect();
        connection.login(user, password);
    }
}
