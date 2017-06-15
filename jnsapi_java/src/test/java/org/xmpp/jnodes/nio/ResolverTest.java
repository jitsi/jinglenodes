package org.xmpp.jnodes.nio;

import junit.framework.TestCase;

import java.net.InetSocketAddress;

public class ResolverTest extends TestCase {

    public void testPublicIP() {
        final InetSocketAddress sa = PublicIPResolver.getPublicAddress("stun1.l.google.com", 19302);

        System.out.println("Public IP: " + sa.getAddress().getHostAddress() + ":" + sa.getPort());
    }

}
