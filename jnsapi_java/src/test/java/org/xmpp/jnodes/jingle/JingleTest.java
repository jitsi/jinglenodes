package org.xmpp.jnodes.jingle;

import junit.framework.TestCase;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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

}
