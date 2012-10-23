/**
 * coderguy.js
 */

$(document).ready(function() {
    var dom = {
        $preSearch: $('.content form input[type=search]'),
        $gitHubUsers: $('.content .githubusers'),
        $progress: $('.content form progress'),
        $submit: $('.content form button'),
        $result: $('.content .result')
    };

    var tmpl = {
        preSearch : {
            github: _.template($("#github_presearch_tmpl").html())
        },
        search : {
            twitter: _.template($("#twitter_search_tmpl").html()),
            github: _.template($("#github_search_tmpl").html()),
            klout: _.template($("#klout_search_tmpl").html()),
            linkedin: _.template($("#linkedin_search_tmpl").html())
        }
    };

    var renderGitHubUsers = function(gitHubUsers) {
        dom.$gitHubUsers.append(tmpl.preSearch.github({
            gitHubUsers: gitHubUsers
        }));
    };

    var renderResult = function(coderGuy) {
        dom.$result.append(tmpl.search.linkedin({
            user: coderGuy.linkedInUser
        }));

        dom.$result.append(tmpl.search.github({
            repositories: coderGuy.repositories
        }));

        dom.$result.append(tmpl.search.twitter({
            user: coderGuy.twitterUser,
            timeline: coderGuy.twitterTimeline
        }));

        dom.$result.append(tmpl.search.klout({
            influencers: coderGuy.influencers,
            influencees: coderGuy.influencees
        }));
    };

    var updateProgress = function(onStop) {
        return function(event) {
            dom.$progress.val(event.data);
            if(event.data == 100) {
                onStop();
            }
        };
    };

    var server = new (function() {
        var self = this;
        this.eventSource = null;

        this.preSearch = function(keywords) {
            return $.ajax({
                url: '/preSearch',
                data: $.param({ keywords: keywords }),
                error: function() {
                    console.log('Error while pre-searching the specified coder guy !');
                    self.close();
                }
            });
        };

        this.search = function(gitHubUser) {
            return $.ajax({
                url: '/search',
                data: $.param(gitHubUser),
                error: function() {
                    console.log('Error while searching the specified coder guy !');
                    self.close();
                }
            });
        };

        this.progress = function(gitHubUser, onReceived) {
            var uri = '/progress?' + $.param(gitHubUser);
            self.eventSource = new EventSource(uri);
            self.eventSource.onmessage = onReceived;
            self.eventSource.onerror = function() {
                console.log('Error while getting progress update');
                self.close();
            };
        };

        this.close = function() {
            self.eventSource.close();
        };
    })();

    dom.$preSearch.on('keydown', function(e) {
        var isEnterKey = function(key) { return key === 13; };
        if(isEnterKey(e.which)) {
            e.preventDefault();
            var keywords = dom.$preSearch.val();
            server.preSearch(keywords).then(renderGitHubUsers);
        }
    });

    dom.$gitHubUsers.on('click', 'li', function(e) {
        var $gitHubUser = $(e.currentTarget),
            gitHubUser = {
                username: $gitHubUser.find('.username').text(),
                fullname: $gitHubUser.find('.fullname').text(),
                language: $gitHubUser.find('.language').text(),
                followers: $gitHubUser.find('.followers').text()
            };
        server.search(gitHubUser).then(renderResult);
        server.progress(gitHubUser, updateProgress(server.close));
    });

    dom.$submit.on('click', function(e) {
        e.preventDefault();
        var keywords = dom.$preSearch.val();
        server.preSearch(keywords).then(renderGitHubUsers);
    });
});