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
            var $parent = $container();
            //spinner.spin($target()[0]);
            $progress().css('width', '0%');
            this.displayTimeline();
            $parent.show();
            return Zanimo.transition($parent[0], 'opacity', 1, 200, 'ease');
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
            if(value >= 50) {
                this.refreshSpinner({
                    color: 'white'
                });
            }
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

        this.displayTimeline = function() {
            $('.twitter-waiting').show();
        };
    };
})();