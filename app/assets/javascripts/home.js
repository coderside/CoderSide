/**
 * home.js
 */

(function() {
    window.Home = function() {
        var self = this;

        this.setInputSearch = function(keywords) {
            $('.search input[name=keywords]').val(keywords);
        };

        this.toggleLoading = function() {
            var $iconGitHub = $('.search .icon-github'),
                $loadingGitHub = $('.search .loading-github');

            if($iconGitHub.css('display') === 'none' || $iconGitHub.css('display') === 'block') {
                $iconGitHub.show();
                $loadingGitHub.hide();
            } else {
                $iconGitHub.hide();
                $loadingGitHub.show();
            }
        };

        this.exist = function() {
            return $('.home').text().trim() != '';
        };

        this.empty = function() {
            $('.home').empty();
        };

        this.isEmpty = function() {
            return $('.home').text().trim() == '';
        };

        this.toggleFadeOut = function() {
            var $home = $('.home');
            if($home.css('opacity') == 0) {
                $home.css('opacity', 1);
            } else {
                $home.css('opacity', 0);
                $home.empty();
            }
        };

        this.render = function(searchDOM) {
            $('.layout-content .home').html(searchDOM);
        };
    };
})();