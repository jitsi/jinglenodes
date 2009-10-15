package org.xmpp.jnodes;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface DatagramListener {

    public void datagramReceived(EventDatagramChannel channel, ByteBuffer buffer, SocketAddress address);

}
