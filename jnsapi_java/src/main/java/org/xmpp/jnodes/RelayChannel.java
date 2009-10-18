package org.xmpp.jnodes;

import org.xmpp.jnodes.nio.DatagramListener;
import org.xmpp.jnodes.nio.EventDatagramChannel;
import org.xmpp.jnodes.nio.ListenerDatagramChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class RelayChannel {

    private final ListenerDatagramChannel channelA;
    private final ListenerDatagramChannel channelB;
    private SocketAddress lastReceivedA;
    private SocketAddress lastReceivedB;

    public RelayChannel(final String hostA, final int portA, final String hostB, final int portB) throws IOException {

        channelA = EventDatagramChannel.open(null, new InetSocketAddress(hostA, portA));
        channelB = EventDatagramChannel.open(null, new InetSocketAddress(hostB, portB));

        channelA.setDatagramListener(new DatagramListener() {
            public void datagramReceived(ListenerDatagramChannel channel, ByteBuffer buffer, SocketAddress address) {
                lastReceivedA = address;
                if (lastReceivedB != null) {
                    try {
                        channelB.send(buffer, lastReceivedB);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        channelB.setDatagramListener(new DatagramListener() {
            public void datagramReceived(ListenerDatagramChannel channel, ByteBuffer buffer, SocketAddress address) {
                lastReceivedB = address;
                if (lastReceivedA != null) {
                    try {
                        channelA.send(buffer, lastReceivedA);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

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
