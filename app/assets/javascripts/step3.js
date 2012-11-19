/**
 * step3.js
 */

(function() {
    window.Step3 = function(container, el, tmpl) {
        var self = this;

        this.$container = $(container);
        this.$el = $(el);

        this.resizeContent = function() {
            self.$container.height(self.$el.height() + 200);
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

            self.$el.append(tmpl.github({
                repositories: coderGuy.repositories
            }));

            self.$el.append(tmpl.twitter({
                user: coderGuy.twitterUser,
                timeline: coderGuy.twitterTimeline
            }));

            self.$el.append(tmpl.klout({
                user: coderGuy.kloutUser,
                influencers: coderGuy.influencers,
                influencees: coderGuy.influencees
            }));
            self.resizeContent();
        };
    };
})();