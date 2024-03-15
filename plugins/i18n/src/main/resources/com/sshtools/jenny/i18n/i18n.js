/* Jenny I18N Javascript API */
class JennyI8N {
	bundles = {};
	
	constructor() {
	}
	
    format(key, args) {
        for (const [k, v] of Object.entries(this.bundles)) {
            if(v.hasOwnProperty(key)) {
                return v[key].format(args);
            }
        }
        return key;
    }
	
}

String.prototype.format = function() {
    var args = arguments;
    return this.replace(/\{(\d+)\}/g, function(a) {
        return args[parseInt(a.match(/(\d+)/g))];
    });
};

const i18n = new JennyI8N();
