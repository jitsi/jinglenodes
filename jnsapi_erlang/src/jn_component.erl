%%%-------------------------------------------------------------------
%%% File    : jn_component.erl
%%% Author  : Thiago Camargo <barata7@gmail.com>
%%% Description : Jingle Nodes Services - External Component
%%% Provides:
%%%		* UDP Relay Services
%%%
%%% Created : 01 Nov 2009 by Thiago Camargo <barata7@gmail.com>
%%% Example Usage: jn_component:start("jn.localhost", "secret", "localhost", 8888, "127.0.0.1", 60000, "gmail.com,xmpp.org", 6, 60).
%%%-------------------------------------------------------------------

-module(jn_component).

-define(NS_CHANNEL,'http://jabber.org/protocol/jinglenodes#channel').
-define(NAME_CHANNEL,'channel').
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

start(JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
	   spawn(?MODULE, init, [JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds]).

stop(JNComPid) ->
    JNComPid ! stop.

init(JID, Pass, Server, [_|_]=Port, PubIP, [_|_]=ChannelTimeout, WhiteDomain, [_|_]=MaxPerPeriod, [_|_]=PeriodSeconds) ->
	{ok, [NPort], _} = io_lib:fread("~u",Port),
	{ok, [NTimeout], _} = io_lib:fread("~u", ChannelTimeout),
	{ok, [NMaxPerPeriod], _} = io_lib:fread("~u",MaxPerPeriod),
	{ok, [NPeriodSeconds], _} = io_lib:fread("~u",PeriodSeconds),
	init(JID, Pass, Server, NPort, PubIP, NTimeout, WhiteDomain, NMaxPerPeriod, NPeriodSeconds);

init(JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    mnesia:create_table(jn_relay_service,
            [{disc_only_copies, [node()]},
             {type, set},
             {attributes, record_info(fields, jn_relay_service)}]),
    mnesia:create_table(jn_tracker_service,
            [{disc_only_copies, [node()]},
             {type, set},
             {attributes, record_info(fields, jn_tracker_service)}]),    
    application:start(exmpp),
    mod_monitor:init(),
    XmppCom = exmpp_component:start(),
    make_connection(XmppCom, JID, Pass, Server, Port),
    ChannelMonitor = scheduleChannelPurge(5000, [], ChannelTimeout),
    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, [list_to_binary(S) || S <- string:tokens(WhiteDomain, ",")], MaxPerPeriod, PeriodSeconds).

make_connection(XmppCom, JID, Pass, Server, Port) ->
    exmpp_component:auth(XmppCom, JID, Pass),
    _StreamId = exmpp_component:connect(XmppCom, Server, Port),
    exmpp_component:handshake(XmppCom).

loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    receive
        stop ->
            exmpp_component:stop(XmppCom);
	Record = #received_packet{packet_type=iq, type_attr=Type, raw_packet=IQ, from=From} ->
	    ?INFO_MSG("IQ Request: ~p~n", [Record]),
	    process_iq(XmppCom, Type, IQ, From, PubIP,exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)),JID, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds),
	    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds);
	{_, tcp_closed} ->
	    exmpp_component:stop(XmppCom),
	    ?INFO_MSG("Connection Closed. Trying to Reconnect...~n", [make_connection(XmppCom, JID, Pass, Server, Port)]),
	    make_connection(XmppCom, JID, Pass, Server, Port),
	    ?INFO_MSG("Reconnected.~n", []),
	    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds);
	Record ->
            ?INFO_MSG("Unknown Request: ~p~n", [Record]),
            loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds)
    end.

%% Create Channel and return details
process_iq(XmppCom, "get", IQ, From, PubIP, ?NS_CHANNEL, _, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds) ->
    Permitted = is_allowed(From, WhiteDomain) andalso mod_monitor:accept(From, MaxPerPeriod, PeriodSeconds),	
	if Permitted == true ->
    		case allocate_relay(ChannelMonitor, From) of
		{A, B} ->
			?INFO_MSG("Allocated Port for : ~p~n", [From]),
			Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, A, B)),
			exmpp_component:send_packet(XmppCom, Result);
		_ ->
			?ERROR_MSG("Could Not Allocate Port for : ~p~n", [From]),
			Error = exmpp_iq:error_without_original(IQ, 'internal-server-error'),
			exmpp_component:send_packet(XmppCom, Error)
		end;
	true -> 
		?ERROR_MSG("Could Not Allocate Port for : ~p~n", [From]),
		Error = exmpp_iq:error_without_original(IQ, 'policy-violation'),
                exmpp_component:send_packet(XmppCom, Error)		
	end;

process_iq(XmppCom, "get", IQ, _, _, ?NS_DISCO_INFO, _, _, _, _, _) ->
        Identity = exmpp_xml:element(?NS_DISCO_INFO, 'identity', [exmpp_xml:attribute("category", <<"proxy">>),
                                                      exmpp_xml:attribute("type", <<"relay">>),
                                                      exmpp_xml:attribute("name", <<"Jingle Nodes Relay">>)
                                                      ],
                                     []),
        IQRegisterFeature1 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_JINGLE_NODES_s)],[]),
        IQRegisterFeature2 = exmpp_xml:element(?NS_DISCO_INFO, 'feature', [exmpp_xml:attribute('var', ?NS_CHANNEL_s)],[]),
        Result = exmpp_iq:result(IQ, exmpp_xml:element(?NS_DISCO_INFO, 'query', [], [Identity, IQRegisterFeature1, IQRegisterFeature2])),
        exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, _, ?NS_JINGLE_NODES, JID, _, _, _, _) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol', "udp"), exmpp_xml:attribute('address', JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	exmpp_component:send_packet(XmppCom, Result);

process_iq(XmppCom, "get", IQ, _, _, _, _, _, _, _, _) ->
		    Error = exmpp_iq:error(IQ,'feature-not-implemented'),
		    exmpp_component:send_packet(XmppCom, Error).

get_candidate_elem(Host, A, B) ->
	Raw_Elem = exmpp_xml:element(?NS_CHANNEL,?NAME_CHANNEL),
        Elem_A = exmpp_xml:set_attribute(Raw_Elem, "localport", A),
        Elem_B = exmpp_xml:set_attribute(Elem_A, "remoteport", B),
	exmpp_xml:set_attribute(Elem_B,"host", Host).

is_allowed(_, []) -> true;
is_allowed({_,D,_}, WhiteDomain) ->
	is_allowed(D, WhiteDomain);
is_allowed(Domain, WhiteDomain) -> 
	lists:any(fun(S) -> S == Domain end, WhiteDomain).

allocate_relay(ChannelMonitor, U) -> allocate_relay(ChannelMonitor, U, 10000,10).
allocate_relay(_, U, _, 0) -> 
	 ?ERROR_MSG("Could Not Allocate Port for : ~p~n", [U]),
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
		?INFO_MSG("Channel Killed: ~p~n", [U]),
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
