/* Jenny IO Javascript API */
class JennyIO {
	on = {};
	_onReady = [];
	_ready = false;
	
	constructor() {
		var sckt = new WebSocket(this._wsUri(), 'monitor');

		sckt.onclose = (event) => {
			console.log('Close  ' + event);
		};
		sckt.onopen = (event) => {
			console.log('Open  ' + event);
			this._ready = true;
			for(var i = 0 ; i < this._onReady.length; i++)
			     this._onReady[i](this);
		};
		sckt.onerror = (event) => {
			console.log('Error ' + event);
		};
		sckt.onmessage = (event) => {
			const msg = JSON.parse(event.data);
			switch (msg.type) {
				case 'reload':
					console.log('Page reload requested');
					window.location.href = window.location.href;
					//window.location.reload();
					break;
				case 'message':
                    if(msg.channel in this.on)
					   this.on[msg.channel](msg.data);
					else
					   console.log('No handler for channel ' + msg.channel);
					break;
				default:
					console.log('No handler for message type of ' + msg.type);
					break;
			}
		};

		this._sckt = sckt;
	}
	
	onReady(cb) {
        if(this._ready) 
            cb(this);
        else
            this._onReady.push(cb);
    }
	
	unsubscribe(channel) {
		this.on[channel] = false;
		debugger;
		this._sckt.send(JSON.stringify({
			"type": "unsubscribe",
			"channel": channel
		}));
	}


	subscribe(channel, cb) {
		this.on[channel] = cb;
		var self = this;
		this._sckt.send(JSON.stringify({
			"type": "subscribe",
			"channel": channel
		}));
		return {
			unsubscribe: function() {
				self.unsubscribe(channel);
			},
			send: function(obj) {
				self.send(channel, obj);
			}
		};
	}

	send(channel, obj) {
		this._sckt.send(JSON.stringify({
			"type": "message",
			"channel": channel,
			"data": obj
		}));
	}

	_wsUri() {
		var loc = window.location, uri;
		if (loc.protocol === 'https:') {
			uri = 'wss:';
		} else {
			uri = 'ws:';
		}
		uri += '//' + loc.host;
		uri += /*loc.pathname + */ '/io/io';
		return uri;
	}
}

io = new JennyIO();
