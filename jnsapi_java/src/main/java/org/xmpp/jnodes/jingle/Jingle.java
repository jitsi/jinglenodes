package org.xmpp.jnodes.jingle;

import java.util.ArrayList;
import java.util.List;

public class Jingle extends JingleElement {

    public static class Action {
        final public static String sessionInitiate = "session-initiate";
        final public static String sessionTerminate = "session-terminate";
        final public static String sessionAccept = "session-accept";
        final public static String sessionInfo = "session-info";
    }

    @Skip
    public static final String elementName = "jingle";
    final public static String xmlns = "urn:xmpp:tmp:jingle";
    String action;
    String initiator;
    String responder;
    String sid;

    JingleInfoRing ringing;

    final List<JingleContent> contents = new ArrayList<JingleContent>();

    public void addContent(final JingleContent content) {
        contents.add(content);
    }

    public void removeContent(final JingleContent content) {
        contents.remove(content);
    }

    public String getAction() {
        return action;
    }

    public String getInitiator() {
        return initiator;
    }

    public String getResponder() {
        return responder;
    }

    public String getSid() {
        return sid;
    }

    public List<JingleContent> getContents() {
        return contents;
    }

    public String getElementName() {
        return elementName;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public void setResponder(String responder) {
        this.responder = responder;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public void setRinging(JingleInfoRing ringing) {
        this.ringing = ringing;
    }

    public JingleInfoRing getRinging() {
        return ringing;
    }
}
