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
        '/search/:keywords': {
            get: function(any, params) {
                if(params.keywords) {
                    CoderSide.home.setInputSearch(params.keywords);
                    jsRoutes.controllers.Application.search(
                        params.keywords
                    ).ajax().done(function(response) {
                        CoderSide.search.render(response);
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
                        CoderSide.profil.render(response);
                    });
                }
            }
        }
    });

    CoderSide.home = new Home();
    CoderSide.search = new Search();
    CoderSide.profil = new Profil();
    CoderSide.start('/');
});

