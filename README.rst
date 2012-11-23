============
CoderGuy
============

The Needs
=============

- Don't you ever ask google for having more information about one coder guy ?
- From what social networks does google found the most valuable information ?

The Idea
=============

Create a Web Application that give a full overview of one coder guy.
A "full overview" means try to understand his programmer life.

How does it work ?
==================

This application is a mashup that takes data from several others web apps:

- LinkedIn : Current job (headline)
- GitHub : repositories (name, forks), number of followers
- Twitter : Description, number of followers, timeline
- Klout : score, influencers, influencees and Twitter/Klout accounts of each influencers/influencees.

CoderGuy assume that the searched coder guy must have a GitHub account. This is the entry point of the application.


| Fetching the Twitter account of each influencers/influences for each search makes a lot of requests to Twitter API.
| Twitter limits the application to 150 requests per hour.

Process
-------

The process of the application is quite short & simple :

1. Search on GitHub the coder guy by his full name (or others criteria like his pseudo).
2. The search can return more than one GitHub user. You have to select the good one.
3. Once selected, the searching process is launch. This can take some seconds to return a result (there is a lot of requests).

Technical solution
------------------

The idea is to use the strengths of the typesafe stack in order to :
 - be asynchronous everywhere.
 - have a reactive (realtime) web application.

Be asynchronous
```````````````

| As we saw previously, the searching process make a lot of requests and can take a long time.
| To make asynchronous this process, I had two choices :

 - | Chain all the requests at the same place and build the result directly with all the promises returns by the WS API.
   | The code would be a huge block hard to deal with it.
 - Use actors (Akka) to organize the WS calls and build the final result.

| Of course, I select the last one. We will talk about this later.

Be realtime
```````````

| Be realtime means give to user realtime information without reloading the web page.
| CorderGuy provides a realtime updated progress bar displayed when a user launch a search.

There are several web technologies to do realtime:
 - Long pooling (Comet)
 - Server Send Event (Event Stream)
 - Websocket

| I don't need websocket in my case. Websocket is bidirectional (moreover, we can't be annoyed by proxy).
| Long pooling is unidirectional, works with old browsers but not efficient at all.
| Server Sent Event is unidirectional, works with modern browsers, and efficient.

I finally decided to use Server Sent Event technology.

NB : To keep the compatibilities with old browsers, the best solution would be to have a fallback to Long pooling when SSE isn't supported.

Scatter-Gatherer Actor Design
`````````````````````````````
image:: reader/scatter-gather.png

SupervisorNode
``````````````

| The SupervisorNode have the role to create (at the start) the HeadNode.
| It receives requests from clients and redirects them to the HeadNode.

HeadNode
````````

HeadNode handles two types of requests come from the SupervisorNode :
 - Launch the searching process.
 - Ask for a stream to have realtime information about the progression of the search.

| When it receives a request for launching the search, it creates an instance of GathererNode and broadcasts the request to GitHubNode, LinkedInNode and TwitterNode.
| After that, it waits for a message from GathererNode to stop the GathererNode.

GitHubNode
``````````

| GitHubNode plays with GitHub API to retrieve the repositories of the searched guy.
| Once done, it sends the result to GathererNode.

LinkedInNode
````````````

| LinkedInNode plays with LinkedIn API to retrieve some extra data like the avatar & the headline of the searched guy.
| Once done, it sends the result to GathererNode.

TwitterNode
```````````

| TwitterNode plays with Twitter API to retrieve the twitter account & timeline of the searched guy.
| Once done, it sends the result to the GathererNode.

KloutNode
`````````

| KlouNode is the only node that not receive directly the request form the HeadNode.
| This actor is very tight with the TwitterNode.
| If the TwitterNode doesn't found the twitter account of the searched guy, this actor isn't used in the searching process.

GathererNode
````````````

| GathererNode is the only actor that is recreated for each request (by the HeadNode).
| It's role is to gather all the results come from GitHubNode, LinkedInNode, TwitterNode & KloutNode.
| It sends to the stream the current progress of the searching process.
| Once all results have been gathered, it sends the final result to client.

Optimization
````````````
In the case where several users make the same search in the same period time, the searching process is launched only once.
All the users subscribe to the same result and have the same stream (progress bar).
A state of the current requests is kept in the HeadNode actor.
When the gatherer node finish to build the result, it asks the head node to remove the request.


Drawbacks
`````````
| CoderGuy does'nt work like we would want in a clustered environnment.
| Why ?

First, the optimization I talk previously does'nt fully work properly :

Each node have his own state of the current searchs. To have a complete optimization, we have two choises :

 - Decentralized synchronization of the state.
 - Centralized synchronization of the state.

Second , each node manage its own streams. If one node goes down, the client will lost totally his stream.
The second node don't have any data of the first dead node.
