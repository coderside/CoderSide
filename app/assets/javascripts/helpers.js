/**
 * utils.js
 */

(function() {
    var re = /([^&=]+)=?([^&]*)/g;
    var decodeRE = /\+/g; // Regex for replacing addition symbol with a space

    var decode = function (str) {return decodeURIComponent( str.replace(decodeRE, " ") );};
    window.parseQueryString = function(query) {
        var params = {}, e;
        while ( e = re.exec(query) ) {
            var k = decode( e[1] ), v = decode( e[2] );
            if (params[k]) {
                if(params[k].push) {
                    params[k].push(v);
                } else {
                    params[k] = [params[k], v];
                }
            }
            else params[k] = v;
        }
        return params;
    };
})();