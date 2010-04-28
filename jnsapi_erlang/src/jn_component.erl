%%%-------------------------------------------------------------------
%%% File    : jn_component.erl
%%% Author  : Thiago Camargo <barata7@gmail.com>
%%% Description : Jingle Nodes Services - External Component
%%% Provides:
%%%		* UDP Relay Services
%%%
%%% Created : 01 Nov 2009 by Thiago Camargo <barata7@gmail.com>
%%% Example Usage: jn_component:start("jn.localhost", "secret", "localhost", "8888", "127.0.0.1", "60000", "gmail.com,xmpp.org", "6", "60", "10000", "60000").
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

-export([start/1, stop/1]).
-export([init/11, cover_test/0, create_port_list/2]).

-record(relay, {pid, user}).
-record(jn_relay_service, {address, xml}).
-record(jn_tracker_service, {address, xml}).
-record(port_mgr, {init, end_port, list}).

start([JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds, InitPort, EndPort]) ->
           spawn(?MODULE, init, [JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds, InitPort, EndPort]).

stop(JNComPid) ->
    JNComPid ! stop.

init(JID, Pass, Server, [_|_]=Port, PubIP, [_|_]=ChannelTimeout, WhiteDomain, [_|_]=MaxPerPeriod, [_|_]=PeriodSeconds, [_|_]=InitPort, [_|_]=EndPort) ->
	{ok, [NPort], _} = io_lib:fread("~u",Port),
	{ok, [NTimeout], _} = io_lib:fread("~u", ChannelTimeout),
	{ok, [NMaxPerPeriod], _} = io_lib:fread("~u",MaxPerPeriod),
	{ok, [NPeriodSeconds], _} = io_lib:fread("~u",PeriodSeconds),
	{ok, [NInitPort], _} = io_lib:fread("~u",InitPort),
	{ok, [NEndPort], _} = io_lib:fread("~u",EndPort),
	init(JID, Pass, Server, NPort, PubIP, NTimeout, WhiteDomain, NMaxPerPeriod, NPeriodSeconds, NInitPort, NEndPort);

init(JID, Pass, Server, Port, PubIP, ChannelTimeout, WhiteDomain, MaxPerPeriod, PeriodSeconds, InitPort, EndPort) ->
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
    ChannelMonitor = scheduleChannelPurge(5000, [], ChannelTimeout),
    {_, XmppCom} = make_connection(JID, Pass, Server, Port),
    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, [list_to_binary(S) || S <- string:tokens(WhiteDomain, ",")], MaxPerPeriod, PeriodSeconds, #port_mgr{init=InitPort,end_port=EndPort,list=[]}).

make_connection(JID, Pass, Server, Port) -> 
	XmppCom = exmpp_component:start(),
	make_connection(XmppCom, JID, Pass, Server, Port, 20).
make_connection(XmppCom, JID, Pass, Server, Port, 0) -> 
	exmpp_component:stop(XmppCom),
	make_connection(JID, Pass, Server, Port);
make_connection(XmppCom, JID, Pass, Server, Port, Tries) ->
    ?INFO_MSG("Connecting: ~p Tries Left~n",[Tries]),
    exmpp_component:auth(XmppCom, JID, Pass),
    try exmpp_component:connect(XmppCom, Server, Port) of
	R -> exmpp_component:handshake(XmppCom),
		?INFO_MSG("Connected.~n",[]),
		{R, XmppCom}
	catch
		Exception -> ?INFO_MSG("Exception: ~p~n",[Exception]),
		timer:sleep((20-Tries) * 200),
		make_connection(XmppCom, JID, Pass, Server, Port, Tries-1)
    end.

loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, State) ->
    receive
	status ->
	    ChannelMonitor ! status,
	    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, State);
        stop ->
            ?INFO_MSG("Component Stopped.~n",[]),
	    exmpp_component:stop(XmppCom);
	#received_packet{packet_type=iq, type_attr=Type, raw_packet=IQ, from=From} ->
	    %%?INFO_MSG("IQ Request: ~p~n", [Record]),
	    {_, NewState}=process_iq(XmppCom, Type, IQ, From, PubIP,exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)),JID, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, State),
	    loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, NewState);
	{_, tcp_closed} ->
	    ?INFO_MSG("Connection Closed. Trying to Reconnect...~n", []),
	    {_, NewXmppCom} = make_connection(JID, Pass, Server, Port),
	    ?INFO_MSG("Reconnected.~n", []),
	    loop(NewXmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, State);
	Record ->
            ?INFO_MSG("Unknown Request: ~p~n", [Record]),
            loop(XmppCom, JID, Pass, Server, Port, PubIP, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, State)
    end.

