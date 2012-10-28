/**
 * Slider.js
 * Just a simple slider that use zanimo.
 */

(function($, Zanimo) {
    window.Slider = function() {
        var self = this;

        var viewportWidth = function() {
            if(document.width) {
                return window.innerWidth > document.width ? window.innerWidth : document.width;
            } return window.innerWidth;
        };

        (function() {
            var $pages = $('.step'),
                $current = $('.step:first'),
                $container = $('.content');

            $pages.each(function(index, page) {
                var $page = $(page);
                $page.css('display', 'block');
                $page.css('opacity', 0);
                $page.css('webkitTransform', 'translate3d(' + viewportWidth() + 'px, 0px, 0px)');
            });

            $current.css('opacity', 1);
            $current.css('webkitTransform', 'translate3d(0px, 0px, 0px)');
            $container.css('height', $pages.css('clientHeight') + 'px');
            window.scrollTo(0, 0);
        })();

        this.current = function() {
            return $('.step.current');
        };

        this.next = function() {
            self.go(self.current().next(), false);
        };

        this.previous = function() {
            self.go(self.current().prev(), true);
        };

        this.go = function($target, prev) {
            var $current = self.current();
            $target.css('opacity', 1);

            var toLeft = prev ? '' : '-';

            var hideCurrent = function() {
                return Zanimo.transition(
                    $current[0],
                    'transform',
                    'translate3d('+ toLeft + viewportWidth() + 'px, 0px, 0px)',
                    1000
                ).then(function(success) {
                    $current.css('opacity', 0);
                    $current.removeClass('current');
                }, function(failed) {
                });
            };

            var showTarget = function() {
                return Zanimo.transition(
                    $target[0],
                    'transform',
                    'translate3d(0px, 0px, 0px)',
                    1000
                ).then(function(success) {
                    $target.addClass('current');
                }, function(failed) {
                });
            };

            hideCurrent();
            showTarget();
        };
    };
})(jQuery, Zanimo);