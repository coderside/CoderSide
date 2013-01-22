/**
 * profil.js
 */

(function() {
    window.Profil = function() {
        var self = this;

        this.empty = function() {
            $('.profil').empty();
        };

        this.isEmpty = function() {
            return $('.profil').text().trim() == '';
        };

        this.toggleFadeIn = function() {
            var $profil = $('.profil');
            if($profil.css('opacity') == 1) {
                $profil.css('opacity', 0);
            } else {
                $profil.css('opacity', 1);
            }
        };

        this.render = function(res) {
            CoderSide.home.toggleFadeOut();
            $('.home').on('webkitTransitionEnd', function(e) {
                e.stopPropagation();
                if($(e.target).hasClass('home') && CoderSide.home.isEmpty()) {
                    $('.layout-content .profil').html(res);
                    $('.layout-content .tweets li:last').addClass('last');
                    $('.layout-content .organization:last li:last').addClass('last');
                    self.toggleFadeIn();
                }
            });
        };
    };
})();