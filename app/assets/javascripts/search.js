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
            self.submit();
        });

        $(document).on('keydown', '.search input[name=keywords]', function(e) {
            if(e.keyCode == 13) { //ENTER key.
                e.preventDefault();
                self.clearResults();
                self.submit();
            }
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
                    language: $gitHubUser.find('.language').text(),
                    classic: true
                };

            $gitHubUser.addClass('selected');

            CoderSide.navigate('/profil?' + $.param(gitHubUser));
        });

        this.clearResults = function() {
            $('.results').empty();
        };

        this.clear = function() {
            $('.search input[name=keywords]').val('');
            self.clearResults();
        };

        this.submit = function() {
            var $keywords = $('.search input[name=keywords]');
            CoderSide.navigate('/search/' + encodeURIComponent($keywords.val()));
        };

        this.progress = function(value) {
            $('.results .selected .progress').css('width', value + '%');
        };

        this.toggleLoading = function() {
            var $iconGitHub = $('.search .icon-github'),
                $loadingGitHub = $('.search .loading-github');

            if($iconGitHub.css('display') === 'none' || $iconGitHub.css('display') != 'block') {
                $iconGitHub.show();
                $loadingGitHub.hide();
            } else {
                $iconGitHub.hide();
                $loadingGitHub.show();
            }
        };

        this.fadeIn = function() {
            $('.results .result').one(transitionend, function(e) {
                e.stopPropagation();
                if($(e.target).hasClass('result')) {
                    var $next = $(this).next('li');
                    if($next.length) $next.addClass('fade-in');
                }
            });
        };

        this.setInputSearch = function(keywords) {
            $('.search input[name=keywords]').val(keywords);
        };

        this.disable = function() {
            $('.search input[name=keywords]').attr('disabled', '');
            $('.search .submit-search').attr('disabled', '');
        };

        this.enable = function() {
            $('.search input[name=keywords]').removeAttr('disabled');
            $('.search .submit-search').removeAttr('disabled');
        };

        this.render = function(res) {
            $('.results').html(res);
            this.fadeIn();

            setTimeout(function() {
                $('.results .result:first').addClass('fade-in');
            }, 100);

            if(!CoderSide.profil.isEmpty()) {
                CoderSide.profil.toggleFade();
                CoderSide.profil.empty();
                CoderSide.home.toggleFade();
            }
        };
    };
})();