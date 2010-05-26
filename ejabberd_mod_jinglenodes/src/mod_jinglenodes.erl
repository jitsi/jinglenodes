%% @author Johann Prieur <johann.prieur@gmail.com>
%% @author Thiago Camargo <barata7@gmail.com>
%% @doc ejabberd Jingle Nodes module

-module(mod_jinglenodes).

-include_lib("exmpp/include/exmpp.hrl").

-include("ejabberd.hrl").

-behaviour(gen_mod).
-export([start/2, stop/1]).

-export([start_link/2]).

-behaviour(gen_server).
-export([init/1,
		handle_call/3,
		handle_cast/2,
		handle_info/2,
		terminate/2,
		code_change/3]).

-record(state, {server_host, host, public_ip, relay_monitor, id = 0, port = 10000, relay_timeout}).
-record(relay, {id, pid, jid, local_port, remote_port, expire}).

-define(NS_JINGLE_NODES, 'http://jabber.org/protocol/jinglenodes').
-define(NS_JINGLE_NODES_CHANNEL, 'http://jabber.org/protocol/jinglenodes#channel').


start(Host, Options) ->
	Proc = gen_mod:get_module_proc(Host, ?MODULE),
	ChildSpec = {Proc,
			{?MODULE, start_link, [Host, Options]},
			transient, 1000, worker, [?MODULE]},
	supervisor:start_child(ejabberd_sup, ChildSpec).

stop(Host) ->
	Proc = gen_mod:get_module_proc(Host, ?MODULE),
	gen_server:call(Proc, stop),
	supervisor:delete_child(ejabberd_sup, Proc).

start_link(Host, Options) ->
	Proc = gen_mod:get_module_proc(Host, ?MODULE),
	gen_server:start_link({local, Proc}, ?MODULE, [Host, Options], []).


init([ServerHost, Options]) ->
	Host = gen_mod:get_opt_host(ServerHost, Options, "jinglenodes.@HOST@"),
	PublicIP = gen_mod:get_opt(public_ip, Options, undefined),
	PurgePeriod = gen_mod:get_opt(purge_period, Options, 10000),
	RelayTimeout = gen_mod:get_opt(relay_timeout, Options, 60000),
	RelayMonitor = start_relay_monitoring(PurgePeriod),
	ejabberd_router:register_route(Host),
	State = #state{server_host = ServerHost,
			host = Host,
			public_ip = PublicIP,
			relay_monitor = RelayMonitor,
			relay_timeout = RelayTimeout},
	{ok, State}.

handle_call(_Command, _From, State) ->
	{noreply, State}.

handle_cast(_Message, State) ->
	{noreply, State}.

handle_info({route, From, To, Packet}, State) when ?IS_IQ(Packet) ->
	IQ = exmpp_iq:xmlel_to_iq(Packet),
	Namespace = exmpp_xml:get_ns_as_atom(exmpp_iq:get_payload(IQ)),
	{Reply, NewState} = handle_iq(From, Namespace, IQ, State),
	ejabberd_router:route(To, From, exmpp_iq:iq_to_xmlel(Reply)),
	{noreply, NewState};
handle_info(_Info, State) ->
	{noreply, State}.

terminate(_Reason, #state{host = Host}) ->
	ejabberd_router:unregister_route(Host).

code_change(_OldVsn, State, _Extra) ->
	{ok, State}.

handle_iq(_Sender, ?NS_DISCO_INFO, IQ, State) ->
	Payload = ?XMLEL4(?NS_DISCO_INFO, 'query', [], [
			?XMLEL4(?NS_DISCO_INFO, 'identity', [
					?XMLATTR('category', <<"proxy">>),
					?XMLATTR('type', <<"relay">>),
					?XMLATTR('name', <<"Jingle Nodes Relay">>)], []),
			?XMLEL4(?NS_DISCO_INFO, 'feature', [?XMLATTR('var', ?NS_DISCO_INFO)], []),
			?XMLEL4(?NS_DISCO_INFO, 'feature', [?XMLATTR('var', ?NS_JINGLE_NODES)], []),
			?XMLEL4(?NS_DISCO_INFO, 'feature', [?XMLATTR('var', ?NS_JINGLE_NODES_CHANNEL)], [])
		]),
	{exmpp_iq:result(IQ, Payload), State};
handle_iq(_Sender, ?NS_DISCO_ITEMS, IQ, State) ->
	{exmpp_iq:result(IQ, ?XMLEL2(?NS_DISCO_ITEMS, 'query')), State};
handle_iq(_Sender, ?NS_JINGLE_NODES, IQ, State) ->
	Payload = ?XMLEL4(?NS_DISCO_INFO, 'services', [], [
			?XMLEL4(undefined, 'relay', [
					?XMLATTR('policy', <<"public">>),
					?XMLATTR('address', State#state.host),
					?XMLATTR('protocol', <<"udp">>)
				], [])
		]),
	{exmpp_iq:result(IQ, Payload), State};
handle_iq(Sender, ?NS_JINGLE_NODES_CHANNEL, IQ, State) ->
	case allocate_relay(Sender, State) of
		{Relay, NewState} ->
			Payload = ?XMLEL4(?NS_JINGLE_NODES_CHANNEL, 'channel', [
					?XMLATTR("id", Relay#relay.id),
					?XMLATTR("host", State#state.public_ip),
					?XMLATTR("localport", Relay#relay.local_port),
					?XMLATTR("remoteport", Relay#relay.remote_port),
					?XMLATTR("expire", Relay#relay.expire)
			], []),
			{exmpp_iq:result(IQ, Payload), NewState};
		_ ->
			{exmpp_iq:error(IQ, 'service-unavailable'), State}
	end;
handle_iq(_Sender, _Namespace, IQ, State) ->
	{exmpp_iq:error(IQ, 'service-unavailable'), State}.

allocate_relay(Jid, State) ->
	Id = State#state.id,
	LocalPort = State#state.port,
	RemotePort = LocalPort + 2,
	case udp_relay:start(LocalPort, RemotePort) of
		{ok, RelayPid} ->
			Relay = #relay{id = Id,
				pid = RelayPid,
				jid = Jid,
				local_port = LocalPort,
				remote_port = RemotePort,
				expire = State#state.relay_timeout},
			State#state.relay_monitor ! Relay,
			NewState = State#state{id = Id + 1, port = RemotePort + 2},
			{Relay, NewState};
		_ ->
			{error, State}
	end.

start_relay_monitoring(Period) ->
	spawn_link(fun() -> relay_monitor(Period, []) end).

relay_monitor(Period, Relays) ->
	receive
		NewRelay ->
			?INFO_MSG("Adding new relay: ~p~n", [NewRelay]),
			relay_monitor(Period, [NewRelay | Relays])
	after
		Period ->
			?DEBUG("Processing to relay purge: ~p~n", [Relays]),
			RemainingRelays = lists:filter(fun purge_relay/1, Relays),
			relay_monitor(Period, RemainingRelays)
	end.

purge_relay(#relay{pid = Pid, expire = Expire} = Relay) ->
	?DEBUG("Checking relay status: ~p~n", [Relay]),
	LastActivityTimestamp = gen_server:call(Pid, get_timestamp),
	Delta = timer:now_diff(now(), LastActivityTimestamp) / 1000,
	if Delta > Expire ->
		?INFO_MSG("Relay expired, removing: ~p~n", [Relay]),
		exit(Pid, kill),
		false;
	true ->
		true
	end.

