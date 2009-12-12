package org.xmpp.jnodes.jingle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.List;

public abstract class JingleElement {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Skip {
    }

    public String getChildElementXML() {
        final StringBuilder sub = new StringBuilder();
        final StringBuilder str = new StringBuilder("<");
        str.append(getElementName()).append(" ");
        for (final Field f : this.getClass().getDeclaredFields()) {
            if (!f.isAnnotationPresent(Skip.class)) {
                try {
                    final Object o = f.get(this);
                    final String value = gen(o);
                    if (o instanceof List || o instanceof JingleElement) {
                        sub.append(value);
                    } else {
                        if (value != null && value.length() > 0) {
                            str.append(f.getName()).append("='").append(value).append("' ");
                        }
                    }
                } catch (IllegalAccessException e) {
                    // Ignore Field
                }
            }
        }
        if (sub.length() == 0) {
            str.append("/>");
        } else {
            str.append(">");
            str.append(sub.toString());
            str.append("</").append(getElementName()).append(">");
        }
        return str.toString();
    }

    private String gen(final Object o) {
        final String value;

        if (o instanceof List) {
            final StringBuilder s = new StringBuilder();
            for (final Object oo : (List) o) {
                final String partial = gen(oo);
                if (partial != null) {
                    s.append(partial);
                }
            }
            value = s.toString();
        } else if (o instanceof JingleElement) {
            value = ((JingleElement) o).getChildElementXML();
        } else if (o instanceof String) {
            value = o.toString();
        } else {
            value = null;
        }
        return value;
    }

    public abstract String getElementName();

    public static String getNamespace(final Class c) {
        final Field f;
        try {
            f = c.getField("xmlns");
            return (String) f.get(c);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            // Ignore
        }
        return null;
    }

    public static String getStaticElementName(final Class c) {
        final Field f;
        try {
            f = c.getField("elementName");
            return (String) f.get(c);
        } catch (NoSuchFieldException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            // Ignore
        }
        return null;
    }


}
