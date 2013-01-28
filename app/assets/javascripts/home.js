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

        this.toggleFade = function() {
            var $home = $('.home');
            if($home.css('opacity') == 0) {
                $home.css('opacity', 1);
            } else {
                $home.css('opacity', 0);
                $home.empty();
            }
        };

        this.isFirstLoading = function() {
            return $('.home').hasClass('first-loading');
        };

        this.notFirstLoading = function() {
            $('.home').removeClass('first-loading');
        };

        this.render = function(searchDOM) {
            $('.layout-content .home').html(searchDOM);
        };
    };
})();