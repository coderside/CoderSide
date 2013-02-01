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

        this.toggleFade = function() {
            var $profile = $('.profile');
            if($profile.css('opacity') == 1) {
                $profile.css('opacity', 0);
                $profile.css('margin-left', '35px');
            } else {
                $profile.css('opacity', 1);
                $profile.css('margin-left', '0');
            }
        };

        this.fade = function(content) {
            $('.home').one(transitionend, function(e) {
                e.stopPropagation();
                if($(e.target).hasClass('home')) {
                    if(self.isEmpty()) {
                        $('.layout-content .profile').html(content);
                        $('.layout-content .tweets li:last').addClass('last');
                        $('.layout-content .organization:last li:last').addClass('last');
                    } else {
                        self.empty();
                    }
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