import junit.framework.TestCase;
import org.xmpp.jnodes.EventDatagramChannel;
import org.xmpp.jnodes.DatagramListener;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.BindException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class EventDatagramTest extends TestCase {

    final static String encode = "UTF-8";
    final static String localIP = "127.0.0.1";

    public void testDatagram() {
        try {

            int num = 10;
            int packets = 50;
            int tests = 100;
            List<TestSocket> cs = new ArrayList<TestSocket>();

            for (int i = 0, j = 0; i < num; i++, j++) {
                for (int t = 0; t < 5; t++) {
                    try {
                        final TestSocket s = new TestSocket(localIP, 50000 + j);
                        cs.add(s);
                        break;
                    } catch (BindException e) {
                        j++;
                    }
                }
            }

            for (int h = 0; h < tests; h++) {

                final long delta = System.currentTimeMillis();

                for (int ii = 0; ii < packets; ii++)
                    for (int i = 0; i < num; i++) {
                        final TestSocket a = cs.get(i);
                        final TestSocket b = i == num - 1 ? cs.get(0) : cs.get(i + 1);
                        a.getChannel().send(b.expectedBuffer.duplicate(), b.address);
                    }

                boolean finished = false;
                while (!finished) {
                    Thread.sleep(1);
                    finished = true;
                    for (int i = 0; i < num; i++) {
                        finished &= cs.get(i).getI().get() == packets;
                    }
                }

                System.out.println("Elapsed Time: " + (System.currentTimeMillis() - delta));

                for (int i = 0; i < num; i++)
                    cs.get(i).getI().set(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static class TestSocket {

        final private String msg;
        final private byte[] b;
        final private AtomicInteger i;
        final private SocketAddress address;
        final private EventDatagramChannel channel;
        final private ByteBuffer expectedBuffer;

        public TestSocket(final String localIP, final int port) throws IOException {
            msg = String.valueOf(Math.random() * 10);
            b = msg.getBytes(encode);
            expectedBuffer = ByteBuffer.wrap(b);
            i = new AtomicInteger(0);
            address = new InetSocketAddress(localIP, port);
            channel = EventDatagramChannel.open(new DatagramListener() {
                public void datagramReceived(final EventDatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
                    final byte[] bt = new byte[b.length];
                    final int aux = buffer.position();
                    buffer.rewind();
                    buffer.get(bt, 0, aux);
                    if (Arrays.equals(bt, b)) {
                        i.incrementAndGet();
                    } else {
                        System.out.println("Invalid Buffer Content.");
                    }
                }
            }, address);
        }

        public AtomicInteger getI() {
            return i;
        }

        public String getMsg() {
            return msg;
        }

        public EventDatagramChannel getChannel() {
            return channel;
        }
    }
}
