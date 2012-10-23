/**
 * coderguy.js
 */

$(document).ready(function() {
    var dom = {
        $search: $('.content .search'),
        $gitHubSearch: function() { return $('.content .github-search'); },
        $gitHubUsers: $('.content .github-users'),
        $keywords: $('.content .search form input[type=search]'),
        $progress: function() { return $('.content progress'); },
        $submit: $('.content form button'),
        $result: $('.content .result')
    };

    var tmpl = {
        search : {
            github: _.template($("#github-search-tmpl").html())
        },
        result : {
            twitter: _.template($("#twitter-result-tmpl").html()),
            github: _.template($("#github-result-tmpl").html()),
            klout: _.template($("#klout-result-tmpl").html()),
            linkedin: _.template($("#linkedin-result-tmpl").html())
        }
    };

    var renderGitHubUsers = function(gitHubUsers) {
        dom.$gitHubSearch().remove();
        dom.$search.append(tmpl.search.github({
            gitHubUsers: gitHubUsers
        }));
    };

    var renderResult = function(coderGuy) {
        dom.$result.empty();
        dom.$result.append(tmpl.result.linkedin({
            user: coderGuy.linkedInUser
        }));

        dom.$result.append(tmpl.result.github({
            repositories: coderGuy.repositories
        }));

        dom.$result.append(tmpl.result.twitter({
            user: coderGuy.twitterUser,
            timeline: coderGuy.twitterTimeline
        }));

        dom.$result.append(tmpl.result.klout({
            influencers: coderGuy.influencers,
            influencees: coderGuy.influencees
        }));
    };

    var updateProgress = function(onStop) {
        return function(event) {
            dom.$progress().val(event.data);
            if(event.data == 100) {
                onStop();
            }
        };
    };

    var server = new (function() {
        var self = this;
        this.eventSource = null;

        this.search = function(keywords) {
            return $.ajax({
                url: '/search',
                data: $.param({ keywords: keywords }),
                error: function() {
                    console.log('Error while pre-searching the specified coder guy !');
                    self.close();
                }
            });
        };

        this.overview = function(gitHubUser) {
            return $.ajax({
                url: '/overview',
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

    dom.$search.on('keydown', function(e) {
        var isEnterKey = function(key) { return key === 13; };
        if(isEnterKey(e.which)) {
            e.preventDefault();
            var keywords = dom.$keywords.val();
            server.search(keywords).then(renderGitHubUsers);
        }
    });

    dom.$submit.on('click', function(e) {
        e.preventDefault();
        var keywords = dom.$keywords.val();
        server.search(keywords).then(renderGitHubUsers);
    });

    dom.$search.on('click', '.github-search li', function(e) {
        var $gitHubUser = $(e.currentTarget),
            gitHubUser = {
                username: $gitHubUser.find('.username').text(),
                fullname: $gitHubUser.find('.fullname').text(),
                language: $gitHubUser.find('.language').text(),
                followers: $gitHubUser.find('.followers').text()
            };
        server.overview(gitHubUser).then(renderResult);
        server.progress(gitHubUser, updateProgress(server.close));
    });
});