package org.xmpp.jnodes;

import org.xmpp.jnodes.nio.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.BindException;
import java.nio.ByteBuffer;

public class RelayChannel {

    private final ListenerDatagramChannel channelA;
    private final ListenerDatagramChannel channelB;
    private final SocketAddress addressA;
    private final SocketAddress addressB;
    private SocketAddress lastReceivedA;
    private SocketAddress lastReceivedB;
    private long lastReceivedTimeA;
    private long lastReceivedTimeB;
    private final int portA;
    private final int portB;
    private final String ip;
    private Object attachment;

    public static RelayChannel createLocalRelayChannel() throws IOException {

        final String ip = LocalIPResolver.getLocalIP();
        int range = 40000;
        IOException be = null;

        for (int t = 0; t < 50; t++) {
            try {
                int a = Math.round((int) (Math.random() * 10000)) + range;
                return new RelayChannel(ip, a, a + 1);
            } catch (BindException e) {
                be = e;
            } catch (IOException e) {
                be = e;
            }
        }
        throw be;
    }

    public RelayChannel(final String host, final int portA, final int portB) throws IOException {

        addressA = new InetSocketAddress(host, portA);
        addressB = new InetSocketAddress(host, portB);

        channelA = SelDatagramChannel.open(null, addressA);
        channelB = SelDatagramChannel.open(null, addressB);

        channelA.setDatagramListener(new DatagramListener() {
            public void datagramReceived(final ListenerDatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
                lastReceivedA = address;
                lastReceivedTimeA = System.currentTimeMillis();

                if (lastReceivedB != null) {
                    try {
                        buffer.flip();
                        channelB.send(buffer, lastReceivedB);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        channelB.setDatagramListener(new DatagramListener() {
            public void datagramReceived(final ListenerDatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
                lastReceivedB = address;
                lastReceivedTimeB = System.currentTimeMillis();
                if (lastReceivedA != null) {
                    try {
                        buffer.flip();
                        channelA.send(buffer, lastReceivedA);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        this.portA = portA;
        this.portB = portB;
        this.ip = host;
    }

    public SocketAddress getAddressB() {
        return addressB;
    }

    public SocketAddress getAddressA() {
        return addressA;
    }

    public int getPortA() {
        return portA;
    }

    public int getPortB() {
        return portB;
    }

    public String getIp() {
        return ip;
    }

    public long getLastReceivedTimeA() {
        return lastReceivedTimeA;
    }

    public long getLastReceivedTimeB() {
        return lastReceivedTimeB;
    }

    public Object getAttachment() {
        return attachment;
    }

    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public void close() {
        try {
            channelA.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            channelB.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
