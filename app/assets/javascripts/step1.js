/**
 * step1.js
 */

(function() {
    window.Step1 = function(container, el, tmpl) {
        var self = this,
            loader = new Loader('.loading', '.overlay-loading'),
            dom = {
                $submit: CoderGuy.commons.dom('#step-1 .github-search button'),
                $keywords: CoderGuy.commons.dom('#content #step-1 .github-search input[type=search]'),
                $step2 : $('#step-2')
            };

        this.$container = $(container);
        this.$el = $(el);
        this.keywords = null;

        this.search = function(keywords) {
            this.keywords = keywords;
            return $.ajax({
                url: '/search',
                data: $.param({ keywords: keywords }),
                error: function() {
                    console.log('Error while pre-searching the specified coder guy !');
                    self.close();
                    CoderGuy.commons.renderError("An error occurred : failed searching on gitHub");
                }
            });
        };

        this.toggleSubmit = function() {
            if(dom.$submit().hasClass('disabled')) {
                dom.$submit().removeClass('disabled');
            } else dom.$submit().addClass('disabled');
        };

        this.toggleLoader = function() {
            loader.toggle();
        };

        this.isNewSearch = function(keywords) {
            return this.keywords != keywords;
        };

        this.isEmpty = function() {
            return !self.$el.children().length;
        };

        this.isEqual = function(otherView) {
            return this.$el.attr('id') == otherView.$el.attr('id');
        };

        this.render = function() {
            this.$el.html(tmpl({}));
        };
    };
})();