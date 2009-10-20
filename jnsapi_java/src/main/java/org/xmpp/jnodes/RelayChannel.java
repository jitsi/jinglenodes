package org.xmpp.jnodes;

import org.xmpp.jnodes.nio.DatagramListener;
import org.xmpp.jnodes.nio.EventDatagramChannel;
import org.xmpp.jnodes.nio.ListenerDatagramChannel;
import org.xmpp.jnodes.nio.SelDatagramChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class RelayChannel {

    private final ListenerDatagramChannel channelA;
    private final ListenerDatagramChannel channelB;
    private final SocketAddress addressA;
    private final SocketAddress addressB;
    private SocketAddress lastReceivedA;
    private SocketAddress lastReceivedB;

    public RelayChannel(final String hostA, final int portA, final String hostB, final int portB) throws IOException {

        addressA = new InetSocketAddress(hostA, portA);
        addressB = new InetSocketAddress(hostB, portB);

        channelA = SelDatagramChannel.open(null, addressA);
        channelB = SelDatagramChannel.open(null, addressB);

        channelA.setDatagramListener(new DatagramListener() {
            public void datagramReceived(final ListenerDatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
                lastReceivedA = address;
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

    }

    public SocketAddress getAddressB() {
        return addressB;
    }

    public SocketAddress getAddressA() {
        return addressA;
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
