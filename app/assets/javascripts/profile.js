/**
 * profile.js
 */

(function() {
    window.Profile = function() {
        var self = this;

        this.empty = function() {
            $('.profile').empty();
        };

        this.isEmpty = function() {
            return $('.profile').text().trim() == '';
        };

        this.fadeIn = function() {
            var $profile = $('.profile');
            var promiseOpacity = Zanimo.transition($profile[0], 'opacity', 1, 200, 'ease');
            var promiseMargin = Zanimo.transition($profile[0], 'margin-left', 0, 200, 'ease');
            return Q.all([promiseOpacity, promiseMargin]);
        };

        this.fadeOut = function() {
            var $profile = $('.profile');
            $profile.empty();
            var promiseOpacity = Zanimo.transition($profile[0], 'opacity', 0, 200, 'ease');
            var promiseMargin = Zanimo.transition($profile[0], 'margin-left', '35px', 200, 'ease');
            return Q.all([promiseOpacity, promiseMargin]);
        };

        this.toggleFade = function() {
            var $profile = $('.profile');
            if($profile.css('opacity') == 1) {
                this.fadeOut();
            } else {
                this.fadeIn();
            }
        };

        this.render = function(content) {
            $('.layout-content .profile').html(content);
            $('.layout-content .tweets li:last').addClass('last');
            $('.layout-content .organization:last li:last').addClass('last');
        };
    };
})();