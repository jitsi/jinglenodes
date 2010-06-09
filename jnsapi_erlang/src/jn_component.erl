%%%-------------------------------------------------------------------
%%% File    : jn_component.erl
%%% Author  : Thiago Camargo <barata7@gmail.com>
%%% Description : Jingle Nodes Services - External Component
%%% Provides:
%%%		* UDP Relay Services
%%%
%%% Created : 01 Nov 2009 by Thiago Camargo <barata7@gmail.com>
%%% Example Usage: jn_component:start(["jn.localhost", "secret", "localhost", "8888", "127.0.0.1", "60000", "localhost", "6", "60", "10000", "60000"]).
%%%-------------------------------------------------------------------

-module(jn_component).
-behaviour(gen_server).

-define(NS_CHANNEL,'http://jabber.org/protocol/jinglenodes#channel').
-define(NAME_CHANNEL,'channel').
-define(NS_JINGLE_NODES_s,"http://jabber.org/protocol/jinglenodes").
-define(NS_JINGLE_NODES,'http://jabber.org/protocol/jinglenodes').
-define(NAME_SERVICES,'services').
-define(NS_CHANNEL_s,"http://jabber.org/protocol/jinglenodes#channel").
-define(LOG_PATH, "./jn_component.log").
-define(SERVER, ?MODULE).
-define(SOCKOPTS, [binary, {active, once}]).

-import(config).
-import(file).

-include_lib("exmpp/include/exmpp.hrl").
-include_lib("exmpp/include/exmpp_client.hrl").
-include_lib("include/p1_logger.hrl").

%% Cover Test
-export([cover_test/0]).

%% API
-export([start_link/0, init/11, get_stats/0]).

%% gen_server callbacks
-export([init/1, handle_call/3, handle_cast/2, handle_info/2,
         terminate/2, code_change/3]).

-record(relay, {pid, user}).
-record(jn_relay_service, {address, xml}).
-record(jn_tracker_service, {address, xml}).
-record(port_mgr, {init, end_port, list}).
-record(state, {xmppCom, jid, pass, server, port, pubIP, channelMonitor, whiteDomain, maxPerPeriod, periodSeconds, extra}).

start_link() ->
	gen_server:start_link({local, ?SERVER}, ?MODULE, [], []).

%%====================================================================
%% gen_server callbacks
%%====================================================================

