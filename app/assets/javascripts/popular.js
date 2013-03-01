/**
 * popular.js
 */

(function() {
    window.Popular = function() {
        $(document).on('click', '.popular .coders li', function(e) {
            var $coder = $(this),
                coder = {
                    username: $coder.data('username')
                };
            $('.popular .coders li').removeClass('selected');
            $coder.addClass('selected');
            CoderSide.navigate('/profile?' + $.param(coder));
        });

        this.oneSelected = function() {
            return $('.popular .coders .selected').length;
        };
    };
})();