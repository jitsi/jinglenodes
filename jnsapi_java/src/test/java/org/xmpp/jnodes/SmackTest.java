package org.xmpp.jnodes;

import junit.framework.TestCase;

public class SmackTest extends TestCase {

    public void testQ(){
        
    }

//
//    public static final String KEY = "6c1477b1f79bc803655f02f8a5efde80";
//
//    public void testConnection() throws XMPPException, InterruptedException {
//        final ConnectionConfiguration conf = new ConnectionConfiguration("xmpp-1.prod.collecta.com", 5222, "guest.collecta.com");
//        conf.setSASLAuthenticationEnabled(true);
//        conf.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
//        final XMPPConnection connection = new XMPPConnection(conf);
//
//        connection.connect();
//
//        connection.loginAnonymously();
//
//        final IQ iq = createRequestOpt(connection, "search.collecta.com");
//
//        PacketCollector c = connection.createPacketCollector(new PacketIDFilter(iq.getPacketID()));
//        connection.sendPacket(iq);
//        Packet r = c.nextResult(5000);
//
//        //connection.sendPacket(createRequestOpt(connection, "search.collecta.com"));
//
//        assertNotNull(r);
//
//        connection.addPacketListener(new PacketListener() {
//            public void processPacket(Packet packet) {
//                System.out.println("R: " + packet.toXML());
//            }
//        }, new PacketFilter() {
//            public boolean accept(Packet packet) {
//                return true;
//            }
//        });
//
//        Thread.sleep(100000);
//    }
//
//
//    public static IQ createRequest(final XMPPConnection xmppConnection, final String destination) {
//        final IQ iq = new IQ() {
//
//            @Override
//            public String getChildElementXML() {
//                return "<pubsub xmlns='http://jabber.org/protocol/pubsub'>" +
//                        "    <subscribe jid='" + xmppConnection.getUser() + "'" +
//                        "               node='search'/>" +
//                        " </pubsub>";
//            }
//        };
//        iq.setType(IQ.Type.SET);
//        iq.setTo(destination);
//        iq.setFrom(xmppConnection.getUser());
//        return iq;
//    }
//
//    public static IQ createRequestOpt(final XMPPConnection xmppConnection, final String destination) {
//        final IQ iq = new IQ() {
//
//            @Override
//            public String getChildElementXML() {
//                return "<pubsub xmlns='http://jabber.org/protocol/pubsub'>" +
//                        "<subscribe jid='" + xmppConnection.getUser() + "'" +
//                        " node='search'/>" +
//                        "<options>" +
//                        "<x xmlns='jabber:x:data' type='submit'>" +
//                        "<field var='FORM_TYPE' type='hidden'>" +
//                        "<value>http://jabber.org/protocol/pubsub#subscribe_options</value>" +
//                        "</field>" +
//                        "<field var='x-collecta#apikey'>" +
//                        "<value>" + KEY + "</value>" +
//                        "</field>" +
//                        "<field var='x-collecta#notify'>" +
//                        "<value>nimbuzz</value>" +
//                        "</field>" +
//                        "</x>" +
//                        "</options>" +
//                        "</pubsub>";
//            }
//        };
//        iq.setType(IQ.Type.SET);
//        iq.setTo(destination);
//        iq.setFrom(xmppConnection.getUser());
//        return iq;
//    }

}