%% Create Channel and return details
process_iq(XmppCom, "get", IQ, From, PubIP, ?NS_CHANNEL, _, ChannelMonitor, WhiteDomain, MaxPerPeriod, PeriodSeconds, #port_mgr{}=State) ->
    Permitted = is_allowed(From, WhiteDomain) andalso mod_monitor:accept(From, MaxPerPeriod, PeriodSeconds),	
	if Permitted == true ->
    		case allocate_relay(ChannelMonitor, From, State) of
		{ok, PortA, PortB, NewState} ->
			?INFO_MSG("Allocated Port for : ~p~n", [From]),
			Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, PortA, PortB)),
			exmpp_component:send_packet(XmppCom, Result),
			{ok, NewState};
		_ ->
			?ERROR_MSG("Could Not Allocate Port for : ~p~n", [From]),
			Error = exmpp_iq:error_without_original(IQ, 'internal-server-error'),
			exmpp_component:send_packet(XmppCom, Error),
			{error, State}
		end;
	true -> 
		?ERROR_MSG("Could Not Allocate Port for : ~p~n", [From]),
		Error = exmpp_iq:error_without_original(IQ, 'policy-violation'),
                exmpp_component:send_packet(XmppCom, Error),
		{error, State}		
	end;

process_iq(XmppCom, "get", IQ, _, _, ?NS_DISCO_INFO, _, _, _, _, _, State) ->
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

process_iq(XmppCom, "get", IQ, _, _, ?NS_JINGLE_NODES, JID, _, _, _, _, State) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol', "udp"), exmpp_xml:attribute('address', JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	exmpp_component:send_packet(XmppCom, Result),
	{ok, State};

process_iq(XmppCom, "get", IQ, _, _, ?NS_PING, _, _, _, _, _, State) ->
        Result = exmpp_iq:result(IQ),
        exmpp_component:send_packet(XmppCom, Result),
        {ok, State};

process_iq(XmppCom, "get", IQ, _, _, _, _, _, _, _, _, State) ->
		    Error = exmpp_iq:error(IQ,'feature-not-implemented'),
		    exmpp_component:send_packet(XmppCom, Error),
		    {ok, State}.

get_candidate_elem(Host, A, B) ->
	Raw_Elem = exmpp_xml:element(?NS_CHANNEL,?NAME_CHANNEL),
        Elem_A = exmpp_xml:set_attribute(Raw_Elem, "localport", A),
        Elem_B = exmpp_xml:set_attribute(Elem_A, "remoteport", B),
	exmpp_xml:set_attribute(Elem_B, "host", Host).

is_allowed(_, []) -> true;
is_allowed({_,D,_}, WhiteDomain) ->
	is_allowed(D, WhiteDomain);
is_allowed(Domain, WhiteDomain) -> 
	lists:any(fun(S) -> S == Domain end, WhiteDomain).

create_port_list(Init, End) -> create_port_list(Init, End-4, Init).
create_port_list(Init, End, N) when N < End -> [N| create_port_list(Init, End, N+4)];
create_port_list(_,_,_) -> [].

allocate_relay(ChannelMonitor, U, #port_mgr{} = State) -> allocate_relay(ChannelMonitor, U, 100, State).
allocate_relay(_, U, 0, #port_mgr{} = State) -> 
	 ?ERROR_MSG("Could Not Allocate Port for : ~p~n", [U]),
	{error, -1, -1, State};
allocate_relay(ChannelMonitor, U, Tries, #port_mgr{} = State) ->
     {Port, NewState} = pull_port(State),
     PortB = Port + 2,
     case udp_relay:start(Port, PortB) of
	{ok, R} -> 
		ChannelMonitor ! #relay{pid=R, user=U},
		{ok, Port, PortB, NewState};
	_ -> allocate_relay(ChannelMonitor, U, Tries-1, NewState)
     end.

pull_port(#port_mgr{init=InitPort, end_port=EndPort, list=[]}) -> 
	L = create_port_list(InitPort, EndPort),
	pull_port(#port_mgr{init=InitPort, end_port=EndPort, list=L});
pull_port(#port_mgr{init=InitPort, end_port=EndPort, list=[H|T]}) ->
	{H, #port_mgr{init=InitPort, end_port=EndPort, list=T}}.

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
        status -> 
		io:format("Active Channels ~p~n", [length(Relays)]),
		schedule(Period, Relays, Timeout);
	NewRelay -> 
		?INFO_MSG("Relay Added: ~p~n", [NewRelay]),
		schedule(Period, [NewRelay | Relays], Timeout)
    after Period ->
	Remain = check_relays(Relays, Timeout),
        schedule(Period, Remain, Timeout)
    end.

cover_test() ->
	cover_channels().

cover_channels() -> 
	ChannelMonitor = scheduleChannelPurge(5000, [], 30000),
	cover_channels(ChannelMonitor, 100, #port_mgr{init=10000, end_port=60000, list=[]}).
cover_channels(_, 0, _) -> ok;
cover_channels(ChannelMonitor, T, State) ->		
	{_, _, _, NewState} = allocate_relay(ChannelMonitor, "s", State),
	cover_channels(ChannelMonitor, T-1, NewState).
