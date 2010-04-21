%%%-------------------------------------------------------------------
%%% File    : jn_component.erl
%%% Author  : Thiago Camargo <barata7@gmail.com>
%%% Description : Jingle Nodes Services - External Component
%%% Provides:
%%%		* UDP Relay Services
%%%
%%% Created : 01 Nov 2009 by Thiago Camargo <barata7@gmail.com>
%%% Example Usage: jn_component:start("jn.localhost", "secret", "localhost", 8888, "127.0.0.1", 60000, ["gmail.com","xmpp.org"], 6, 60).
%%%-------------------------------------------------------------------

-module(jn_component).

-define(NS_CHANNEL,'http://jabber.org/protocol/jinglenodes#channel').
-define(NAME_CHANNEL,'candidate').
-define(NS_JINGLE_NODES_s,"http://jabber.org/protocol/jinglenodes").
-define(NS_JINGLE_NODES,'http://jabber.org/protocol/jinglenodes').
-define(NAME_SERVICES,'services').
-define(NS_CHANNEL_s,"http://jabber.org/protocol/jinglenodes#channel").

-define(ERROR_MSG(Format, Args),
	error_logger:error_msg("(~p:~p:~p) " ++ Format ++ "~n",
			       [self(), ?MODULE, ?LINE | Args])).

-define(INFO_MSG(Format, Args),
	error_logger:info_msg("(~p:~p:~p) " ++ Format ++ "~n",
			       [self(), ?MODULE, ?LINE | Args])).

-include_lib("exmpp/include/exmpp.hrl").
-include_lib("exmpp/include/exmpp_client.hrl").

-export([start/1, start/9, stop/1]).
-export([init/9, is_allowed/2]).

-record(relay, {pid, user}).
-record(jn_relay_service, {address, xml}).
-record(jn_tracker_service, {address, xml}).

start([JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds]) ->
           spawn(?MODULE, init, [JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds]).

start(JID, Pass, Server, Port, PubIP, ChannelTimeout,WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
	   spawn(?MODULE, init, [JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds]).

stop(JNComPid) ->
    JNComPid ! stop.

init(JID, Pass, Server, [_|_]=Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
	  {ok, [NPort], _} = io_lib:fread("~u",Port),
	  init(JID, Pass, Server, NPort, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds);

init(JID, Pass, Server, Port, PubIP, [_|_]=ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
	  {ok, [NTimeout], _} = io_lib:fread("~u", ChannelTimeout),
	  init(JID, Pass, Server, Port, PubIP, NTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds);

init(JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    mnesia:create_table(jn_relay_service,
            [{disc_only_copies, [node()]},
             {type, set},
             {attributes, record_info(fields, jn_relay_service)}]),
    mnesia:create_table(jn_tracker_service,
            [{disc_only_copies, [node()]},
             {type, set},
             {attributes, record_info(fields, jn_tracker_service)}]),    
    mod_monitor:init(),
    application:start(exmpp),
    XmppCom = exmpp_component:start(),
    exmpp_component:auth(XmppCom, JID, Pass),
    _StreamId = exmpp_component:connect(XmppCom, Server, Port),
    exmpp_component:handshake(XmppCom),
    ChannelMonitor = scheduleChannelPurge(5000, [], ChannelTimeout),
    loop(XmppCom, JID, PubIP, ChannelMonitor, [list_to_binary(S) || S <- string:tokens(WhiteDomain, ",")], MaxPerPeriod, PeriodSeconds).

loop(XmppCom, JID, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    receive
        stop ->
            exmpp_component:stop(XmppCom);
	Record = #received_packet{packet_type=iq, type_attr=Type, raw_packet=IQ} ->
	    ?INFO_MSG("IQ Request: ", [Record]),
	    process_iq(XmppCom, Type, IQ, PubIP,exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)),JID, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds),
	    loop(XmppCom, JID, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds);
	Record ->
            ?INFO_MSG("Unknown Request: ", [Record]),
            loop(XmppCom, JID, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds)
    end.

%% Create Channel and return details
process_iq(XmppCom, "get", IQ, PubIP, ?NS_CHANNEL, _, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    P = exmpp_xml:get_attribute(exmpp_iq:get_payload(IQ),"from","server"),
    Permitted = is_allowed(P, WhiteDomain) orelse mod_monitor:accept(P, MaxPerPeriod, PeriodSeconds),	
	if Permitted ->
    		case allocate_relay(ChannelMonitor, P) of
		{A, B} ->
			Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, A, B)),
			exmpp_component:send_packet(XmppCom, Result);
		_ ->
			Error = exmpp_iq:error(IQ),
			exmpp_component:send_packet(XmppCom, Error)
		end;
	true -> 
		Error = exmpp_iq:error(IQ),
                exmpp_component:send_packet(XmppCom, Error)		
	end;

process_iq(XmppCom, "get", IQ, _, ?NS_DISCO_INFO, _, _, _, _, _) ->
        Identity = exmpp_xml:element(?NS_DISCO_INFO, 'identity', [exmpp_xml:attribute("category", <<"proxy">>),
                                                      exmpp_xml:attribute("type", <<"relay">>),
                                                      exmpp_xml:attribute("name", <<"Jingle Nodes Relay">>)
                                                      ],
                                     []),
        IQRegisterFeature1 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_JINGLE_NODES_s)],[]),
        IQRegisterFeature2 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_CHANNEL_s)],[]),
        Result = exmpp_iq:result(IQ, exmpp_xml:element(?NS_DISCO_INFO, 'query', [], [Identity, IQRegisterFeature1, IQRegisterFeature2])),
        exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, ?NS_JINGLE_NODES, JID, _, _, _, _) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol', "udp"), exmpp_xml:attribute('address', JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	io:format("~p~n", [Result]),
	exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, _, _, _, _, _, _) ->
		    Error = exmpp_iq:error(IQ,'feature-not-implemented'),
		    exmpp_component:send_packet(XmppCom, Error).

