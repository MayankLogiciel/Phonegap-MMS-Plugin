var exec = require('cordova/exec');

var sms = {};

function convertPhoneToArray(phone) {
    if (typeof phone === 'string' && phone.indexOf(',') !== -1) {
        phone = phone.split(',');
    }
    if (Object.prototype.toString.call(phone) !== '[object Array]') {
        phone = [phone];
    }
    return phone;
}


sms.send = function(phone, message, imageFile, method, success, failure) {
    // parsing phone numbers
    phone = convertPhoneToArray(phone);

    // fire
    exec(
        success,
		failure,
		'Sms',
		'send',
		[phone, message, imageFile, method]
    );
};


module.exports = sms;