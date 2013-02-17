/**
 * coderside.js
 */

$(document).ready(function() {

    var currentRequest = null;
    var cancelAnyRequest = function() {
        currentRequest && currentRequest.abort();
    };
    var clearConnections = function() {
        cancelAnyRequest();
        CoderSide.streams.closeProgress();
    };

    CoderSide = new Simrou({
        '/' : {
            get: function() {
                console.log('Welcome to CoderSide !');
                clearConnections();
                if(!CoderSide.home.exist()) {
                    jsRoutes.controllers.Application.home().ajax().done(function(response) {
                        CoderSide.home.render(response);
                    });
                } else {
                    CoderSide.search.clear();
                }
                CoderSide.transitions.toHome();
            }
        },
        '/search/:keywords': {
            get: function(any, params) {
                clearConnections();
                CoderSide.home.notFirstLoading();
                if(params.keywords) {
                    params.keywords = decodeURIComponent(params.keywords);
                    if(CoderSide.home.exist()) {
                        CoderSide.search.disable();
                        CoderSide.search.showSearchLoading();
                        jsRoutes.controllers.Application.search(params.keywords).ajax().done(function(response) {
                            CoderSide.search.render(response);
                            CoderSide.search.enable();
                            CoderSide.search.hideSearchLoading();
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
                            CoderSide.transitions.toHome();
                        });
                    }
                }
            }
        },
        '/profile?*queryString': {
            get: function(any, params) {
                if(CoderSide.home.isFirstLoading() || CoderSide.popular.oneSelected()) {
                    CoderSide.home.empty();
                    CoderSide.transitions.toLoading();
                } else CoderSide.search.showProgress();

                var data = parseQueryString(params.queryString);
                if(data.username) {
                    currentRequest = jsRoutes.controllers.Application.profile(
                        data.username,
                        data.fullname || ""
                    ).ajax().done(function(response) {
                        CoderSide.profile.render(response);
                        CoderSide.transitions.toProfile().then(function() {
                            CoderSide.search.hideProgress();
                        });
                    });
                    CoderSide.resolve('/progress?' + params.queryString, 'get');
                }
            }
        },
        '/progress?*queryString': {
            get: function(any, params) {
                var data = parseQueryString(params.queryString);
                if(data.username) {
                    CoderSide.streams.progress(
                        data.username,
                        data.fullname
                    );
                }
            }
        },
        '/*any': {
            get: function() {
                clearConnections();
                CoderSide.navigate('/');
            }
        }
    });

    CoderSide.transitions = new Transitions();
    CoderSide.streams = new Streams();
    CoderSide.home = new Home();
    CoderSide.search = new Search();
    CoderSide.profile = new Profile();
    CoderSide.loading = new Loading();
    CoderSide.popular = new Popular();
    CoderSide.start('/');
});
