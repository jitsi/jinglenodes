package org.xmpp.jnodes;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class EventDatagramChannel implements ListenerDatagramChannel {

    private final static ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final static ConcurrentHashMap<String, EventDatagramChannel> channels = new ConcurrentHashMap<String, EventDatagramChannel>();
    private static final AtomicInteger idg = new AtomicInteger(10);

    // Instance Properties 
    protected final DatagramChannel channel;
    private final DatagramListener datagramListener;
    private final String id;

    static {

        final Runnable task = new Runnable() {
            public void run() {
                while (true) {
                    try {
                        for (final EventDatagramChannel channel : channels.values()) {
                            try {
                                final ByteBuffer b = ByteBuffer.allocateDirect(1450);
                                if (channel.channel.isOpen()) {
                                    final SocketAddress addr = channel.channel.receive(b);
                                    if (addr != null) {
                                        // Execute in a different Thread avoid serialization
                                        executorService.submit(new Runnable() {
                                            public void run() {
                                                channel.datagramListener.datagramReceived(channel, b, addr);
                                            }
                                        });
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Thread.yield();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        };

        executorService.submit(task);
    }

    protected EventDatagramChannel(final String id, final DatagramChannel channel, final DatagramListener datagramListener) {
        this.id = id;
        this.channel = channel;
        this.datagramListener = datagramListener;
    }

    public static EventDatagramChannel open(final DatagramListener datagramListener, final SocketAddress localAddress) throws IOException {
        final DatagramChannel dc = DatagramChannel.open();
        dc.configureBlocking(false);
        dc.socket().bind(localAddress);
        final EventDatagramChannel c = new EventDatagramChannel(String.valueOf(idg.incrementAndGet()), dc, datagramListener);
        channels.put(c.id, c);
        return c;
    }

    public int send(final ByteBuffer src, final SocketAddress target) throws IOException {
        return this.channel.send(src, target);
    }

    public void close() throws IOException {
        channels.remove(id);
        channel.close();
    }
}
