/**
 * profil.js
 */

(function() {
    window.Profil = function() {
        var self = this;

        this.render = function(res) {
            $('.layout-content .tweets li:last').addClass('last');
            $('.layout-content .organization:last li:last').addClass('last');
            $('.layout-content .home').css('opacity', 0);

            $('.home').on('webkitTransitionEnd', function() {
                $('.layout-content').html(res);
                $('.layout-content .profil').css('margin-left', 0);
                $('.layout-content .profil').css('opacity', 1);
            });
        };
    };
})();