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

        this.toggleFade = function() {
            var $profil = $('.profil');
            if($profil.css('opacity') == 1) {
                $profil.css('opacity', 0);
                $profil.css('margin-left', '35px');
            } else {
                $profil.css('opacity', 1);
                $profil.css('margin-left', '0');
            }
        };

        this.fade = function(content) {
            $('.home').one(transitionend, function(e) {
                e.stopPropagation();
                if($(e.target).hasClass('home')) {
                    $('.layout-content .profil').html(content);
                    $('.layout-content .tweets li:last').addClass('last');
                    $('.layout-content .organization:last li:last').addClass('last');
                    self.toggleFade();
                }
            });
        };

        this.render = function(content) {
            this.fade(content);
            CoderSide.home.toggleFade();
        };
    };
})();