/**
 * coderguy.js
 */

$(document).ready(function() {
    var dom = {
        $back : $('.back'),
        $next : $('.next'),
        $step1 : $('#step-1'),
        $step2 : $('#step-2'),
        $step3 : $('#step-3'),
        $content: $('#content'),
        $keywords: function() { return $('#content #step-1 .github-search input[type=search]'); },
        $progress: function() { return $('#content #step-2 .github-users .progress'); },
        resizeContent: function($container) {
            return function() {
                $('#content').height($container.height());
            };
        }
    };

    var tmpl = {
        gitHubSearch: _.template($("#github-search-tmpl").html()),
        gitHubUsers: _.template($("#github-users-tmpl").html()),
        result: {
            twitter: _.template($("#twitter-result-tmpl").html()),
            github: _.template($("#github-result-tmpl").html()),
            klout: _.template($("#klout-result-tmpl").html()),
            linkedin: _.template($("#linkedin-result-tmpl").html())
        }
    };

    /**
     * Spinner
     */
    var Loader = function(target) {
        var $target = $(target);
        var spinner = new Spinner({
            lines: 9,
            length: 7,
            width: 3,
            radius: 10,
            corners: 1,
            rotate: 0,
            color: '#000',
            speed: 1,
            trail: 60,
            shadow: false,
            hwaccel: false,
            className: 'spinner',
            zIndex: 2e9,
            top: 'auto',
            left: 'auto'
        });

        this.show = function() {
            spinner.spin($target[0]);
        };

        this.hide = function() {
            $target.find('.spinner').remove();
        };
    };

    /**
     * Views
     */

    var renderGitHubSearch = function() {
        dom.$step1.html(tmpl.gitHubSearch({}));
    };

    var renderGitHubUsers = function(gitHubUsers) {
        dom.$step2.html(tmpl.gitHubUsers({
            gitHubUsers: gitHubUsers
        }));
    };

    var renderResult = function(coderGuy) {
        dom.$step3.empty();
        dom.$step3.append(tmpl.result.linkedin({
            user: coderGuy.linkedInUser
        }));

        dom.$step3.append(tmpl.result.github({
            repositories: coderGuy.repositories
        }));

        dom.$step3.append(tmpl.result.twitter({
            user: coderGuy.twitterUser,
            timeline: coderGuy.twitterTimeline
        }));

        dom.$step3.append(tmpl.result.klout({
            user: coderGuy.kloutUser,
            influencers: coderGuy.influencers,
            influencees: coderGuy.influencees
        }));
    };

    var updateProgress = function(onStop) {
        return function(event) {
            dom.$progress().css('width', event.data + '%');
            if(event.data == 100) {
                onStop();
            }
        };
    };

    /**
     * Server requests.
     */
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

    /**
     * DOM bindings.
     */
    dom.$content.on('keydown', '#step-1 .github-search input[type=search]', function(e) {
        var isEnterKey = function(key) { return key === 13; };
        if(isEnterKey(e.which)) {
            e.preventDefault();
            var keywords = dom.$keywords().val();
            var loader = new Loader('.loading');
            loader.show();
            server.search(keywords).then(renderGitHubUsers)
                                   .then(dom.resizeContent(dom.$step2))
                                   .then(slider.next)
                                   .then(loader.hide);
        }
    });

    dom.$content.on('click', '#step-1 .github-search button', function(e) {
        e.preventDefault();
        var keywords = dom.$keywords().val();
        var loader = new Loader('.loading');
        loader.show();
        server.search(keywords).then(renderGitHubUsers)
                               .then(dom.resizeContent(dom.$step2))
                               .then(slider.next)
                               .then(loader.hide);
    });

    dom.$content.on('click', '#step-2 .github-users li', function(e) {
        var $gitHubUser = $(e.currentTarget),
            gitHubUser = {
                username: $gitHubUser.find('.username a').text(),
                fullname: $gitHubUser.find('.fullname').text(),
                language: $gitHubUser.find('.language').text(),
                followers: $gitHubUser.find('.followers').text()
            };
        server.overview(gitHubUser).then(renderResult)
                                   .then(dom.resizeContent(dom.$step3))
                                   .then(slider.next);
        server.progress(gitHubUser, updateProgress(server.close));
    });

    /**
     * Starting app
     */
    renderGitHubSearch();
    var slider = new Slider({
        $back: dom.$back,
        $next: dom.$next
    });
});