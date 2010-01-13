/**
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Lesser Public License (LGPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.sparkplugin;

import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.spark.PresenceManager;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.phone.Phone;
import org.jivesoftware.spark.phone.PhoneManager;
import org.jivesoftware.spark.plugin.Plugin;
import org.jivesoftware.spark.ui.ChatRoom;
import org.jivesoftware.spark.ui.TranscriptWindow;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;
import org.xmpp.jnodes.jingle.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * A simple Jingle Plugin for Spark that uses server Media Proxy for the transport and NAT Traversal
 */
public class JinglePlugin implements Plugin, Phone, ConnectionListener, IncomingSessionListener {

    private JingleSessionManager sessionManager;

    public void initialize() {
        // Add Jingle to discovered items list.
        SparkManager.addFeature(Jingle.xmlns);

        // Set Jingle Enabled
        JingleIQ.enableJingle(SparkManager.getConnection());

        // Add to PhoneManager
        PhoneManager.getInstance().addPhone(this);

        // Adds a tab handler.
        SparkManager.getChatManager().addSparkTabHandler(new JingleTabHandler());

        final SwingWorker jingleLoadingThread = new SwingWorker() {
            public Object construct() {
                sessionManager = new JingleSessionManager(SparkManager.getConnection(), new MediaProvider() {
                    public List<JingleContent> getJingleContents() {
                        return null;
                    }

                    public List<JingleContent> getJingleContents(List<JingleContent> jingleContent) {
                        return null;
                    }
                }, new RawUdpTransportProvider() {
                    public void setupTransport(List<JingleContent> contents) {
                        
                    }
                });
                return true;
            }

            public void finished() {
                addListeners();
            }
        };

        jingleLoadingThread.start();

        SparkManager.getConnection().addConnectionListener(this);
    }

    /**
     * Adds Jingle and ChatRoom listeners.
     */
    private void addListeners() {
        // Listen in for new incoming Jingle requests.
        sessionManager.addIncomingSessionListener(this);
    }


    public Collection<Action> getPhoneActions(final String jid) {

        String fullJID = PresenceManager.getFullyQualifiedJID(jid);

        final Presence p = PresenceManager.getPresence(fullJID);

        final boolean supportsJingle = true;

        if (!supportsJingle) {
            return Collections.emptyList();
        }

        final List<Action> actions = new ArrayList<Action>();
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1467355627829748086L;

            public void actionPerformed(ActionEvent e) {
                placeCall(jid);
            }
        };

        action.putValue(Action.NAME, "<html><b>" + JingleResources.getString("label.computer.to.computer") + "</b></html>");
        action.putValue(Action.SMALL_ICON, SparkRes.getImageIcon(SparkRes.COMPUTER_IMAGE_16x16));
        actions.add(action);
        return actions;
    }


    public void placeCall(String jid) {

        // cancel call request if no Media Locator available
        if (PhoneManager.isUseStaticLocator() && PhoneManager.isUsingMediaLocator()) {
            return;
        }

        PhoneManager.setUsingMediaLocator(true);

        jid = SparkManager.getUserManager().getFullJID(jid);

        ChatRoom room = SparkManager.getChatManager().getChatRoom(StringUtils.parseBareAddress(jid));
        if (JingleStateManager.getInstance().getJingleRoomState(room) != null) {
            return;
        }

        SparkManager.getChatManager().getChatContainer().activateChatRoom(room);

        // Create a new Jingle Call with a full JID
        //JingleSession session = new JingleSession();

        TranscriptWindow transcriptWindow = room.getTranscriptWindow();
        StyledDocument doc = (StyledDocument) transcriptWindow.getDocument();
        Style style = doc.addStyle("StyleName", null);

        OutgoingCall outgoingCall = new OutgoingCall();
        //outgoingCall.handleOutgoingCall(session, room, jid);
        StyleConstants.setComponent(style, outgoingCall);

        // Insert the image at the end of the text
        try {
            doc.insertString(doc.getLength(), "ignored text", style);
            doc.insertString(doc.getLength(), "\n", null);
        }
        catch (BadLocationException e) {
            Log.error(e);
        }

        room.scrollToBottom();
    }

    public void shutdown() {
    }

    public boolean canShutDown() {
        return false;
    }

    public void uninstall() {
    }

    /**
     * Notify user that a new incoming jingle request has been receieved.
     *
     * @param session the <code>JingleSession</code>.
     */
    private void incomingJingleSession(JingleSession session) {
        if (PhoneManager.isUseStaticLocator() && PhoneManager.isUsingMediaLocator()) {
            // Reject
        } else {
            PhoneManager.setUsingMediaLocator(true);
            new IncomingCall(session);
        }
    }


    public void connectionClosed() {
    }

    public void connectionClosedOnError(Exception e) {
    }

    public void reconnectingIn(int seconds) {
    }

    public void reconnectionSuccessful() {
        JingleIQ.enableJingle(SparkManager.getConnection());
        SparkManager.addFeature(Jingle.xmlns);
    }

    public void reconnectionFailed(Exception e) {

    }

    public void sessionReceived(JingleSession session) {
        incomingJingleSession(session);
    }
}
