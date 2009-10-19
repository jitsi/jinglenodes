package org.xmpp.jnodes;

import junit.framework.TestCase;
import org.xmpp.jnodes.nio.*;

import java.io.IOException;
import java.net.BindException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class RelayChannelTest extends TestCase {

    final static String encode = "UTF-8";
    final static String localIP = "127.0.0.1";

    public void testDatagramChannels() {

        for (int i = 0; i < 5; i++) {
            socketTest(new TestSocket.ChannelProvider() {
                public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                    return EventDatagramChannel.open(datagramListener, address);
                }

                public String getName() {
                    return "EventDatagramChannel";
                }
            });

            socketTest(new TestSocket.ChannelProvider() {
                public ListenerDatagramChannel open(DatagramListener datagramListener, SocketAddress address) throws IOException {
                    return SelDatagramChannel.open(datagramListener, address);
                }

                public String getName() {
                    return "SelDatagramChannel";
                }
            });
        }
    }

    public void socketTest(final TestSocket.ChannelProvider provider) {
        try {

            int num = 10;
            int packets = 50;
            int tests = 100;
            final List<TestSocket> cs = new ArrayList<TestSocket>();
            final List<RelayChannel> rc = new ArrayList<RelayChannel>();

            assertEquals(num % 2, 0);

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
                if (i % 2 == 0) {
                    for (int t = 0; t < 50; t++) {
                        try {
                            final RelayChannel c = new RelayChannel(localIP, 60000 + l, localIP, 60000 + l + 1);
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

            for (int h = 0; h < tests; h++) {

                final long start = System.currentTimeMillis();

                for (int ii = 0; ii < packets; ii++)
                    for (int i = 0; i < num; i++) {
                        final TestSocket a = cs.get(i);
                        final TestSocket b = i % 2 == 0 ? cs.get(i + 1) : cs.get(i - 1);

                        final RelayChannel c = rc.get(i / 2);
                        final SocketAddress d = i % 2 == 0 ? c.getAddressA() : c.getAddressB();

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
            for (final RelayChannel r : rc) {
                r.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
