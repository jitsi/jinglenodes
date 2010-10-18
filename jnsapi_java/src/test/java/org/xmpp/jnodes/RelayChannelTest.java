package org.xmpp.jnodes;

import junit.framework.TestCase;
import org.junit.Ignore;
import org.xmpp.jnodes.nio.DatagramListener;
import org.xmpp.jnodes.nio.ListenerDatagramChannel;
import org.xmpp.jnodes.nio.MockSocket;
import org.xmpp.jnodes.nio.SelDatagramChannel;

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
        int max = 1;

        for (int i = 0; i < max; i++) {
            final int ii = i;
            futures.add(executorService.submit(new Runnable() {
                public void run() {
                    try {
                        socketTest(new MockSocket.ChannelProvider() {
                            public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                                return SelDatagramChannel.open(datagramListener, address);
                            }

                            public String getName() {
                                return "SelDatagramChannel";
                            }
                        }, 1500 * ii + 10000, 1500 * ii + 11000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
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
    public static boolean testDatagramChannelsExternal(final int portA, final int portB) throws IOException, InterruptedException {

        final SocketAddress sa = new InetSocketAddress(localIP, portA);
        final SocketAddress sb = new InetSocketAddress(localIP, portB);

        boolean f = true;
        for (int i = 0; i < 1 && f; i++) {
            f = socketTest(new MockSocket.ChannelProvider() {
                public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                    return SelDatagramChannel.open(datagramListener, address);
                }

                public String getName() {
                    return "SelDatagramChannel";
                }
            }, sa, sb);
        }
        return f;
    }

    public void socketTest(final MockSocket.ChannelProvider provider, final int socketRange, final int relayRange) throws IOException, InterruptedException {
        final int num = 2;
        final int packets = 10;
        final int tests = 1;
        final List<MockSocket> cs = new ArrayList<MockSocket>();
        final List<RelayChannel> rc = new ArrayList<RelayChannel>();

        for (int i = 0, j = 0, l = 0; i < num; i++, j++, l++) {
            for (int t = 0; t < num; t++) {
                try {
                    final MockSocket s = new MockSocket(localIP, socketRange + j, provider);
                    cs.add(s);
                    break;
                } catch (BindException e) {
                    j++;
                }
            }
            if (i % 2 == 0) {
                for (int t = 0; t < num; t++) {
                    try {
                        final RelayChannel c = new RelayChannel(localIP, relayRange + l);
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
                            final MockSocket a = cs.get(i);
                            final MockSocket b = i % 2 == 0 ? cs.get(i + 1) : cs.get(i - 1);

                            final RelayChannel c = rc.get(i / 2);
                            final SocketAddress d = i % 2 == 0 ? c.getAddressA() : c.getAddressB();

                            try {
                                int ss = 0;
                                while (ss == 0) {
                                    ss = a.getChannel().send(b.getExpectedBuffer().duplicate(), d);
                                    if (ss == 0) {
                                        System.out.println("Retrying Send...");
                                    } else {
                                        sent.incrementAndGet();
                                    }
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
                for (final MockSocket s : cs) {
                    if (s.getI().get() >= packets) {
                        b++;
                    } else if (s.getI().get() >= target) {
                        a++;
                    }
                }
                finished = a + b == num;
            }

            final long d = (System.currentTimeMillis() - start);
            if (d > max) {
                max = d;
            }
            if (d < min) {
                min = d;
            }
            tTime += d;

            for (final MockSocket ts : cs)
                ts.getI().set(0);
        }

        System.out.println(provider.getName() + " -> Max: " + max + "ms, Min: " + min + "ms, Avg: " + Math.ceil(tTime / tests) + "ms");

        for (final MockSocket ts : cs) {
            ts.getChannel().close();
        }
        for (final RelayChannel r : rc) {
            r.close();
        }

    }

    public static boolean socketTest(final MockSocket.ChannelProvider provider, final SocketAddress sa, final SocketAddress sb) throws IOException, InterruptedException {
        final int num = 2;
        int packets = 25;
        int tests = 100;
        final List<MockSocket> cs = new ArrayList<MockSocket>();

        for (int i = 0, j = 0, l = 0; i < num; i++, j++, l++) {
            for (int t = 0; t < 50; t++) {
                try {
                    final MockSocket s = new MockSocket(localIP, 20000 + j, provider);
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
        boolean finished = true;

        for (int h = 0; h < tests && finished; h++) {

            final long start = System.currentTimeMillis();

            for (int ii = 0; ii < packets; ii++)
                for (int i = 0; i < num; i++) {
                    final MockSocket a = cs.get(i);
                    final MockSocket b = i % 2 == 0 ? cs.get(i + 1) : cs.get(i - 1);

                    final SocketAddress d = i % 2 == 0 ? sa : sb;

                    a.getChannel().send(b.getExpectedBuffer().duplicate(), d);
                }

            finished = false;
            final int target = packets - 1;
            int t = 0;
            while (!finished && t < packets * tests) {
                Thread.sleep(1);
                finished = true;
                for (int i = 0; i < num; i++) {
                    finished &= cs.get(i).getI().get() >= target;
                }
                t++;
            }

            if (finished) {
                final long d = (System.currentTimeMillis() - start);
                if (d > max) {
                    max = d;
                }
                if (d < min) {
                    min = d;
                }
                tTime += d;
            }

            for (final MockSocket ts : cs)
                ts.getI().set(0);
        }

        if (finished) {
            System.out.println(provider.getName() + " -> Max: " + max + "ms, Min: " + min + "ms, Avg: " + Math.ceil(tTime / tests) + "ms");
        }

        for (final MockSocket ts : cs) {
            ts.getChannel().close();
        }

        return finished;
    }

    public void testSocketPerformance() {
        int max = 500;
        for (int j = 0; j < max; j++)
            for (int i = 0; i < max; i++) {
                try {
                    SelDatagramChannel c = SelDatagramChannel.open(null, new InetSocketAddress(localIP, i + 30000));
                    c.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    public void testSocketPerformanceConcurrent() {
        int max = 100;
        int t = 10;
        final List<List<SelDatagramChannel>> lists = new ArrayList<List<SelDatagramChannel>>();

        for (int i = 0; i < t; i++) {
            lists.add(new ArrayList<SelDatagramChannel>());
        }

        int r = 0;
        for (int j = 0; j < max; j++)
            for (int i = 0; i < max; i++) {
                try {
                    SelDatagramChannel c = SelDatagramChannel.open(null, new InetSocketAddress(localIP, i + 20000 + (j * max)));
                    lists.get(r++).add(c);
                    if (r >= t) {
                        r = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        for (int i = 0; i < t; i++) {
            final int tt = t;
            executorService.submit(new Runnable() {
                public void run() {
                    for (final SelDatagramChannel c : lists.get(tt)) {
                        try {
                            c.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public static void main(String args[]) {
        try {
            testDatagramChannelsExternal(Integer.valueOf(args[1]), Integer.valueOf(args[2]));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testRelayChannel() throws IOException {
        final RelayChannel rc = RelayChannel.createLocalRelayChannel("0.0.0.0",10000,30000);

        assertNotNull(rc);

    }
}
