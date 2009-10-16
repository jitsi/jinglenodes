package org.xmpp.jnodes;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface DatagramListener {

    public void datagramReceived(ListenerDatagramChannel channel, ByteBuffer buffer, SocketAddress address);

}
