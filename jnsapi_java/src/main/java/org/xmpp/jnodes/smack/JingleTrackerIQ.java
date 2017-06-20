package org.xmpp.jnodes.smack;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.Jid;

public class JingleTrackerIQ extends IQ {

    public static final String NAME = "services";
    public static final String NAMESPACE = "http://jabber.org/protocol/jinglenodes";

    private final ConcurrentHashMap<Jid, TrackerEntry> entries = new ConcurrentHashMap<Jid, TrackerEntry>();

    public JingleTrackerIQ() {
        super(NAME, NAMESPACE);
        this.setType(Type.get);
    }

    public boolean isRequest() {
        return Type.get.equals(this.getType());
    }

    public void addEntry(final TrackerEntry entry) {
        entries.put(entry.getJid(), entry);
    }

    public void removeEntry(final TrackerEntry entry) {
        entries.remove(entry.getJid());
    }

    @Override
    public IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder str) {
        str.rightAngleBracket();
        for (final TrackerEntry entry : entries.values()) {
            str.halfOpenElement(entry.getType().toString())
                .attribute("policy", entry.getPolicy().toString())
                .attribute("address", entry.getJid())
                .attribute("protocol", entry.getProtocol())
                .optBooleanAttribute("verified", entry.isVerified())
                .closeEmptyElement();
        }

        return str;
    }

    public Collection<TrackerEntry> getEntries() {
        return entries.values();
    }
}
