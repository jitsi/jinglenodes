import org.xmpp.jnodes.SelDatagramChannel;
import org.xmpp.jnodes.ListenerDatagramChannel;

import java.util.concurrent.atomic.AtomicInteger;
import java.nio.ByteBuffer;
import java.net.SocketAddress;

public interface TestSocket {
    AtomicInteger getI();

    String getMsg();

    ListenerDatagramChannel getChannel();

    ByteBuffer getExpectedBuffer();

    SocketAddress getAddress();
}
