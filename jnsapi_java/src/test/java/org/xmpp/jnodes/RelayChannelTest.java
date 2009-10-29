package org.xmpp.jnodes;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.xmpp.jnodes.nio.DatagramListener;
import org.xmpp.jnodes.nio.ListenerDatagramChannel;
import org.xmpp.jnodes.nio.SelDatagramChannel;
import org.xmpp.jnodes.nio.TestSocket;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class RelayChannelTest extends TestCase {

    final static String encode = "UTF-8";
    final static String localIP = "127.0.0.1";
    private final static ExecutorService executorService = Executors.newCachedThreadPool();

    public void testDatagramChannels() {
        final List<Future> futures = new ArrayList<Future>();

        for (int i = 0; i < 8; i++) {
            final int ii = i;
            futures.add(executorService.submit(new Runnable() {
                public void run() {
                    socketTest(new TestSocket.ChannelProvider() {
                        public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                            return SelDatagramChannel.open(datagramListener, address);
                        }

                        public String getName() {
                            return "SelDatagramChannel";
                        }
                    }, 1500 * ii + 10000, 1500 * ii + 11000);
                }
            }));
        }
        boolean finished = false;

        while (!finished) {
            try {
                Thread.sleep(5000);
                Thread.yield();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finished = true;
            for (final Future f : futures) {
                finished &= f.isDone();
            }
        }
    }

    @Ignore("Meant to be ran manually")
    public static void testDatagramChannelsExternal(final int portA, final int portB) {

        final SocketAddress sa = new InetSocketAddress(localIP, portA);
        final SocketAddress sb = new InetSocketAddress(localIP, portB);

        for (int i = 0; i < 1; i++) {
            socketTest(new TestSocket.ChannelProvider() {
                public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                    return SelDatagramChannel.open(datagramListener, address);
                }

                public String getName() {
                    return "SelDatagramChannel";
                }
            }, sa, sb);
        }
    }

    public void socketTest(final TestSocket.ChannelProvider provider, final int socketRange, final int relayRange) {
        try {

            final int num = 10;
            final int packets = 15;
            final int tests = 50;
            final List<TestSocket> cs = new ArrayList<TestSocket>();
            final List<RelayChannel> rc = new ArrayList<RelayChannel>();

            for (int i = 0, j = 0, l = 0; i < num; i++, j++, l++) {
                for (int t = 0; t < 50; t++) {
                    try {
                        final TestSocket s = new TestSocket(localIP, socketRange + j, provider);
                        cs.add(s);
                        break;
                    } catch (BindException e) {
                        j++;
                    }
                }
                if (i % 2 == 0) {
                    for (int t = 0; t < 50; t++) {
                        try {
                            final RelayChannel c = new RelayChannel(localIP, relayRange + l, relayRange + l + 1);
                            rc.add(c);
                            break;
                        } catch (BindException e) {
                            l++;
                        }
                    }
                }
            }

            long tTime = 0;
            long min = 1000;
            long max = 0;
            final AtomicInteger fSent = new AtomicInteger(0);

            for (int h = 0; h < tests; h++) {
                final List<Future> futures = new ArrayList<Future>();

                final long start = System.currentTimeMillis();

                for (int ii = 0; ii < packets; ii++) {
                    futures.add(executorService.submit(new Runnable() {
                        public void run() {
                            final AtomicInteger sent = new AtomicInteger(0);
                            for (int i = 0; i < num; i++) {
                                final TestSocket a = cs.get(i);
                                final TestSocket b = i % 2 == 0 ? cs.get(i + 1) : cs.get(i - 1);

                                final RelayChannel c = rc.get(i / 2);
                                final SocketAddress d = i % 2 == 0 ? c.getAddressA() : c.getAddressB();

                                try {
                                    int ss = 0;
                                    while (ss == 0) {
                                        ss = a.getChannel().send(b.getExpectedBuffer().duplicate(), d);
                                        if (ss == 0) System.out.println("Retrying Send...");
                                        else sent.incrementAndGet();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            fSent.incrementAndGet();
                        }
                    }));
                }

                boolean finished = false;

                while (!finished) {
                    Thread.sleep(1);
                    finished = true;
                    for (final Future f : futures) {
                        finished &= f.isDone();
                    }
                }

                finished = false;
                final int target = packets - 1;
                while (!finished) {
                    Thread.sleep(5);
                    int a = 0;
                    int b = 0;
                    for (final TestSocket s : cs) {
                        if (s.getI().get() >= packets) b++;
                        else if (s.getI().get() >= target) a++;
                    }
                    finished = a + b == num;
                }

                final long d = (System.currentTimeMillis() - start);
                if (d > max) max = d;
                if (d < min) min = d;
                tTime += d;

                for (final TestSocket ts : cs)
                    ts.getI().set(0);
            }

            System.out.println(provider.getName() + " -> Max: " + max + "ms, Min: " + min + "ms, Avg: " + Math.ceil(tTime / tests) + "ms");

            for (final TestSocket ts : cs) {
                ts.getChannel().close();
            }
            for (final RelayChannel r : rc) {
                r.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void socketTest(final TestSocket.ChannelProvider provider, final SocketAddress sa, final SocketAddress sb) {
        try {

            final int num = 2;
            int packets = 30;
            int tests = 1000;
            final List<TestSocket> cs = new ArrayList<TestSocket>();

            for (int i = 0, j = 0, l = 0; i < num; i++, j++, l++) {
                for (int t = 0; t < 50; t++) {
                    try {
                        final TestSocket s = new TestSocket(localIP, 50000 + j, provider);
                        cs.add(s);
                        break;
                    } catch (BindException e) {
                        j++;
                    }
                }
            }

            long tTime = 0;
            long min = 1000;
            long max = 0;

            for (int h = 0; h < tests; h++) {

                final long start = System.currentTimeMillis();

                for (int ii = 0; ii < packets; ii++)
                    for (int i = 0; i < num; i++) {
                        final TestSocket a = cs.get(i);
                        final TestSocket b = i % 2 == 0 ? cs.get(i + 1) : cs.get(i - 1);

                        final SocketAddress d = i % 2 == 0 ? sa : sb;

                        a.getChannel().send(b.getExpectedBuffer().duplicate(), d);
                    }

                boolean finished = false;
                final int target = packets - 1;
                while (!finished) {
                    Thread.sleep(1);
                    finished = true;
                    for (int i = 0; i < num; i++) {
                        finished &= cs.get(i).getI().get() >= target;
                    }
                }

                final long d = (System.currentTimeMillis() - start);
                if (d > max) max = d;
                if (d < min) min = d;
                tTime += d;

                for (final TestSocket ts : cs)
                    ts.getI().set(0);
            }

            System.out.println(provider.getName() + " -> Max: " + max + "ms, Min: " + min + "ms, Avg: " + Math.ceil(tTime / tests) + "ms");

            for (final TestSocket ts : cs) {
                ts.getChannel().close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) {
        testDatagramChannelsExternal(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
    }
}
