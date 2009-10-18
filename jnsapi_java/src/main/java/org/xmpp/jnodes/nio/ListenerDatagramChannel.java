package org.xmpp.jnodes.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface ListenerDatagramChannel {
    int send(ByteBuffer src, SocketAddress target) throws IOException;

    void setDatagramListener(final DatagramListener listener);

    void close() throws IOException;
}
