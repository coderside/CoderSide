/**
 * coderside.js
 */

$(document).ready(function() {
    $('form[name=github-search] input[name=keywords]').focus(function() {
        var $search = $(this);
        var $github = $search.next('.icon-github');

        $github.css('color', '#393939');
    });

    $('form[name=github-search] input[name=keywords]').focusout(function() {
        var $search = $(this);
        var $github = $search.next('.icon-github');

        $github.css('color', '#797575');
    });

    $('.results li').hover(function() {
        $(this).find('.hint').addClass('hover');
    }, function() {
        $(this).find('.hint').removeClass('hover');
    });

    $('.results li').css('left', '-110%');

    var fadeIn = function($elt) {
        return function() {
            $elt.addClass('transition');
        };
    };

    $('.results li').on('webkitTransitionEnd', function() {
        var $next = $(this).next('li');
        if($next.length) $next.addClass('transition');
    });

    window.setTimeout(fadeIn($('.results li:first')));
});