/**
 * loading.js
 */

(function() {
    window.Loading = function(options) {
        var $container = function() { return $('.loading'); },
            $target = function() { return $('.loading .spin-container'); },
            $progress = function() { return $('.loading .progress .bar'); };

        var dftOptions = {
            lines: 9,
            length: 8,
            width: 2.5,
            radius: 10,
            corners: 1,
            rotate: 0,
            color: '#252525',
            speed: 0.9,
            trail: 30,
            shadow: false,
            hwaccel: false,
            className: 'spinner',
            zIndex: 2e9,
            top: 'auto',
            left: 'auto'
        };

        options = options || {};

        var spinner = new Spinner(
            $.extend(dftOptions, options)
        );

        this.fadeIn = function() {
            var self = this,
                $spinContainer = $('.spin-container');
            //spinner.spin($spinContainer[0]);
            var fadeInTimeline = function(retry) {
                var $parent = $('.twitter-waiting');
                return function() {
                    if((retry || 0) < 3) {
                        var $waiting = $('.twitter-waiting'),
                            $timeline = $('iframe.twitter-timeline');
                        $progress().css('width', '0%');
                        if($timeline.length) {
                            $spinContainer.empty();
                            $waiting.show();
                            $timeline.show();
                            Zanimo.transition($waiting[0], 'opacity', 1, 500, 'ease');
                            Zanimo.transition($timeline[0], 'opacity', 1, 500, 'ease');
                        } else {
                            retry = retry ? (retry + 1) : 1;
                            setTimeout(fadeInTimeline(retry), 1000);
                        }
                    }
                };
            };
            var $loading = $container();
            $loading.show();
            Zanimo.transition($loading[0], 'opacity', 1, 200, 'ease');
            fadeInTimeline()();
        };

        this.fadeOut = function() {
            var $parent = $container();
            return Zanimo.transition($parent[0], 'opacity', 0, 200, 'ease').then(function() {
                $parent.hide();
            });
        };

        this.refreshSpinner = function(opt) {
            spinner = new Spinner(
                $.extend(dftOptions, opt)
            );
            $container().find('.spinner').remove();
            spinner.spin($target()[0]);
        };

        this.progress = function(value) {
            $progress().css('width', value + '%');
        };

        this.preLoadTimeline = function() {
            var timeline = '<a class="twitter-timeline" href="https://twitter.com/coderside" data-widget-id="294713405175111681"></a>';
            $container().find('.twitter-waiting').append(timeline);
            !function(d,s,id) {
                var js, fjs = d.getElementsByTagName(s)[0];
                     if(!d.getElementById(id)) {
                    js = d.createElement(s);
                    js.id=id;
                    js.src="//platform.twitter.com/widgets.js";
                    fjs.parentNode.insertBefore(js,fjs);
                }
            }(document,"script","twitter-wjs");
        };
    };
})();