/**
 * Slider.js
 */

(function($, Zanimo) {
    window.Slider = function(startView, buttons) {
        var self = this,
            duration = 300;

        this.currentView = startView;

        var updateButtons = function(optButtons) {
            if(optButtons.back) {
                buttons.$back.show();
            } else buttons.$back.hide();

            if(optButtons.next) {
                buttons.$next.show();
            } else buttons.$next.hide();
        };

        var viewportWidth = function() {
            if(document.width) {
                return window.innerWidth > document.width ? window.innerWidth : document.width;
            } return window.innerWidth;
        };

        var unbindButtons = function() {
            buttons.$back.unbind('click');
            buttons.$next.unbind('click');
        };

        var bindButtons = function() {
            buttons.$back.on('click', function(e) {
                history.back();
                unbindButtons();
            });

            buttons.$next.on('click', function(e) {
                history.forward();
                unbindButtons();
            });
        };

        if(buttons) bindButtons();

        (function() {
            var $pages = $('.step'),
                $current = self.currentView.$el,
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

        this.goAsFunction = function(targetView, optButtons, succeed) {
            return function() {
                return self.go(targetView, optButtons, succeed);
            };
        };

        this.go = function(targetView, optButtons, succeed) {
            var $current = this.currentView.$el,
                $target = targetView.$el;

            if(!this.currentView.isEqual(targetView)) {
                var way = ($current.nextAll('#' + $target.attr('id')).length > 0) ? '-' : '';
                $target.css('opacity', 1);

                var hideCurrent = function() {
                    return Zanimo.transition(
                        $current[0],
                        'transform',
                        'translate3d('+ way + viewportWidth() + 'px, 0px, 0px)',
                        duration
                    ).then(function(success) {
                        $current.css('opacity', 0);
                        $current.removeClass('current');
                        self.currentView = targetView;
                        updateButtons(optButtons);
                        bindButtons();
                        targetView.resizeContent();
                        succeed && succeed();
                    }, function(err) {
                        $current.css('opacity', 0);
                        $current.removeClass('current');
                        self.currentView = targetView;
                        updateButtons(optButtons);
                        bindButtons();
                        targetView.resizeContent();
                        succeed && succeed();
                    });
                };

                var showTarget = function() {
                    return Zanimo.transition(
                        $target[0],
                        'transform',
                        'translate3d(0px, 0px, 0px)',
                        duration
                    ).then(function(success) {
                        $target.addClass('current');
                        targetView.resizeContent();
                    }, function(err) {
                        $target.addClass('current');
                        targetView.resizeContent();
                    });
                };

                hideCurrent();
                showTarget();
            }
        };
    };
})(jQuery, Zanimo);