var exec = require('cordova/exec');

exports.getType = function(success, error) {
    exec(success, error, 'NetworkTypePlugin', 'getType', []);
};
