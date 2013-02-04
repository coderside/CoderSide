/**
 * transitions.js
 */

(function() {
    window.Transitions = function() {
        this.toProfile = function() {
            return Q.all([CoderSide.home.fadeOut(), CoderSide.profile.fadeIn()]);
        };

        this.toHome = function() {
            return Q.all([CoderSide.profile.fadeOut(), CoderSide.home.fadeIn()]);
        };
    };
})();