# Introduction #

This Tutorial provides a step-by-step how to add Jingle Nodes Services to your XMPP Server using Jingle Nodes erlang version of the Services API

# Requirements #

**Latest Erlang Version Installed** XMPP Server with External Component Enabled (ejabberd recommended for erlang version)

# Procedures #

### 1) Configuring ejabberd ###

Append your ejabberd.cfg file:

```
  %%
  %% ejabberd_service: Interact with external components (transports...)
  %%
  {8888, ejabberd_service, [
                            {access, all},
                            {shaper_rule, fast},
                            {ip, {127, 0, 0, 1}},
                            {hosts, ["jn.localhost"],
                             [{password, "secret"}]
                            }
                           ]},
```

### 2) Install exmpp ###

git clone https://github.com/processone/exmpp.git

Run:
```
autoconf

autoreconf -vif

./configure

make install
```

### 3) Checking Out Jingle Services API Code ###

# Non-members may check out a read-only working copy anonymously over HTTP.
```
svn checkout http://jinglenodes.googlecode.com/svn/trunk/jnsapi_erlang jnsapi_erlang 
```

Extra details and SVN Source Browse: Jingle Nodes Source

### 4) Building ###
```
~/jnsapi_erlang$ make
```
### 5) Configuring: jn\_component.cfg ###
```
{jid, "jn.localhost"}.
{server, "localhost"}.
{port, 8888}.
{pass,"secret"}.
{public_ip, "89.100.102.102"}.
{whitelist, "localhost"}.
{channel_timeout,60000}.
{max_per_period, 10}.
{period_seconds, 6}.
{init_port, 20000}.
{end_port, 50000}.
{handler, jingle_handler}.
```
### 6) Running ###

```
./start.sh
```