package org.xmpp.jnodes.smack;

import junit.framework.TestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPConnection;
import org.junit.Ignore;

public class SmackServiceNodeTest extends TestCase {

    @Ignore("Meant to be ran manually")
    public void testConnect() {

        final String server = "sa.azzu.com.br";
        final int port = 5222;
        final String user1 = "12011910";
        final String pass1 = "63416341";
        final String user2 = "12014410";
        final String pass2 = "63416341";

        XMPPConnection.DEBUG_ENABLED=true;        

        final SmackServiceNode ssn1 = new SmackServiceNode(server, port, 60000);

        final SmackServiceNode ssn2 = new SmackServiceNode(server, port, 60000);

        try {
            ssn2.connect(user2, pass2);
            ssn1.connect(user1, pass1);

            Thread.sleep(1000);

            final JingleChannelIQ iq = new JingleChannelIQ();
            iq.setFrom(ssn1.getConnection().getUser());
            iq.setTo(ssn2.getConnection().getUser());

            System.out.println(iq.toXML());

            ssn1.getConnection().sendPacket(iq);

            System.out.println("Connected");

            Thread.sleep(100000);

        } catch (XMPPException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}