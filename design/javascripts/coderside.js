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
});