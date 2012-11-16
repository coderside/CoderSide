/**
 * Slider.js
 * Just a simple slider that use zanimo.
 */

(function($, Zanimo) {
    window.Slider = function(buttons) {
        var self = this;

        if(buttons) {
            buttons.$back.on('click', function(e) {
                self.previous();
            });

            buttons.$next.on('click', function(e) {
                self.next();
            });
        }

        var atFirstPage = function() {
            return $('.step.current').attr('id') == 'step-1';
        };

        var atLastPage = function() {
            return $('.step.current').attr('id') == 'step-3';
        };

        var updateButtons = function() {
            if(!atFirstPage()) {
                buttons.$back.show();
            } else buttons.$back.hide();

            if(!atLastPage()) {
                buttons.$next.show();
            } else buttons.$next.hide();
        };

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
            self.go(self.current().next(), false, function() {
                updateButtons();
            });
        };

        this.previous = function() {
            self.go(self.current().prev(), true, function() {
                updateButtons();
            });
        };

        this.go = function($target, prev, callback) {
            var $current = self.current();
            $target.css('opacity', 1);

            var way = prev ? '' : '-';
            var hideCurrent = function() {
                return Zanimo.transition(
                    $current[0],
                    'transform',
                    'translate3d('+ way + viewportWidth() + 'px, 0px, 0px)',
                    750
                ).then(function(success) {
                    $current.css('opacity', 0);
                    $current.removeClass('current');
                }, function(failed) {
                    console.log('[Slider] An error occured white making the transition : ' + failed);
                    $current.css('opacity', 0);
                    $current.removeClass('current');
                });
            };

            var showTarget = function() {
                return Zanimo.transition(
                    $target[0],
                    'transform',
                    'translate3d(0px, 0px, 0px)',
                    750
                ).then(function(success) {
                    $target.addClass('current');
                    callback();
                }, function(failed) {
                    console.log('[Slider] An error occured white making the transition : ' + failed);
                    $target.addClass('current');
                    callback();
                });
            };

            hideCurrent();
            showTarget();
        };
    };
})(jQuery, Zanimo);