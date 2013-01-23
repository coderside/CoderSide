/**
 * coderside.js
 */

$(document).ready(function() {
    CoderSide = new Simrou({
        '/' : {
            get: function() {
                console.log('Welcome to CoderSide !');
                if(!CoderSide.home.exist()) {
                    jsRoutes.controllers.Application.home().ajax().done(function(response) {
                        CoderSide.home.render(response);
                    });
                } else {
                    CoderSide.search.clear();
                }
            }
        },
        '/search/:keywords': {
            get: function(any, params) {
                if(params.keywords) {
                    if(CoderSide.home.exist()) {
                        CoderSide.search.disable();
                        CoderSide.search.toggleLoading();

                        jsRoutes.controllers.Application.search(params.keywords).ajax().done(function(response) {
                            CoderSide.search.render(response);
                            CoderSide.search.enable();
                            CoderSide.search.toggleLoading();
                            CoderSide.search.setInputSearch(params.keywords);
                        });
                    } else {
                        var promiseHome = jsRoutes.controllers.Application.home().ajax(),
                            promiseResults = jsRoutes.controllers.Application.search(params.keywords).ajax();
                        $.when(promiseHome, promiseResults).done(function(home, results) {
                            CoderSide.home.render(home[0]);
                            CoderSide.search.render(results[0]);
                            CoderSide.search.enable();
                            CoderSide.search.setInputSearch(params.keywords);
                        });
                    }
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
        },
        '/progress?*queryString': {
            get: function(any, params) {
                var data = parseQueryString(params.queryString);
                if(data.username && data.fullname && data.language && EventSource) {
                    var uri = jsRoutes.controllers.Application.progress(
                        data.username,
                        data.fullname,
                        data.language
                    ).absoluteURL();

                    var eventSource = new EventSource(uri);
                    eventSource.onmessage = function(msg) {
                        var progress = JSON.parse(msg.data);
                        CoderSide.search.progress(progress);
                    };
                    eventSource.onerror = function() {
                        console.log('Error while getting progress update');
                    };
                }
            }
        },
        '/*any': {
            get: function() {
                CoderSide.navigate('/');
            }
        }
    });

    CoderSide.home = new Home();
    CoderSide.search = new Search();
    CoderSide.profil = new Profil();
    CoderSide.start('/');
});