get_candidate_elem(Host, A, B) ->
	Raw_Elem = exmpp_xml:element(?NS_CHANNEL,?NAME_CHANNEL),
        Elem_A = exmpp_xml:set_attribute(Raw_Elem, "localport", A),
        Elem_B = exmpp_xml:set_attribute(Elem_A, "remoteport", B),
	exmpp_xml:set_attribute(Elem_B,"host", Host).

is_allowed(_, []) -> true;
is_allowed(Jid, WhiteDomain) ->
	case exmpp_jid:is_jid(Jid) of
	true -> is_allowed(Jid, exmpp_jid:domain(Jid), WhiteDomain);
	_ -> false
	end.
is_allowed(_, Domain, WhiteDomain) -> lists:any(fun(S) -> S== Domain end, WhiteDomain).

allocate_relay(ChannelMonitor, U) -> allocate_relay(ChannelMonitor, U, 10000,10).
allocate_relay(_, U, _, 0) -> 
	 ?ERROR_MSG("Could Not Allocate Port for : ~p", [U]),
	{error, null};
allocate_relay(ChannelMonitor, U, I, Tries) ->
     case udp_relay:start(I, I+2) of
	{ok, R} -> 
		ChannelMonitor ! #relay{pid=R, user=U},
		{I, I+2};
	_ -> allocate_relay(ChannelMonitor, U, I+3, Tries-1)
     end.

check_relay(#relay{pid= PID, user=U}, Timeout) ->
	T = gen_server:call(PID, get_timestamp),	
	Delta = timer:now_diff(now(), T)/1000,
	if
	Delta > Timeout ->
		?INFO_MSG("Channel Killed: ", [U]),
		exit(PID, kill),
		removed;
	true -> 
		ok
	end.

check_relays(Relays, Timeout) ->
	check_relays(Relays, Timeout, []).

check_relays([], _, Remain) -> Remain;
check_relays([A|B], Timeout, Remain) ->
	case check_relay(A, Timeout) of
	ok -> check_relays(B, Timeout, [A|Remain]);
	_ -> check_relays(B, Timeout, Remain)
	end.

scheduleChannelPurge(Period, Relays, Timeout) -> spawn(fun () -> schedule(Period, Relays, Timeout) end).

schedule(Period, Relays, Timeout) ->
    receive
        NewRelay -> 
		?INFO_MSG("Relay Added: ~p~n", [NewRelay]),
		schedule(Period, [NewRelay | Relays], Timeout)
    after Period ->
	Remain = check_relays(Relays, Timeout),
        schedule(Period, Remain, Timeout)
    end.
