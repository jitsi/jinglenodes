package org.xmpp.jnodes.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class MockSocket {
    public interface ChannelProvider {
        public SelDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException;

        public String getName();
    }
    //test
    final static String encode = "UTF-8";
    final private String msg;
    final private byte[] b;
    final private AtomicInteger i;
    final private SocketAddress address;
    final private SelDatagramChannel channel;
    final private ByteBuffer expectedBuffer;

    public MockSocket(final String localIP, final int port, final ChannelProvider provider) throws IOException {
        msg = String.valueOf(Math.random() * 10);
        b = msg.getBytes(encode);
        expectedBuffer = ByteBuffer.wrap(b);
        i = new AtomicInteger(0);
        address = new InetSocketAddress(localIP, port);

        final DatagramListener dl = new DatagramListener() {
            @Override
            public void datagramReceived(final SelDatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
                try {
                    final int aux = buffer.position();
                    final byte[] bt = new byte[aux];
                    buffer.rewind();
                    buffer.get(bt, 0, aux);
                    if (Arrays.equals(bt, b)) {
                        i.incrementAndGet();
                    } else {
                        System.out.println("Invalid Buffer Content.");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        channel = provider.open(dl, address);
    }

    public AtomicInteger getI() {
        return i;
    }

    public String getMsg() {
        return msg;
    }

    public SelDatagramChannel getChannel() {
        return channel;
    }

    public ByteBuffer getExpectedBuffer() {
        return expectedBuffer;
    }

    public SocketAddress getAddress() {
        return address;
    }
}