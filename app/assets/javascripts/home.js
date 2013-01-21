/**
 * home.js
 */

(function() {
    window.Home = function() {
        var self = this;

        this.setInputSearch = function(keywords) {
            $('.search input[name=keywords]').val(keywords);
        };
    };
})();