%%--------------------------------------------------------------------
%% Function: init(Args) -> {ok, State} |
%%                         {ok, State, Timeout} |
%%                         ignore               |
%%                         {stop, Reason}
%% Description: Initiates the server
%%--------------------------------------------------------------------
init([]) ->
	init_logger(),
	?INFO_MSG("Loading Application",[]),
	case file:consult("jn_component.cfg") of
		{_, Cfg} ->
			?INFO_MSG("Loding Config",[]),
			init(                   get(jid, Cfg),
                                get(pass, Cfg),
                                get(server, Cfg),
                                get(port, Cfg),
                                get(public_ip, Cfg),
                                get(channel_timeout, Cfg),
                                get(whitelist, Cfg),
                                get(max_per_period, Cfg),
                                get(period_seconds, Cfg),
                                get(init_port, Cfg),
                                get(end_port, Cfg));
		_ -> 		
				?INFO_MSG("TESTES~n",[]),
				{error, config_missing}
	end.

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
    {ok, #state{xmppCom=XmppCom, jid=JID, pass=Pass, server=Server, port=Port, pubIP=PubIP, channelMonitor=ChannelMonitor, whiteDomain=[list_to_binary(S) || S <- string:tokens(WhiteDomain, ",")], maxPerPeriod=MaxPerPeriod, periodSeconds=PeriodSeconds, extra=#port_mgr{init=InitPort,end_port=EndPort,list=[]}}}.

%%--------------------------------------------------------------------
%% Function: handle_info(Info, State) -> {noreply, State} |
%%                                       {noreply, State, Timeout} |
%%                                       {stop, Reason, State}
%% Description: Handling all non call/cast messages
%%--------------------------------------------------------------------
handle_info(#received_packet{packet_type=iq, type_attr=Type, raw_packet=IQ, from=From}, #state{}=State) ->
  	case process_iq(Type, IQ, From, exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)), State) of 
	{_, #state{}=NewState} -> {noreply, NewState};
	_ -> {noreply, State}
	end;

handle_info({_, tcp_closed}, #state{jid=JID, server=Server, pass=Pass, port=Port}=State) ->
  ?INFO_MSG("Connection Closed. Trying to Reconnect...~n", []),
  {_, NewXmppCom} = make_connection(JID, Pass, Server, Port),
  ?INFO_MSG("Reconnected.~n", []),
  {noreply, State#state{xmppCom=NewXmppCom}};

handle_info(stop, #state{xmppCom=XmppCom}=State) ->
  ?INFO_MSG("Component Stopped.~n",[]),
  exmpp_component:stop(XmppCom),
  {noreply, State};

handle_info({active, PID}=S,  #state{channelMonitor=ChannelMonitor}=State) ->
  ChannelMonitor ! S,
	receive
		{active, _}=Active -> PID!Active
	end,
  {noreply, State};

handle_info(Record, State) -> 
  ?INFO_MSG("Unknown Info Request: ~p~n", [Record]),
  {noreply, State}.

%%--------------------------------------------------------------------
%% Function: handle_cast(Msg, State) -> {noreply, State} |
%%                                      {noreply, State, Timeout} |
%%                                      {stop, Reason, State}
%% Description: Handling cast messages
%%--------------------------------------------------------------------
handle_cast(_Msg, State) ->
 ?INFO_MSG("Received: ~p~n", [_Msg]), 
 {noreply, State}.

%%--------------------------------------------------------------------
%% Function: %% handle_call(Request, From, State) -> {reply, Reply, State} |
%%                                      {reply, Reply, State, Timeout} |
%%                                      {noreply, State} |
%%                                      {noreply, State, Timeout} |
%%                                      {stop, Reason, Reply, State} |
%%                                      {stop, Reason, State}
%% Description: Handling call messages
%%--------------------------------------------------------------------
handle_call(Info,_From, _State) ->
 ?INFO_MSG("Received Call: ~p~n", [Info]), 
 {reply, ok, _State}.

%%--------------------------------------------------------------------
%% Function: terminate(Reason, State) -> void()
%% Description: This function is called by a gen_server when it is about to
%% terminate. It should be the opposite of Module:init/1 and do any necessary
%% cleaning up. When it returns, the gen_server terminates with Reason.
%% The return value is ignored.
%%--------------------------------------------------------------------
terminate(_Reason, #state{channelMonitor=ChannelMonitor}) ->
	?INFO_MSG("Terminating Component...", []),
	ChannelMonitor ! stop,
	application:stop(exmpp),
	?INFO_MSG("Terminated Component.", []),
	ok;

terminate(_Reason, _) -> 
	application:stop(exmpp),
        ?INFO_MSG("Forced Terminated Component.", []),
        ok.

%%--------------------------------------------------------------------
%% Func: code_change(OldVsn, State, Extra) -> {ok, NewState}
%% Description: Convert process state when code is changed
%%--------------------------------------------------------------------
code_change(_OldVsn, State, _Extra) ->
  {ok, State}.

%%--------------------------------------------------------------------
%%% Internal functions
%%--------------------------------------------------------------------

init_logger() ->
	p1_loglevel:set(4),
	LogPath = get_log_path(),
	error_logger:add_report_handler(p1_logger_h, LogPath).

%% It first checks for application configuration parameter 'log_path'.
%% If not defined it checks the environment variable LOG_PATH.
%% And if that one is neither defined, returns the default value:
%% "ejabberd.log" in current directory.
get_log_path() ->
    case application:get_env(log_path) of
        {ok, Path} ->
            Path;
        undefined ->
            case os:getenv("LOG_PATH") of
                false ->
                    ?LOG_PATH;
                Path ->
                    Path
            end
    end.

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

%% Create Channel and return details
process_iq("get", IQ, From, ?NS_CHANNEL, #state{xmppCom=XmppCom, pubIP=PubIP, channelMonitor=ChannelMonitor, whiteDomain=WhiteDomain, maxPerPeriod=MaxPerPeriod, periodSeconds=PeriodSeconds, extra=Extra}=State) ->
    Permitted = is_allowed(From, WhiteDomain) andalso mod_monitor:accept(From, MaxPerPeriod, PeriodSeconds),	
	if Permitted == true ->
    		case allocate_relay(ChannelMonitor, From, Extra) of
		{ok, PortA, PortB, NewExtra} ->
			?INFO_MSG("Allocated Port for : ~p~n", [From]),
			Result = exmpp_iq:result(IQ,get_candidate_elem(PubIP, PortA, PortB)),
			exmpp_component:send_packet(XmppCom, Result),
			{ok, State#state{extra=NewExtra}};
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

process_iq("get", IQ, _, ?NS_DISCO_INFO, #state{xmppCom=XmppCom}=State) ->
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

process_iq("get", IQ, _, ?NS_JINGLE_NODES, #state{jid=JID, xmppCom=XmppCom}=State) ->
	Relay = exmpp_xml:element(undefined, 'relay', [exmpp_xml:attribute('policy',"public"), exmpp_xml:attribute('protocol', "udp"), exmpp_xml:attribute('address', JID)], []),
	Services = exmpp_xml:element(?NS_JINGLE_NODES, ?NAME_SERVICES, [],[Relay]),
	Result = exmpp_iq:result(IQ, Services),
	exmpp_component:send_packet(XmppCom, Result),
	{ok, State};

process_iq("get", IQ, _, ?NS_PING, #state{xmppCom=XmppCom}=State) ->
        Result = exmpp_iq:result(IQ),
        exmpp_component:send_packet(XmppCom, Result),
        {ok, State};

process_iq(_, IQ, _, _,  #state{}=State) ->
	?INFO_MSG("Unknown Request: ~p~n", [IQ]),	    
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

get_stats() ->
	get_stats(3, whereis(jn_component)).
get_stats(PID) ->
	timer:sleep(300),
	get_stats(3, PID).
get_stats(0, _) -> -1;
get_stats(N, PID) ->
	PID!{active, self()},
	receive 
		{_, A} -> A
	after 100 -> get_stats(N-1, PID)
	end.

schedule(Period, Relays, Timeout) ->
    receive
        {active, PID} -> 
		Active = length(Relays),
		?INFO_MSG("Active Channels ~p~n", [Active]),
		PID!{active, Active},
		schedule(Period, Relays, Timeout);
	stop ->
                ?INFO_MSG("Stopping Schedule Loop.~n", []);
	NewRelay -> 
		?INFO_MSG("Relay Added: ~p~n", [NewRelay]),
		schedule(Period, [NewRelay | Relays], Timeout)
     after Period ->
	Remain = check_relays(Relays, Timeout),
        schedule(Period, Remain, Timeout)
    end.

cover_test() ->
	init_logger(),
	cover_channels().

cover_channels() -> 
	ChannelMonitor = scheduleChannelPurge(500, [], 4000),
	Max = 20,
	cover_channels(ChannelMonitor, Max, Max , #port_mgr{init=10000, end_port=60000, list=[]}).
cover_channels(ChannelMonitor, 0, Max, _) ->
	timer:sleep(1000),
        Active = get_stats(ChannelMonitor),
        case Active of
                Max ->
                        ?INFO_MSG("Channel Coverage Test Succeed. [~p]~n", [ok]);
                C ->
                        ?ERROR_MSG("Channel Coverage Test Failed!!! Not enough Channels: ~p Open.~n", [C])
        end,
	timer:sleep(5000),
	Remaining = get_stats(ChannelMonitor),	
	case Remaining of
		0 ->
			?INFO_MSG("Channel Coverage Test Succeed. [~p]~n", [ok]),
			ok;
		N ->
			?ERROR_MSG("Channel Coverage Test Failed!!! Remaining ~p Channel to be Closed.~n", [N])
	end;
	
cover_channels(ChannelMonitor, T, Max, State) ->		
	case allocate_relay(ChannelMonitor, "s", State) of
		{ok, _, _, NewState} ->
			cover_channels(ChannelMonitor, T-1, Max, NewState); 
		_ ->
			ChannelMonitor ! stop,
			?ERROR_MSG("Coverage Test Failed!!!~n", [])
	end.

cover_udp_agent(LocalPort, DstHost, DstPort) ->
	case gen_udp:open(LocalPort, ?SOCKOPTS) of
		{ok, Socket} -> start_udp_test(Socket, self(), DstHost, DstPort);
		_ -> {error, port} 
	end.

start_udp_test(Socket, PID, DstHost, DstPort) ->
	spawn(fun () -> burst_traffic(Socket, PID, 50, DstHost, DstPort) end).	

burst_traffic(Socket, PID, 0, _, _) -> 
	PID ! {finish, Socket},
	ok;
burst_traffic(Socket, PID, N, DstHost, DstPort) ->
	case gen_udp:send(Socket, DstHost, DstPort, <<"Relay Test Relay Test Relay Test Relay Test Relay Test Relay Test Relay Test ">>) of
	        ok ->
			timer:sleep(10),
        		burst_traffic(Socket, PID, N-1, DstHost, DstPort);
        	Err ->
            		?ERROR_MSG("unable to send data: ~p", [Err]),
			error
	end.

get(_Key, []) ->
  ?ERROR_MSG("Property Not Found: ~p~n", [_Key]),
  not_found;
get(Key, [{Key, Value} | _Config]) ->
  Value;
get(Key, [{_Other, _Value} | Config]) ->
  get(Key, Config).

