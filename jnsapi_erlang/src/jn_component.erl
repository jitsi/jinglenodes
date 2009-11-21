-module(jn_component).

-define(NS_CHANNEL,'http://jabber.org/protocol/jinglenodes#channel').
-define(NAME_CHANNEL,'candidate').
-define(NS_JINGLE_NODES_s,"http://jabber.org/protocol/jinglenodes").
-define(NS_JINGLE_NODES,'http://jabber.org/protocol/jinglenodes').
-define(NAME_SERVICES,'services').
-define(NS_CHANNEL_s,"http://jabber.org/protocol/jinglenodes#channel").

-include_lib("exmpp/include/exmpp.hrl").
-include_lib("exmpp/include/exmpp_client.hrl").

-export([start/5, stop/1]).
-export([init/5]).

start(JID, Pass, Server, Port, PubIP) ->
	   spawn(?MODULE, init, [JID, Pass, Server, Port, PubIP]).

stop(JNComPid) ->
    JNComPid ! stop.

init(JID, Pass, Server, [_|_]=Port, PubIP) ->
	  {ok, [NPort], _} = io_lib:fread("~u",Port),
	  init(JID, Pass, Server, NPort, PubIP);

init(JID, Pass, Server, Port, PubIP) ->
    application:start(exmpp),
    XmppCom = exmpp_component:start(),
    exmpp_component:auth(XmppCom, JID, Pass),
    _StreamId = exmpp_component:connect(XmppCom, Server, Port),
    exmpp_component:handshake(XmppCom),
    loop(XmppCom, JID, PubIP).

loop(XmppCom, JID, PubIP) ->
    receive
        stop ->
            exmpp_component:stop(XmppCom);
        %% If we receive a message, we reply with the same message
        Record = #received_packet{packet_type=message, raw_packet=Packet} ->
            io:format("~p~n", [Record]),
            process_message(XmppCom, Packet, JID),
            loop(XmppCom, JID, PubIP);
	Record = #received_packet{packet_type=iq, type_attr=Type, raw_packet=IQ} ->
	    io:format("~p~n", [Record]),
	    process_iq(XmppCom, Type, IQ, PubIP,exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)),JID),
	    loop(XmppCom, JID, PubIP);
	Record ->
            io:format("~p~n", [Record]),
            loop(XmppCom, JID, PubIP)
    end.

%% Create Channel and return details
process_iq(XmppCom, "get", IQ, PubIP, ?NS_CHANNEL, _) ->
    case allocate_relay() of
	{A, B} ->
		Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, A, B)),
		exmpp_component:send_packet(XmppCom, Result);
	_ ->
		Error = exmpp_iq:error(IQ),
		exmpp_component:send_packet(XmppCom, Error)
	end; 

process_iq(XmppCom, "get", IQ, _, ?NS_DISCO_INFO, _) ->
        Identity = exmpp_xml:element(?NS_DISCO_INFO, 'identity', [exmpp_xml:attribute("category", <<"proxy">>),
                                                      exmpp_xml:attribute("type", <<"relay">>),
                                                      exmpp_xml:attribute("name", <<"Jingle Nodes Relay">>)
                                                      ],
                                     []),
        IQRegisterFeature1 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_JINGLE_NODES_s)],[]),
        IQRegisterFeature2 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_CHANNEL_s)],[]),
        Result = exmpp_iq:result(IQ, exmpp_xml:element(?NS_DISCO_INFO, 'query', [], [Identity, IQRegisterFeature1, IQRegisterFeature2])),
        exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, ?NS_JINGLE_NODES, JID) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol',"udp"), exmpp_xml:attribute('address',JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	io:format("~p~n", [Result]),
	exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, _, _) ->
		    Error = exmpp_iq:error(IQ,'feature-not-implemented'),
		    exmpp_component:send_packet(XmppCom, Error).

get_candidate_elem(Host, A, B) ->
	Raw_Elem = exmpp_xml:element(?NS_CHANNEL,?NAME_CHANNEL),
        Elem_A = exmpp_xml:set_attribute(Raw_Elem, "porta", A),
        Elem_B = exmpp_xml:set_attribute(Elem_A, "portb", B),
	exmpp_xml:set_attribute(Elem_B,"host", Host).

%% Reply Stats
process_message(XmppCom, Packet, JID) ->
    From = exmpp_xml:get_attribute(Packet, from, <<"unknown">>),
    To = exmpp_xml:get_attribute(Packet, to, JID),
    Tmp = exmpp_xml:set_attribute(Packet, from, To),
    Tmp2 = exmpp_xml:set_attribute(Tmp, to, From),
    NewPacket = exmpp_xml:remove_attribute(Tmp2, id),
    exmpp_component:send_packet(XmppCom, NewPacket).

allocate_relay() -> allocate_relay(10000,10).
allocate_relay(_, 0) -> {error};
allocate_relay(I, Tries) ->
     case udp_relay:start(I,I+2) of
	{ok, _} -> {I,I+2};
	_ -> allocate_relay(I+3,Tries-1)
     end.

