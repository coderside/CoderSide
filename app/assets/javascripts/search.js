/**
 * search.js
 */

(function() {
    window.Search = function() {
        var self = this;

        $(document).on('focus', '.search input[name=keywords]', function() {
            var $search = $(this);
            var $github = $search.next('.icon-github');
            $github.css('color', '#393939');
        });

        $(document).on('focusout', '.search input[name=keywords]', function() {
            var $search = $(this);
            var $github = $search.next('.icon-github');
            $github.css('color', '#797575');
        });

        $(document).on('click', '.search .submit-search', function(e) {
            e.preventDefault();
            self.clear();
            self.submit();
        });

        $(document).on('keydown', '.search input[name=keywords]', function(e) {
            if(e.keyCode == 13) { //ENTER key.
                e.preventDefault();
                self.clear();
                self.submit();
            }
        });

        $(document).on('webkitTransitionEnd', '.results li', function() {
            var $next = $(this).next('li');
            if($next.length) $next.addClass('fade-in');
        });

        $(document).on('mouseenter', '.results li', function() {
            $(this).find('.hint').addClass('hover');
        });

        $(document).on('mouseleave', '.results li', function() {
            $(this).find('.hint').removeClass('hover');
        });

        $(document).on('click', '.results li', function(e) {
            var $gitHubUser = $(this),
                gitHubUser = {
                    username: $gitHubUser.find('.username').text(),
                    fullname: $gitHubUser.find('.fullname').text(),
                    language: $gitHubUser.find('.language').text()
                };
            CoderSide.navigate('/profil?' + $.param(gitHubUser));
        });

        this.clear = function() {
            $('.results').empty();
        };

        this.submit = function() {
            var $keywords = $('.search input[name=keywords]');
            CoderSide.navigate('/search/' + encodeURIComponent($keywords.val()));
        };

        this.render = function(res) {
            $('.results').html(res);
            $('.results li:first').addClass('fade-in');
        };
    };
})();