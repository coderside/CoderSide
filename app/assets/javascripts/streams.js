/**
 * stream.js
 */

(function() {
    window.Streams = function() {
        var progressES = null;

        this.closeProgress = function() {
            progressES && progressES.close();
        };

        this.progress = function(username, fullname, language) {
            if(EventSource) {
                var uri = jsRoutes.controllers.Application.progress(
                    username,
                    fullname,
                    language
                ).absoluteURL();

                progressES = new EventSource(uri);
                progressES.onmessage = function(msg) {
                    var progress = JSON.parse(msg.data);
                    if(progress >= 100) progressES.close();
                    if(CoderSide.home.isFirstLoading() || CoderSide.popular.oneSelected()) {
                        CoderSide.loading.progress(progress);
                    } else {
                        CoderSide.search.progress(progress);
                    }
                };
                progressES.onerror = handleError
            }
        };
    };
})();
