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
            console.log(coderGuy);
            self.$el.append(tmpl.linkedin({
                user: coderGuy.linkedInUser
            }));
            self.$el.append(tmpl.github({
                organizations: coderGuy.gitHubUser.organizations || [],
                repositories: coderGuy.gitHubUser.repositories || [],
                sortByContributions: function(repositories) {
                    return _(repositories).sortBy(function(repository) {
                        return -repository.contributions;
                    });
                }
            }));

            self.$el.append(tmpl.twitter({
                user: coderGuy.twitterUser,
                timeline: coderGuy.twitterUser.timeline
            }));

            self.$el.append(tmpl.klout({
                user: coderGuy.kloutUser,
                influencers: coderGuy.kloutUser.influencers,
                influencees: coderGuy.kloutUser.influencees
            }));
            self.resizeContent();
        };
    };
})();