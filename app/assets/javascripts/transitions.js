/**
 * transitions.js
 */

(function() {
    window.Transitions = function() {
        this.toProfile = function() {
            return Q.all([CoderSide.home.fadeOut(), CoderSide.loading.fadeOut()], CoderSide.profile.fadeIn());
        };

        this.toHome = function() {
            return Q.all([CoderSide.profile.fadeOut(), CoderSide.loading.fadeOut(), CoderSide.home.fadeIn()]);
        };

        this.toLoading = function() {
            return Q.all([CoderSide.home.fadeOut(), CoderSide.profile.fadeOut(), CoderSide.loading.fadeIn()]);
        };
    };
})();