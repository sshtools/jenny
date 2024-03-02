io.onReady(function(io)  {
	const cat = $('#jobCategory');

	io.subscribe('jobs.' + cat.data('job-category'), function(msg) {

		const uuid = msg.uuid;
		const bar = $(cat.find('[data-job-uuid=' + uuid + ']'));
		if (msg.type === 'update') {
			var prg = $(bar.find('.progress'));
			prg.attr('aria-valuemax', msg.max);
			prg.attr('aria-valuenow', msg.val);
			prg.attr('aria-label', msg.text);
			
			bar.find('.job-title').html(msg.title);
			
			var br = $(bar.find('.progress-bar'));
			br.html(msg.text);
			br.css('width', msg.percent + '%');
		}
		else if (msg.type === 'complete') {
			window.location.reload();
		}

	});
});