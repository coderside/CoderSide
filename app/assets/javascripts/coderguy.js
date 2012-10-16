/**
 * coderguy.js
 */

$(document).ready(function() {
    var dom = {
        $search: $('.content form input[type=search]'),
        $progress: $('.content form progress'),
        $submit: $('.content form button'),
        $result: $('.content .result')
    };

    var tmpl = {
        twitter: _.template($("#twitter_tmpl").html()),
        github: _.template($("#github_tmpl").html()),
        klout: _.template($("#klout_tmpl").html()),
        linkedin: _.template($("#linkedin_tmpl").html())
    };

    var renderResult = function(coderGuy) {
        dom.$result.append(tmpl.linkedin({
            user: coderGuy.linkedInUser
        }));

        dom.$result.append(tmpl.github({
            repositories: coderGuy.repositories
        }));

        dom.$result.append(tmpl.twitter({
            user: coderGuy.twitterUser,
            timeline: coderGuy.twitterTimeline
        }));

        dom.$result.append(tmpl.klout({
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

        this.search = function(success) {
            var keywords = dom.$search.val();
            return $.ajax({
                url: '/search',
                data: $.param({ keywords: keywords }),
                success: success,
                error: function() {
                    console.log('Error while searching the specified coder guy !');
                    self.close();
                }
            });
        };

        this.progress = function(onReceived) {
            var keywords = dom.$search.val(),
                uri = '/progress?' + $.param({ keywords: keywords });

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
            server.search().then(renderResult);
            server.progress(updateProgress(server.close));
        }
    });

    dom.$submit.on('click', function(e) {
        e.preventDefault();
        server.search().then(renderResult);
    });
});