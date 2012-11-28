/**
 * step2.js
 */

(function() {
    window.Step2 = function(container, el, tmpl) {
        var self = this,
            loader = new Loader('.progress-container', '.overlay-loading'),
            eventSource = null,
            dom = {
                $progress: CoderGuy.commons.dom('#content #step-2 .github-users .progress'),
                $step3 : $('#step-3')
            };

        this.$container = $(container);
        this.$el = $(el);
        this.username = null;

        var closeStream = function() {
            eventSource && eventSource.close();
        };

        var errorProgress = function() {
            dom.$progress().css('background-color', 'red');
            dom.$progress().fadeOut(2000, function() {
                dom.$progress().css('width', '0%');
            });
        };

       var updateProgress = function(onStop) {
            return function(event) {
                dom.$progress().css('width', event.data + '%');
                if(event.data == 100) {
                    onStop();
                    setTimeout(function() {
                        dom.$progress().css('width', '0%');
                    }, 1000);
                }
            };
        };

        this.overview = function(gitHubUser) {
            this.username = gitHubUser.username;
            return $.ajax({
                url: '/overview',
                data: $.param(gitHubUser),
                error: function() {
                    closeStream();
                    CoderGuy.commons.renderError("An error occurred : failed getting data");
                    errorProgress();
                }
            });
        };

        this.progress = function(gitHubUser) {
            var uri = '/progress?' + $.param(gitHubUser);
            eventSource = new EventSource(uri);
            eventSource.onmessage = updateProgress(closeStream);
            eventSource.onerror = function() {
                console.log('Error while getting progress update');
                closeStream();
                errorProgress();
            };
        };

        this.bindItems = function(context) {
            self.$el.find('.github-users li').on('click', function(e) {
                var $gitHubUser = $(e.currentTarget),
                    gitHubUser = {
                        username: $gitHubUser.find('.username a').text(),
                        fullname: $gitHubUser.find('.fullname').text(),
                        language: $gitHubUser.find('.language').text(),
                        followers: $gitHubUser.find('.followers').text()
                    };
                context.redirect('#/mashup/', gitHubUser);
            });
        };

        this.resizeContent = function() {
            self.$container.height(self.$el.height() + 100);
        };

        this.toggleLoader = function() {
            loader.toggle();
        };

        this.isEmpty = function() {
            return !self.$el.children().length;
        };

        this.isNewSearch = function(username) {
            return this.username != username;
        };

        this.isEqual = function(otherView) {
            return this.$el.attr('id') == otherView.$el.attr('id');
        };

        this.render = function(context) {

            var sortByFollowers = function(gitHubUsers) {
                return _(gitHubUsers).sortBy(function(user) {
                    return -user.followers;
                });
            };

            return function(gitHubUsers) {
                self.$el.html(tmpl({
                    gitHubUsers: sortByFollowers(gitHubUsers)
                }));
                self.bindItems(context);
                self.resizeContent();
            };
        };
    };
})();