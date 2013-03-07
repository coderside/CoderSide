/**
 * popular.js
 */

(function() {
    window.Popular = function() {
        $(document).on('click', '.popular .coders li', function(e) {
            var $coder = $(this),
                username = $coder.data('username');

            $('.popular .coders li').removeClass('selected');
            $coder.addClass('selected');
            CoderSide.navigate('/profile/' + encodeURIComponent(username));
        });

        this.oneSelected = function() {
            return $('.popular .coders .selected').length;
        };
    };
})();