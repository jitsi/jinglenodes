package org.xmpp.jnodes.jingle;

import java.util.List;

public interface MediaProvider {
    public List<JingleContent> getJingleContents();

    public List<JingleContent> getJingleContents(final List<JingleContent> jingleContent);
}
