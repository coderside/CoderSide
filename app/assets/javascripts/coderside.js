/**
 * coderside.js
 */

$(document).ready(function() {
    CoderSide = new Simrou({
        '/' : {
            get: function() {
                console.log('Welcome to CoderSide !');
            }
        },
        '/search?*queryString': {
            get: function(any, params) {
                var data = parseQueryString(params.queryString);
                if(data.keywords) {
                    jsRoutes.controllers.Application.search(
                        data.keywords
                    ).ajax().done(function(response) {
                        console.log(response);
                    });
                }
            }
        },
        '/profil?*queryString': {
            get: function(any, params) {
                var data = parseQueryString(params.queryString);
                if(data.username && data.fullname && data.language) {
                    jsRoutes.controllers.Application.profil(
                        data.username,
                        data.fullname,
                        data.language
                    ).ajax().done(function(response) {
                        console.log(response);
                    });
                }
            }
        }
    });

    CoderSide.start('/');
});

