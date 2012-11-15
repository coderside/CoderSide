============
CoderGuy
============

The Needs
=============

- Don't you ever ask google for having more information about one coder guy ?
- From what social networks does google found the most valuable information ?

The Idea
=============

Create a Web Application that give to users a full overview of one coder guy in only two clicks.

How does it work ?
==================

This application is a mashup that takes data from several others web apps:

- LinkedIn : Current job (headline)
- GitHub : Repositories, number of followers
- Twitter : Description, number of followers, timeline
- Klout : score, influencers, influencees and Twitter accounts of each influencers/influencees.

| CoderGuy assume that the searched coder guy must have a GitHub account.
| This is the entry point of the application.

| The challenge was to find the best way to match accounts between them.
| CoderGuy doesn't always guarantee a fully valid result.

Process
-------------------

The process of the application is quite short & simple :

1. Search on GitHub the coder guy by his full name.
2. The search can return more than one GitHub user. You have to select the good one.
3. | Once selected, the searching process is launch.
   | This can take some seconds to return a result.
   | There is a lot of requests.

Technical solution
------------------

The idea is to use the strengths of the typesafe stack in order to :
 - be asynchronous everywhere.
 - have a reactive (realtime) web application.

Be asynchronous
```````````````

| As we saw previously, the searching process make a lot of requests and can take a long time.
| To make asynchronous this process, I had two choices :

 - Chain all the requests at the same place and build the result directly with all the promises returns by the WS API. The code would be a huge block hard to deal with it.
 - Use actors (Akka) to organize the WS calls and build the final result.

Off course, I select the second one.
Here how I design the actors model :


Be realtime
```````````

| Be realtime means give to user realtime information without reloading the web page.
| CorderGuy provide a realtime updated progress bar displayed when a user launch a search.

There are several web technologies to do realtime:
 - Long pooling (Comet)
 - Server Send Event (Event Stream)
 - Websocket

| I don't need websocket in my case. Websocket is bidirectional.
| Long pooling is unidirectional, works with old browser but not efficient at all.
| Server Sent Event is unidirectional, works with modern browsers, and efficient.

I finally decided to use Server Sent Event technology.

NB : To keep the compatibilities with old browsers, the best solution would be to have a fallback to Long pooling when SSE isn't supported.

Optimization
````````````
In the case where several users make the same search at the same time,
the app launchs only one searching process but all the users are subscribed to the response & the realtime progress stream.

Drawbacks
`````````
The optimization doesn't fully works in a clustered environnement.