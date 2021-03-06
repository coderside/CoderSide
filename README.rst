########
CoderSide
########

- Don't you ever ask google for having more information about one coder guy ?
- From what social networks does google found the most valuable information ?

The Idea
========

Create a Web Application that gives a full overview of one coder guy.
A "full overview" means try to understand his programmer life.

How does it work ?
==================

This application is a mashup that takes data from several others web apps :

- GitHub : repositories, followers, forks, contributions.
- Twitter : description, followers, timeline.
- Klout : score, influencers, influencees and their Twitter/Klout account.

CoderSide assume that the searched coder guy must have a GitHub account. This is the entry point of the application.

Process
-------

The process of the application is quite short & simple :

1. Search on GitHub the coder guy by his fullname (or others criteria like his pseudo).
2. The search can return more than one GitHub account. You have to select the good one.
3. Once selected, the searching process is launch. This can take some seconds to return a result.

Technical solution
==================

The idea is to use the strengths of the Typesafe Stack in order to :
 - be asynchronous everywhere.
 - have a reactive (realtime) web application.
 - focus simplicity.

Be asynchronous
---------------

| As we saw previously, the searching process makes a lot of requests and can take a long time.
| To make asynchronous this process, I had two choices :

 - | Chain all the requests at the same place and build the result directly with all the promises returns by the WS API.
   | The code would be a huge block hard to deal with it.
 - Use actors (Akka) to organize the WS calls and build the final result.

Of course, I select the last one. We will talking about this later.

Be realtime
-----------

| Be realtime means give to user realtime information without reloading the web page.
| CorderGuy provides a realtime updated progress bar displayed when a user launch a search.

There are several web technologies to do realtime:
 - Long pooling (Comet)
 - Server Send Event (Event Stream)
 - Websocket

| I don't need websocket in my case. Websocket is bidirectional (moreover, we can be annoyed by proxy).
| Long pooling is unidirectional, works with old browsers but not efficient at all.
| Server Sent Event is unidirectional, works with modern browsers, and efficient.

I finally decided to use Server Sent Event technology.

NB : To keep the compatibilities with old browsers, the best solution would be to have a fallback to Long pooling when SSE isn't supported.

Scatter-Gatherer design
-----------------------

Here how I organize the searching process through the actors :

.. image:: /CoderSide/raw/master/readme/scatter-gather-small.png

For the next part, I will describe each actor.

SupervisorNode
^^^^^^^^^^^^^^

| The SupervisorNode have the role to create (at the start) the HeadNode.
| SupervisorNode receives requests from clients and redirects them to the HeadNode.
| In the event of failure of the HeadNode, it's automatically restarted.

HeadNode
^^^^^^^^

| The HeaderNode have the role to create (at the start) the GitHubNode, TwitterNode & KloutNode.
| HeadNode handles two types of requests come from the SupervisorNode :

 - Launch the searching process.
 - Ask for a stream to have realtime information about the progression of the search.

| When it receives a request for launching a search, it creates an instance of GathererNode, then broadcasts it to GitHubNode and TwitterNode.
| After that, it waits for a message to stop the GathererNode.
| In the event of failure of one search node (GiHubNode, TwitterNode, KloutNode), it's automatically restarted.

GitHubNode
^^^^^^^^^^

| GitHubNode plays with GitHub API to retrieve the profile & the repositories of the searched guy.
| Once done, it sends the result to GathererNode.

TwitterNode
^^^^^^^^^^^

| TwitterNode plays with Twitter API to retrieve the twitter account & timeline of the searched guy.
| We can't search a user on Twitter with an identifier like address email.
| The only choice is to use the fullname of the GitHub account to query Twitter.
| But the fullname isn't a required value when you create a account GitHub.
| It's possible that querying Twitter with fullname returns nothing.
| In this case, the last chance is to query Twitter with the GitHub username.
| Once we have searched with fullname or username, Twitter give us a list of Twitter accounts that could match.
| The challange was to select the good one. The idea is to use some information from GitHub account to optimize the result.
| After finding the best matched Twitter account, It's easy to get back the Twitter timeline.
| Finally, the Twitter account and its timeline are sent to the GathererNode.
| The found Twitter account is also sent to the KloutNode.

KloutNode
^^^^^^^^^

| KlouNode is the only node that doesn't receive directly the request from the HeadNode but TwitterNode (blue arrow).
| This actor requires that the TwitterNode found the Twitter account of the searched guy to perform.
| If it doesn't found, this actor become useless and isn't used.
| The KloutNode uses the Twitter account to get back influencers/influencees data from the Klout API.
| Once all influencers/influencees are retrieved, the KloudNode queries their associated Twitter account.
| Finally, it sends to GathererNode the Twitter account of each influencer/influcencee along with its Klout score.

GathererNode
^^^^^^^^^^^^

| GathererNode is the only actor that is recreated for each request (by the HeadNode).
| It's role is to gather all the results come from GitHubNode, TwitterNode & KloutNode.
| While building the final result, it sends through the stream (grey arrow from GathererNode to Client) the current progress of the searching process.
| Once all results have been gathered, it sends the final result to clients and closes the stream.
| In the case where the GathererNode doesn't receive all the result within a duration, it cancels the search and asks the HeadNode to stop it.

Optimization
------------

| In the case where several users make the same search in the same period time, the searching process is launched only once.
| All the users subscribe to the same result and share the same stream (progress bar).
| To do that, a state of the current requests is kept in the HeadNode actor.
| Once the GathererNode finishes to build the result, it asks the HeadNode to remove its request from the current state.

The purpose of this "feature" is to save the number of requests against the Twitter API.

NB : To test it with a single computer, you need to use two different browsers.

Drawbacks
---------

| CoderSide doesn't work like we would want in a clustered environment.
| Why ?

| There are two main issues :

 - | The first is about the optimization we talk previously.
   | Each node have his own state of the current searchs.
   | There will be no optimization is one search is made on the node N1 and the second one is made on the node N2.


 - | The second issue is about the stream.
   | If one client get a stream from a node N1, and then this node goes down.
   | The client will be disconnected from the node N1 and will have a new one from the node N2.
   | But the node N2 doesn't know what data to send to the client.

To resolve those two concerns, we could centralize the data through a database.

Licence
=======

This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
