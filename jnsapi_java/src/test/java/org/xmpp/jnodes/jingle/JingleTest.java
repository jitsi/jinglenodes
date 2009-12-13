package org.xmpp.jnodes.jingle;

import junit.framework.TestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmpp.jnodes.nio.LocalIPResolver;
import org.xmpp.jnodes.smack.SmackServiceNode;

import java.io.IOException;
import java.io.StringReader;

public class JingleTest extends TestCase {

    public void testCandidate() throws XmlPullParserException, IOException {
        XmlPullParser parser = new MXParser();

        String ip = "127.0.0.1";
        String port = "9009";
        String type = "";
        final JingleRawUdpCandidate c = new JingleRawUdpCandidate();
        c.setIp(ip);
        c.setPort(port);
        c.setType(type);
        System.out.println(c.getChildElementXML());

        final JingleRawUdpTransport transport = new JingleRawUdpTransport();
        final int max = 2;
        for (int i = 0; i < max; i++) {
            ip = "127.0.0.1";
            port = String.valueOf(9000 + i);
            type = "";
            final JingleRawUdpCandidate cc = new JingleRawUdpCandidate();
            cc.setIp(ip);
            cc.setPort(port);
            cc.setType(type);
            transport.addCandidate(cc);
        }

        final JingleDescription jd = new JingleDescription();
        jd.setMedia("audio");
        for (int i = 0; i < max; i++) {
            JinglePayloadType pt = new JinglePayloadType();
            pt.setId(String.valueOf(i));
            pt.setName("ilbc");
            pt.setClockrate(String.valueOf(16000 + i * 100));
            jd.addPayloadtype(pt);
        }
        System.out.println(jd.getChildElementXML());

        final JingleContent jc = new JingleContent();
        jc.setCreator("initiator");
        jc.setName("call");
        jc.setDescription(jd);
        jc.setTransport(transport);

        System.out.println(jc.getChildElementXML());

        final Jingle j = new Jingle();
        j.setAction(Jingle.Action.sessionInitiate);
        j.setInitiator("user@jn.com");
        j.setResponder("userb@jn.com");
        j.setSid("123");
        j.addContent(jc);

        System.out.println(j.getChildElementXML());

        final JingleIQ jiq = new JingleIQ(j);

        System.out.println(jiq.toXML());

        final Jingle jt = new Jingle();
        jt.setAction(Jingle.Action.sessionTerminate);
        jt.setInitiator("user@jn.com");
        jt.setResponder("userb@jn.com");
        jt.setSid("123");

        System.out.println(jt.getChildElementXML());

        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(jt.getChildElementXML()));
        JingleIQ provider = new JingleIQ(jt);
        JingleIQ r = provider.parseIQ(parser);
        assertEquals(jt.getChildElementXML(), r.getChildElementXML());

        parser.setInput(new StringReader(j.getChildElementXML()));
        JingleIQ rr = provider.parseIQ(parser);
        assertEquals(j.getChildElementXML(), rr.getChildElementXML());

        final Jingle jr = new Jingle();
        jr.setAction(Jingle.Action.sessionInfo);
        jr.setInitiator("user@jn.com");
        jr.setResponder("userb@jn.com");
        jr.setSid("123");
        jr.setRinging(new JingleInfoRing());

        parser.setInput(new StringReader(jr.getChildElementXML()));
        JingleIQ rrr = provider.parseIQ(parser);
        assertEquals(jr.getChildElementXML(), rrr.getChildElementXML());

    }


    public void testConnect() throws InterruptedException, XMPPException, IOException {

        LocalIPResolver.setOverrideIp("127.0.0.1");

        final String proxy = null;
        final String server = "localhost";
        final int port = 5222;
        final String user1 = "user1";
        final String pass1 = "user1";
        final String user2 = "user2";
        final String pass2 = "user2";
        final int timeout = 1250;

        final SmackServiceNode ssn1 = new SmackServiceNode(server, port, timeout);

        final SmackServiceNode ssn2 = new SmackServiceNode(server, port, timeout);

        ssn2.connect(user2, pass2, true, Roster.SubscriptionMode.accept_all);
        ssn1.connect(user1, pass1, true, Roster.SubscriptionMode.accept_all);

        ssn1.getConnection().getRoster().createEntry(ssn2.getConnection().getUser().split("/")[0], "test", new String[]{});
        ssn2.getConnection().getRoster().createEntry(ssn1.getConnection().getUser().split("/")[0], "test", new String[]{});

        JingleIQ.enableJingle(ssn1.getConnection());
        JingleIQ.enableJingle(ssn2.getConnection());


        final Jingle initiate = new Jingle();
        initiate.setAction(Jingle.Action.sessionInitiate);
        initiate.setInitiator(ssn1.getConnection().getUser());
        initiate.setResponder(ssn2.getConnection().getUser());

        final JingleContent content = new JingleContent();
        content.setName("audio");
        content.setCreator(JingleContent.Creator.INITIATOR);

        final JingleDescription description = new JingleDescription();
        description.setMedia("audio");
        description.addPayloadtype(new JinglePayloadType("102", "ilbc", "8000"));
        content.setDescription(description);

        final JingleRawUdpTransport transport = new JingleRawUdpTransport();
        transport.addCandidate(new JingleRawUdpCandidate("127.0.0.1", "10500", null));

        initiate.addContent(content);

        final JingleIQ iq = new JingleIQ(initiate);
        iq.setFrom(ssn1.getConnection().getUser());
        iq.setTo(proxy != null ? proxy : ssn2.getConnection().getUser());
        iq.setPacketID(IQ.nextID());

        ssn2.getConnection().addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {

                if (packet instanceof JingleIQ) {
                    System.out.println(packet.toXML());
                }

            }
        }, new PacketFilter() {
            public boolean accept(Packet packet) {
                return true;
            }
        });

        ssn1.getConnection().sendPacket(iq);

        Thread.sleep(500);
    }
}
