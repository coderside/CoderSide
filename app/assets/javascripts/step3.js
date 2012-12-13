/**
 * step3.js
 */

(function() {
    window.Step3 = function(container, el, tmpl) {
        var self = this;

        this.$container = $(container);
        this.$el = $(el);

        this.resizeContent = function() {
            self.$container.height(self.$el.height() + 15);
        };

        this.isEmpty = function() {
            return !self.$el.children().length;
        };

        this.isEqual = function(otherView) {
            return this.$el.attr('id') == otherView.$el.attr('id');
        };

        this.render = function(coderGuy) {
            self.$el.empty();

            self.$el.append(tmpl.linkedin({
                user: coderGuy.linkedInUser
            }));

            var gitHubUser = coderGuy.gitHubUser;
            self.$el.append(tmpl.github({
                organizations: gitHubUser ? gitHubUser.organizations : [],
                repositories: gitHubUser ? gitHubUser.repositories : [],
                sortByUpdatedAt: function(repositories) {
                    return _(repositories).sortBy(function(repository) {
                        return -repository.updatedAt;
                    });
                }
            }));

            var twitterUser = coderGuy.twitterUser;
            self.$el.append(tmpl.twitter({
                user: twitterUser,
                timeline: twitterUser ? twitterUser.timeline : null
            }));

            var kloutUser = coderGuy.kloutUser;
            self.$el.append(tmpl.klout({
                user: kloutUser,
                influencers: kloutUser ? kloutUser.influencers : [],
                influencees: kloutUser ? kloutUser.influencees : []
            }));

            self.$el.append(tmpl.errors({
                errors: coderGuy.errors
            }));

            self.resizeContent();
        };
    };
})();