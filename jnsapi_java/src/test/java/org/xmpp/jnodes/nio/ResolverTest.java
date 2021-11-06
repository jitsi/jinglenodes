package org.xmpp.jnodes.nio;

import java.net.InetSocketAddress;

import org.junit.Test;

public class ResolverTest {

    @Test
    public void testPublicIP() {
        final InetSocketAddress sa = PublicIPResolver.getPublicAddress("stun1.l.google.com", 19302);

        System.out.println("Public IP: " + sa.getAddress().getHostAddress() + ":" + sa.getPort());
    }

}
