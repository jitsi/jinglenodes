In the creation of the very first version of the Jingle Protocol, one of the main goals was to achieve a P2P enable protocol, that would depend on XMPP for routing, but would be also able to negotiate sessions and exchange content without main proxy servers like present SIP deployments.

After 5 years we still don't have any massive deployments containing and fully supported the current specifications, and even the closer to the specification ones are suffering when P2P is not possible and relay is required and there is no available ones.

SIP in the other hand is not very efficient and simple for P2P methods but is widely deployed around the planet as it is much simpler to deploy and although with higher costs, can provide media connectivity.

Jingle Nodes comes in place with the goal of making more nodes available, as it makes the task of having public relays close to trivial, as you just need to run an XMPP client with a Public Node Policy available for everyone. Also makes every buddy in your contact list a potential Node.
Another positive point is that a client don't need to implement a Relay Node if not applicable, it only needs to implement the Usage specification that would be no more than two or three pages of specification as the main logic is on server side.

## A few words about Jingle Nodes ##
Jingle Nodes and Super Nodes are intend to provide easy to use Jingle Relay Type Candidates that can be used in ICE-UDP and also on RAW-UDP Jingle Sessions.
Relay Candidates can provide NAT Traversal for users that don't have STUN/TURN Support, but also for users with STUN/TURN support that the negotiation failed.

## What about similar architecture and approach? ##
The unique similar project is Skype, which is a closed and non inter-operable network.
Skype Network works in a similar way except that on Skype you can't choose whether share your bandwidth or not. And as a closed protocol you can't do anything about it.
On Jingle you can choose to share with:
**everyone** nobody
**only buddies** only whitelist
**blacklist** etc...
_In other words you are free to choose and decide about your device and network._

Another similar technologies but with less coverage is STUN/TURN.
STUN is very nicely designed technology, and it is responsible for almost all P2P VoIP traffic available nowadays. The idea is very efficient and assertive.
I also like TURN security and assertion, but it doesn't seem like the market already adopted it very widely to serve as the default VoIP relay system. But both lacks badly in simplicity and implementation timeframe.
TURN RFC is something about 80 pages, which makes it very complex and unpractical for "thin" clients like mobile voip clients, where vendors needs to release a version for almost each new mobile phone model.
Jingle Relay Nodes uses the benefit of the embedded security and flexibility of XMPP to provide the same functionality of Relay, but in a much simpler process and minimal implementation.
So we can finally have XMPP Clients doing VoIP with a pure XMPP Stack Client.

## What is it that people really need? ##
People needs to talk. End users don't necessarily care about which protocol or connection type is being used in the call, but if it effectively has audio and with reasonable quality.
The Jingle Nodes try to promote the beauty of P2P, but without dropping the Jingle Responsibility with the end user, which is call completion.

## What does Jingle have and what needs to be done? ##
Jingle already have specifications for Direct Media, P2P based on ICE-STUN, but it needs an extension to cover Jingle Relay Nodes.
The long term goal is to have Jingle widely deployed across the world and collaborate to this adoption by providing "easy to use" and "always works" solutions.