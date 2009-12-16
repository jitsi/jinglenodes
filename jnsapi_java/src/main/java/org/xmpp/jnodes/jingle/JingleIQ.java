package org.xmpp.jnodes.jingle;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.XMPPConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class JingleIQ extends IQ implements IQProvider {

    final Jingle jingle;

    final static List<Class> parsingClasses = new ArrayList<Class>();

    static {
        final Class[] classes = {Jingle.class, JingleContent.class, JingleDescription.class, JingleElement.class, JinglePayloadType.class, JingleRawUdpCandidate.class, JingleRawUdpTransport.class, JingleInfoRing.class};
        for (final Class c : classes) {
            final Class sup = c.getSuperclass();
            if (sup != null && sup.equals(JingleElement.class)) {
                parsingClasses.add(c);
            }
        }
    }

    public JingleIQ(final Jingle jingle) {
        this.setType(IQ.Type.SET);
        this.jingle = jingle;
    }

    public String getChildElementXML() {
        return this.jingle.getChildElementXML();
    }

    public Jingle getJingle() {
        return jingle;
    }

    public JingleIQ parseIQ(final XmlPullParser parser) throws XmlPullParserException, IOException {
        JingleIQ iq = null;

        boolean done = false;
        int eventType;
        String elementName;
        String namespace;
        List<JingleElement> objects = new ArrayList<JingleElement>(10);
        JingleElement first = null;

        while (!done) {

            eventType = parser.getEventType();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG) {

                for (final Class c : parsingClasses) {

                    if (elementName.equals(JingleElement.getStaticElementName(c))) {
                        final String expectedNamespace = JingleElement.getNamespace(c);
                        if (namespace == null || expectedNamespace == null || namespace.equals(expectedNamespace)) {

                            final Object o;
                            try {
                                o = c.newInstance();

                                if (first == null) {
                                    first = (JingleElement) o;
                                }

                                for (int i = 0; i < parser.getAttributeCount(); i++) {
                                    try {
                                        final String n = parser.getAttributeName(i);
                                        final String v = parser.getAttributeValue(i);
                                        final Method m = c.getMethod("set" + Character.toUpperCase(n.charAt(0)) + n.substring(1), String.class);
                                        m.invoke(o, v);
                                    } catch (NoSuchMethodException e) {
                                        e.printStackTrace();
                                    } catch (InvocationTargetException e) {
                                        e.printStackTrace();
                                    }
                                }
                                objects.add((JingleElement) o);

                            } catch (InstantiationException e) {
                                e.printStackTrace();
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }

                }

            } else if (eventType == XmlPullParser.END_TAG && elementName.equals(first.getElementName())) {
                done = true;
            }
            if (!done) {
                parser.next();
            }
        }

        System.out.println(objects.size());

        if (objects.size() > 0) {
            JingleElement parent, child = null;
            JingleElement root = objects.get(0);
            if (root instanceof Jingle) {
                if (objects.size() > 1) {
                    for (int i = 0; i < objects.size() - 1; i++) {
                        for (JingleElement object : objects) {
                            parent = objects.get(i);
                            child = object;
                            final String n = child.getElementName().replace("-", "");
                            final String nn = Character.toUpperCase(n.charAt(0)) + n.substring(1);
                            Method m = null;
                            try {
                                m = parent.getClass().getMethod("set" + nn, child.getClass());
                            } catch (NoSuchMethodException e) {
                                // Ignore
                            }
                            try {
                                m = parent.getClass().getMethod("add" + nn, child.getClass());
                            } catch (NoSuchMethodException e) {
                                // Ignore
                            }

                            if (m != null) {
                                try {
                                    m.invoke(parent, child);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }
                }
                iq = new JingleIQ((Jingle) root);
            }
        }

        return iq;
    }

    public static void enableJingle(final XMPPConnection connection) {
        ProviderManager.getInstance().addIQProvider(Jingle.elementName, Jingle.xmlns, new JingleIQ(new Jingle()));

        Presence presence = new Presence(Presence.Type.available);
        presence.addExtension(new PacketExtension() {
            public String getElementName() {
                return "c";
            }

            public String getNamespace() {
                return "http://jabber.org/protocol/caps";
            }

            public String toXML() {
                return "<c xmlns=\"http://jabber.org/protocol/caps\" node=\"client:caps\" ver=\"0.1\" ext=\"jingle-v1 voice-v1\"></c>";
            }
        });

        for (int i = 0; i < 3; i++) {
            connection.sendPacket(presence);
        }

    }
}
