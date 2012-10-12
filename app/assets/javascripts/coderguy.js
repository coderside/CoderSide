/**
 * coderguy.js
 */

$(document).ready(function() {
    var dom = {
        $search: $('.content form input[type=search]'),
        $submit: $('.content form button'),
        $result: $('.content .result')
    };

    var tmpl = {
        twitter: _.template($("#twitter_tmpl").html()),
        github: _.template($("#github_tmpl").html()),
        klout: _.template($("#klout_tmpl").html()),
        linkedin: _.template($("#linkedin_tmpl").html())
    };

    var renderResult = function() {
        dom.$result.append(tmpl.linkedin({
        }));

        dom.$result.append(tmpl.github({
        }));

        dom.$result.append(tmpl.twitter({
        }));

        dom.$result.append(tmpl.klout({
        }));
    };

    var requests = {
        search: function(success) {
            var keywords = dom.$search.val();
            return $.ajax({
                url: '/search',
                data: $.param({ keywords: keywords }),
                success: success,
                error: function() {
                    alert('Error while searching the specified coder guy !');
                }
            });
        }
    };

    dom.$search.on('keydown', function(e) {
        var isEnterKey = function(key) { return key === 13; };
        if(isEnterKey(e.which)) {
            e.preventDefault();
            requests.search().then(renderResult);
        }
    });

    dom.$submit.on('click', function(e) {
        e.preventDefault();
        requests.search().then(renderResult);
    });
});