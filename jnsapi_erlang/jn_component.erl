-module(jn_component).

-include_lib("exmpp/include/exmpp.hrl").
-include_lib("exmpp/include/exmpp_client.hrl").

-export([start/4, stop/1]).
-export([init/4]).

start(JID, Pass, Server, Port) ->
    spawn(?MODULE, init, [JID, Pass, Server, Port]).

stop(JNComPid) ->
    JNComPid ! stop.

init(JID, Pass, Server, Port) ->
    application:start(exmpp),
    XmppCom = exmpp_component:start(),
    exmpp_component:auth(XmppCom, JID, Pass),
    _StreamId = exmpp_component:connect(XmppCom, Server, Port),
    exmpp_component:handshake(XmppCom),
    loop(XmppCom, JID).

loop(XmppCom, JID) ->
    receive
        stop ->
            exmpp_component:stop(XmppCom);
        %% If we receive a message, we reply with the same message
        Record = #received_packet{packet_type=message, raw_packet=Packet} ->
            io:format("~p~n", [Record]),
            process_message(XmppCom, Packet, JID),
            loop(XmppCom, JID);
	Record = #received_packet{packet_type=iq, type_attr=get, raw_packet=Packet} ->
	    io:format("~p~n", [Record]),
	    process_iq(XmppCom, Packet, JID),
	    loop(XmppCom, JID);
	Record ->
            io:format("~p~n", [Record]),
            loop(XmppCom, JID)
    end.

%% Create Channel and return details
process_iq(XmppCom, Packet, JID) ->
    From = exmpp_xml:get_attribute(Packet, from, <<"unknown">>),
    To = exmpp_xml:get_attribute(Packet, to, JID),
    TmpPacket = exmpp_xml:set_attribute(Packet, from, To),
    TmpPacket2 = exmpp_xml:set_attribute(TmpPacket, to, From),
    NewPacket = exmpp_xml:remove_attribute(TmpPacket2, id),
    exmpp_component:send_packet(XmppCom, NewPacket).

%% Reply Stats
process_message(XmppCom, Packet, JID) ->
    From = exmpp_xml:get_attribute(Packet, from, <<"unknown">>),
    To = exmpp_xml:get_attribute(Packet, to, JID),
    TmpPacket = exmpp_xml:set_attribute(Packet, from, To),
    TmpPacket2 = exmpp_xml:set_attribute(TmpPacket, to, From),
    NewPacket = exmpp_xml:remove_attribute(TmpPacket2, id),
    exmpp_component:send_packet(XmppCom, NewPacket).

