(function(window, document) {
	if (!window.wx) {
		window.wx = {};
	}
	if (typeof window.wx.closeWindow !== 'function') {
		window.wx.closeWindow = function() {
			if (window.history.length > 1) {
				window.history.back();
			}
		};
	}

	function removeById(id) {
		var node = document.getElementById(id);
		if (node && node.parentNode) {
			node.parentNode.removeChild(node);
		}
	}

	window.BpmtH5 = {
		showLoading: function(message) {
			removeById('bpmt_h5_loading');
			var loading = document.createElement('div');
			loading.id = 'bpmt_h5_loading';
			loading.className = 'bpmt-h5-loading';
			loading.innerHTML = '<div>' + (message || '数据加载中') + '</div>';
			document.body.appendChild(loading);
		},
		hideLoading: function() {
			removeById('bpmt_h5_loading');
		},
		toast: function(message) {
			removeById('bpmt_h5_toast');
			var toast = document.createElement('div');
			toast.id = 'bpmt_h5_toast';
			toast.className = 'bpmt-h5-toast';
			toast.innerHTML = message || '';
			document.body.appendChild(toast);
			window.setTimeout(function() {
				removeById('bpmt_h5_toast');
			}, 2400);
		}
	};
})(window, document);
