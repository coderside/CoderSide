/**
 * loader.js
 */

window.Loader = function(target, overlay, options) {
    var $target = function() { return $(target); },
        $overlay = function() { return $(overlay); };

    var dftOptions = {
        lines: 9,
        length: 7,
        width: 3,
        radius: 10,
        corners: 1,
        rotate: 0,
        color: '#000',
        speed: 1,
        trail: 60,
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

    var isShown = false;

    this.show = function() {
        spinner.spin($target()[0]);
        $target().show();
        $overlay().show();
        isShown = true;
    };

    this.hide = function() {
        $target().find('.spinner').remove();
        $overlay().hide();
        isShown = false;
    };

    this.toggle = function() {
        if(isShown) {
            this.hide();
        } else this.show();
    };
};
