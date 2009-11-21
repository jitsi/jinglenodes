%%%-------------------------------------------------------------------
%%% File    : udp_relay.erl
%%% Author  : Evgeniy Khramtsov <ekhramtsov@process-one.net>
%%% Description : Simple UDP relay
%%%
%%% Created : 29 Oct 2009 by Evgeniy Khramtsov <ekhramtsov@process-one.net>
%%%-------------------------------------------------------------------
-module(udp_relay).

-behaviour(gen_server).

%% API
-export([start/2, start_link/2]).

%% gen_server callbacks
-export([init/1, handle_call/3, handle_cast/2, handle_info/2,
	 terminate/2, code_change/3]).

-define(ERROR_MSG(Format, Args),
	error_logger:error_msg("(~p:~p:~p) " ++ Format ++ "~n",
			       [self(), ?MODULE, ?LINE | Args])).

-define(INFO_MSG(Format, Args),
	error_logger:info_msg("(~p:~p:~p) " ++ Format ++ "~n",
			       [self(), ?MODULE, ?LINE | Args])).

-record(state, {sock1, sock2, last_recv1, last_recv2, lastTimestamp}).

-define(SOCKOPTS, [binary, {active, once}]).

%%====================================================================
%% API
%%====================================================================
start_link(P1, P2) ->
    gen_server:start_link(?MODULE, [P1, P2], []).

start(P1, P2) ->
    gen_server:start(?MODULE, [P1, P2], []).

%%====================================================================
%% gen_server callbacks
%%====================================================================
init([Port1, Port2]) ->
    case {gen_udp:open(Port1, ?SOCKOPTS),
	  gen_udp:open(Port2, ?SOCKOPTS)} of
	{{ok, Sock1}, {ok, Sock2}} ->
	    ?INFO_MSG("relay started at ~p and ~p", [Port1, Port2]),
	    {ok, #state{sock1 = Sock1, sock2 = Sock2}};
	Errs ->
	    ?ERROR_MSG("unable to open port: ~p", [Errs]),
	    {stop, Errs}
    end.

handle_call(_Request, _From, State) ->
    {reply, ok, State}.

handle_cast(_Msg, State) ->
    {noreply, State}.

handle_info({udp, Sock, SrcIP, SrcPort, Data},
	    #state{sock1 = Sock} = State) ->
    inet:setopts(Sock, [{active, once}]),
    case State#state.last_recv2 of
	{DstIP, DstPort} ->
	    send(State#state.sock2, DstIP, DstPort, Data);
	_ ->
	    ok
    end,
    {noreply, State#state{last_recv1 = {SrcIP, SrcPort}, lastTimestamp= now()}};

handle_info({udp, Sock, SrcIP, SrcPort, Data},
	    #state{sock2 = Sock} = State) ->
    inet:setopts(Sock, [{active, once}]),
    case State#state.last_recv1 of
	{DstIP, DstPort} ->
	    send(State#state.sock1, DstIP, DstPort, Data);
	_ ->
	    ok
    end,
    {noreply, State#state{last_recv2 = {SrcIP, SrcPort}, lastTimestamp= now()}};
handle_info(_Info, State) ->
    {noreply, State}.

terminate(_Reason, _State) ->
    ok.

code_change(_OldVsn, State, _Extra) ->
    {ok, State}.

%%--------------------------------------------------------------------
%%% Internal functions
%%--------------------------------------------------------------------
send(Sock, Addr, Port, Data) ->
    case gen_udp:send(Sock, Addr, Port, Data) of
	ok ->
	    ok;
	Err ->
	    ?ERROR_MSG("unable to send data: ~p", [Err]),
	    exit(normal)
    end.
