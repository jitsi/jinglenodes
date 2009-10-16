package org.xmpp.jnodes;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface ListenerDatagramChannel {
    int send(ByteBuffer src, SocketAddress target) throws IOException;

    void close() throws IOException;
}
