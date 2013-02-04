/**
 * search.js
 */

(function() {
    window.Search = function() {
        var self = this;

        var progressSpinner = new Spinner({
            lines: 9,
            length: 7,
            width: 2.3,
            radius: 10,
            corners: 1,
            rotate: 0,
            color: 'grey',
            speed: 1,
            trail: 73,
            shadow: false,
            hwaccel: false,
            className: 'spinner',
            zIndex: 2e9,
            top: '-15px',
            left: 'auto'
        });

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

        $(document).on('mouseenter', '.results .result', function() {
            $(this).find('.hint').addClass('hover');
        });

        $(document).on('mouseleave', '.results .result', function() {
            $(this).find('.hint').removeClass('hover');
        });

        $(document).on('click', '.results .result', function(e) {
            if(!$('.results .result.selected').length) {
                var $gitHubUser = $(this),
                    gitHubUser = {
                        username: $gitHubUser.find('.username').text(),
                        fullname: $gitHubUser.find('.fullname').text(),
                        language: $gitHubUser.find('.language').text()
                    };
                $gitHubUser.addClass('selected');
                $gitHubUser.addClass('hover');
                CoderSide.navigate('/profile?' + $.param(gitHubUser));
            }
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

        this.hideSearchLoading = function() {
            var $iconGitHub = $('.search .icon-github'),
                $loadingGitHub = $('.search .loading-github');
            $iconGitHub.show();
            $loadingGitHub.hide();
        };

        this.showSearchLoading = function() {
            var $iconGitHub = $('.search .icon-github'),
                $loadingGitHub = $('.search .loading-github');
            $iconGitHub.hide();
            $loadingGitHub.show();
        };

        this.toggleSearchLoading = function() {
            var $iconGitHub = $('.search .icon-github'),
                $loadingGitHub = $('.search .loading-github');

            if($iconGitHub.css('display') === 'none' || $iconGitHub.css('display') != 'block') {
                this.hideSearchLoading();
            } else {
                this.showSearchLoading();
            }
        };

        this.showProgress = function() {
            var $selected = $('.results .selected'),
                $spinner = $selected.find('.spinner'),
                target = $selected.find('.spinner-container')[0];
            progressSpinner.spin(target);
        };

        this.hideProgress = function() {
            var $selected = $('.results .selected'),
                $spinner = $selected.find('.spinner');
            $spinner.remove();
        };

        this.toggleProgress = function() {
            var $selected = $('.results .selected'),
                $spinner = $selected.find('.spinner');

            if($selected.length) {
                if(!$spinner.length) {
                    this.showProgress();
                } else {
                    this.hideProgress();
                }
            }
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

        this.fadeIn = function() {
            var $results = $('.results .result');
            var d = Q.defer();
            var show = function($elt) {
                setTimeout(function() {
                    Zanimo.transform($elt[0], 'translate3d(105%, 0, 0)');
                    if($elt.next().length) {
                        show($elt.next());
                    } else {
                        d.resolve($elt[0]);
                    }
                }, 120);
            };
            show($('.results .result:first'));
            return d.promise;
        };

        this.render = function(res) {
            $('.results').html(res);
            this.fadeIn();
        };
    };
})();