package org.xmpp.jnodes.jingle;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
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
        ProviderManager.getInstance().addIQProvider(Jingle.elementName, Jingle.xmlns, new JingleIQ(new Jingle()));
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

    public static Class[] getClasses(final String pckgname) throws ClassNotFoundException {
        ArrayList<Class> classes = new ArrayList<Class>();
        // Get a File object for the package
        File directory = null;
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final Enumeration<URL> urls = cl.getResources(pckgname.replace('.', '/'));
            for (final URL url : Collections.list(urls)) {
                directory = new File(url.toURI());
                if (directory.exists()) {
                    // Get the list of the files contained in the package
                    String[] files = directory.list();
                    for (String file : files) {
                        // we are only interested in .class files
                        if (file.endsWith(".class")) {
                            // removes the .class extension
                            classes.add(Class.forName(pckgname + '.' + file.substring(0, file.length() - 6)));
                        }
                    }
                } else {
                    throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
                }
            }
        } catch (NullPointerException x) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
        } catch (URISyntaxException e) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
        } catch (IOException e) {
            throw new ClassNotFoundException(pckgname + " does not appear to be a valid package");
        }

        Class[] classesA = new Class[classes.size()];
        classes.toArray(classesA);
        return classesA;
    }
}
