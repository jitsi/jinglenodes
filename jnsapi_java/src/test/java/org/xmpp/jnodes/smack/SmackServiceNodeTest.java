package org.xmpp.jnodes.smack;

import junit.framework.TestCase;
import org.jivesoftware.smack.XMPPException;
import org.junit.Ignore;

public class SmackServiceNodeTest extends TestCase {

    @Ignore("Meant to be ran manually")
    public void testConnect() {

        final String server = "jabber.org";
        final int port = 5222;
        final String user = "user";
        final String pass = "pass";

        final SmackServiceNode ssn = new SmackServiceNode(server, port);

        try {
            ssn.connect(user, pass);

            System.out.println("Connected");

        } catch (XMPPException e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }
}