-module(jingle_handler).

-define(NS_CHANNEL,'http://jabber.org/protocol/jinglenodes#channel').
-define(NAME_CHANNEL,'channel').
-define(NS_JINGLE_NODES_s,"http://jabber.org/protocol/jinglenodes").
-define(NS_JINGLE_NODES,'http://jabber.org/protocol/jinglenodes').
-define(NS_JINGLE_NODES_EVENT, 'http://jabber.org/protocol/jinglenodes#event').
-define(NAME_SERVICES,'services').
-define(NS_CHANNEL_s,"http://jabber.org/protocol/jinglenodes#channel").
-define(LOG_PATH, "./jn_component.log").
-define(SERVER, ?MODULE).

-import(config).
-import(file).

-include_lib("exmpp/include/exmpp.hrl").
-include_lib("exmpp/include/exmpp_client.hrl").
-include("../include/p1_logger.hrl").
-include("../include/jn_component.hrl").

%% API
-export([pre_process_iq/4]).
-export([notify_channel/4]).

notify_channel(ID, User, Event, #state{jid=JID, xmppCom=XmppCom}=State) ->
        Notify = exmpp_xml:element(?NS_JINGLE_NODES_EVENT, 'channel', [exmpp_xml:attribute('event', Event), exmpp_xml:attribute('id', ID)], []),
        SetBare = exmpp_iq:set(?NS_JINGLE_NODES_EVENT, Notify),
	SetTo = exmpp_xml:set_attribute(SetBare, to, User),	
	SetFrom = exmpp_xml:set_attribute(SetTo, from, JID),
        exmpp_component:send_packet(XmppCom, SetFrom),
        {ok, State}.

pre_process_iq(Type, IQ, From, State) ->
        ?INFO_MSG("Preparing: ~p~n On State:~p~n", [IQ, State]),
        Payload = exmpp_iq:get_payload(IQ),
        NS = exmpp_xml:get_ns_as_atom(Payload),
        ?INFO_MSG("NS:~p~n", [NS]),
        process_iq(Type, IQ, From, NS, Payload, State).

%% Create Channel and return details
process_iq("get", IQ, From, ?NS_CHANNEL, _, #state{xmppCom=XmppCom, pubIP=PubIP, channelMonitor=ChannelMonitor, whiteDomain=WhiteDomain, maxPerPeriod=MaxPerPeriod, periodSeconds=PeriodSeconds, portMonitor=PortMonitor}=State) ->
    Permitted = jn_component:is_allowed(From, WhiteDomain) andalso mod_monitor:accept(From, MaxPerPeriod, PeriodSeconds),	
	if Permitted == true ->
		?INFO_MSG("T: ~p~n", [PortMonitor]),
    		case allocate_relay(ChannelMonitor, From, PortMonitor) of
		{ok, PortA, PortB, ID} ->
			?INFO_MSG("Allocated Port for : ~p ~p~n", [From, ID]),
			Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, PortA, PortB, ID)),
			exmpp_component:send_packet(XmppCom, Result),
			{ok, State};
		_ ->
			?ERROR_MSG("Could Not Allocate Port for : ~p~n", [From]),
			Error = exmpp_iq:error_without_original(IQ, 'internal-server-error'),
			exmpp_component:send_packet(XmppCom, Error),
			{error, State}
		end;
	true -> 
		?ERROR_MSG("[Not Acceptable] Could Not Allocate Port for : ~p~n", [From]),
		Error = exmpp_iq:error_without_original(IQ, 'policy-violation'),
                exmpp_component:send_packet(XmppCom, Error),
		{error, State}		
	end;

process_iq("get", IQ, _, ?NS_DISCO_INFO, _, #state{xmppCom=XmppCom}=State) ->
        Identity = exmpp_xml:element(?NS_DISCO_INFO, 'identity', [exmpp_xml:attribute("category", <<"proxy">>),
                                                      exmpp_xml:attribute("type", <<"relay">>),
                                                      exmpp_xml:attribute("name", <<"Jingle Nodes Relay">>)
                                                      ],
                                     []),
        IQRegisterFeature1 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_JINGLE_NODES_s)],[]),
        IQRegisterFeature2 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_CHANNEL_s)],[]),
        Result = exmpp_iq:result(IQ, exmpp_xml:element(?NS_DISCO_INFO, 'query', [], [Identity, IQRegisterFeature1, IQRegisterFeature2])),
        exmpp_component:send_packet(XmppCom, Result),
	{ok, State};

process_iq("get", IQ, _, ?NS_JINGLE_NODES, _, #state{jid=JID, xmppCom=XmppCom}=State) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol', "udp"), exmpp_xml:attribute('address', JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	exmpp_component:send_packet(XmppCom, Result),
	{ok, State};

process_iq("get", IQ, _, ?NS_PING, _, #state{xmppCom=XmppCom}=State) ->
        Result = exmpp_iq:result(IQ),
        exmpp_component:send_packet(XmppCom, Result),
        {ok, State};

process_iq(_, IQ, _, _, _, #state{}=State) ->
	?INFO_MSG("Unknown Request: ~p~n", [IQ]),	    
	{ok, State}.

get_candidate_elem(Host, A, B, ID) ->
	Raw_Elem = exmpp_xml:element(?NS_CHANNEL,?NAME_CHANNEL),
        Elem_A = exmpp_xml:set_attribute(Raw_Elem, <<"localport">>, A),
        Elem_B = exmpp_xml:set_attribute(Elem_A, <<"remoteport">>, B),
	Elem_C = exmpp_xml:set_attribute(Elem_B, <<"id">>, jn_component:prepare_id(ID)),
        exmpp_xml:set_attribute(Elem_C, <<"host">>, Host).

allocate_relay(ChannelMonitor, U, PortMonitor) -> allocate_relay(ChannelMonitor, U, 5, PortMonitor).
allocate_relay(_, U, 0, _) -> 
	 ?ERROR_MSG("Could Not Allocate Port for : ~p~n", [U]),
	{error, -1, -1};
allocate_relay(ChannelMonitor, U, Tries, PortMonitor) ->
     	case jn_component:get_port(PortMonitor) of
		{ok, Port} ->
     			PortB = Port + 2,
     			case jingle_relay:start(Port, PortB) of
				{ok, R} ->
					ID=erlang:pid_to_list(R), 
					ChannelMonitor ! #relay{pid=R, user=U, id=ID, creationTime=now()},
					{ok, Port, PortB, ID};
				_ -> allocate_relay(ChannelMonitor, U, Tries-1, PortMonitor)
	     		end;
		{error, M} ->
			?ERROR_MSG("Could Not Allocate Port for : ~p ~p~n", [U, M]),
        		{error, -1, -1}
	end.
