## Scope of the API ##

The services API aims to provide the relay functionality, it has the goal of providing a generic RTP Relay Library.
That can be use in an application to provide:

  1. Dynamic RTP/UDP Relay Channels
  1. Basic Permissions Rules based on domain whitelist
  1. Basic Permissions Rules based on Max Concurrent Channels per User(BareJID);

## Auto of Scope ##

As a Prototype Services API does NOT intend to provide:
  1. Provide XMPP Connectivity, as it is meant to be generic as reusable for different XMPP Clients and Servers.
  1. Blacklist Permission Rules
  1. RTP/TCP Transport Relay

## User Interaction ##

Application can use the Library for the following tasks:
  1. Request a Dynamic Bind Relay Channel - Dynamic as the Channel has a mechanism that does not require User/Application Interaction in order to bind the Channel both ways.
  1. Request Destruction of a Channel
  1. Select the Domain Whitelist that can request Channels
  1. Select the Max Concurrent Channels per User(BareJID)

## Services Activities ##

The Services API has Internal Activities and Responsibilities:
  1. Timeout, Expire and Destroy Inactive Channels (Inactivity is based on last packet received by the Channel)

## Flow Diagram ##

Basically the Services API has 2 Main Processes:
  1. **Channel Timeout Monitor** - That monitors channels activity
  1. **Candidate Processor** - That process Candidate(Channel) Requests and allocates channels.

**The Timeout Monitor** has the task of monitoring channels activity and destroy expired ones. In order to prevent that no longer used channel remains valid. This situation can happen when a client requests a node, but looses internet connectivity, making the sending of the destroy request impossible.

**The Candidate Processor** process Candidate(Channel) Creation and Destroy Requests. It will allocate and destroy active channel on demand. It is also responsible for controlling whitelists, blacklists and other available security policies.