/**
 * home.js
 */

(function() {
    window.Home = function() {
        var self = this;

        this.exist = function() {
            return $('.home').text().trim() != '';
        };

        this.empty = function() {
            $('.home').empty();
        };

        this.isEmpty = function() {
            return $('.home').text().trim() == '';
        };

        this.fadeIn = function() {
            var $home = $('.home');
            return Zanimo.transition($home[0], 'opacity', 1, 200, 'ease');
        };

        this.fadeOut = function() {
            var $home = $('.home');
            $home.empty();
            return Zanimo.transition($home[0], 'opacity', 0, 200, 'ease');
        };

        this.toggleFade = function() {
            var $home = $('.home');
            if($home.css('opacity') == 0) {
                this.fadeIn();
            } else {
                $home.css('opacity', 0);
                this.fadeOut();
            }
        };

        this.isFirstLoading = function() {
            return $('.home').hasClass('first-loading');
        };

        this.notFirstLoading = function() {
            $('.home').removeClass('first-loading');
        };

        this.render = function(content) {
            $('.layout-content .home').html(content);
            //CoderSide.home.fade();
            //this.fadeIn();
        };
    };
})();