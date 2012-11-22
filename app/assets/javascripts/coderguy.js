/**
 * coderguy.js
 */

$(document).ready(function() {

    window.CoderGuy = Sammy('.wrapper', function() {
        this.get('#/', function() {
            var step1  = CoderGuy.step1,
                step2  = CoderGuy.step2,
                slider = CoderGuy.slider;

            if(step1.isEmpty()) CoderGuy.step1.render();
            slider.go(step1, { back: false, next: !step2.isEmpty() });
        });

        this.get('#/github/search', function(context) {
            var keywords = this.params['keywords'],
                step1    = CoderGuy.step1,
                step2    = CoderGuy.step2,
                step3    = CoderGuy.step3,
                slider   = CoderGuy.slider,
                commons  = CoderGuy.commons;

            if(step2.isEmpty() || step1.isNewSearch(keywords)) {
                if(CoderGuy.isEmpty()) commons.loader.show();
                step1.toggleSubmit();
                step1.toggleLoader();
                step1.search(keywords)
                     .then(step2.render(context))
                     .then(slider.goAsFunction(step2, { back: true, next: false }))
                     .then(step1.toggleLoader)
                     .then(step1.toggleSubmit)
                     .then(commons.loader.hide)
                     .fail(step1.toggleLoader)
                     .fail(step1.toggleSubmit)
                     .fail(commons.loader.hide);
            } else {
                slider.go(step2, { back: true, next: !step3.isEmpty() });
            }
        });

        this.get('#/mashup/', function(context) {
            var step2  = CoderGuy.step2,
                step3  = CoderGuy.step3,
                slider = CoderGuy.slider,
                commons  = CoderGuy.commons,
                gitHubUser = {
                    username: this.params['username'],
                    fullname: this.params['fullname'],
                    language: this.params['language'],
                    followers: this.params['followers']
                };

            if(step3.isEmpty() || step2.isNewSearch(gitHubUser.username)) {
                if(CoderGuy.isEmpty()) commons.loader.show();
                step2.toggleLoader();
                step2.overview(gitHubUser)
                     .then(step3.render)
                     .then(slider.goAsFunction(step3, { back: true, next: false }))
                     .then(step2.toggleLoader)
                     .then(commons.loader.hide)
                     .fail(step2.toggleLoader)
                     .fail(commons.loader.hide);
                step2.progress(gitHubUser);
            } else {
                if(step3.isEmpty()) {
                    
                } else {
                    slider.go(step3, { back: true, next: false });
                }
            }
        });
    });

    CoderGuy.commons = new function() {
        this.loader = new Loader(
            '.coderguy-loader',
            '.overlay-loading',
            {
                top: '-40px',
                left: '15px'
            }
        );

        this.renderError = function(msg) {
            alert(msg);
        };

        this.dom = function(selector) {
            return function() {
                return $(selector);
            };
        };
    };

    CoderGuy.step1 = new Step1(
        '#content',
        '#step-1',
        _.template($("#github-search-tmpl").html())
    );

    CoderGuy.step2 = new Step2(
        '#content',
        '#step-2',
        _.template($("#github-users-tmpl").html())
    );

    CoderGuy.step3 = new Step3(
        '#content',
        '#step-3', {
            twitter: _.template($("#twitter-result-tmpl").html()),
            github: _.template($("#github-result-tmpl").html()),
            klout: _.template($("#klout-result-tmpl").html()),
            linkedin: _.template($("#linkedin-result-tmpl").html())
        }
    );

    CoderGuy.isEmpty = function() {
        return CoderGuy.step1.isEmpty() &&
            CoderGuy.step2.isEmpty() &&
            CoderGuy.step3.isEmpty();
    };

    CoderGuy.slider = new Slider(
        CoderGuy.step1, {
            $back: $('.back'),
            $next: $('.next')
        }
    );

    CoderGuy.run('#/');
    console.log('CoderGuy started...');
});