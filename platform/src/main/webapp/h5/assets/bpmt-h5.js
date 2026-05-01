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

	function each(nodes, callback) {
		for (var i = 0; i < nodes.length; i++) {
			callback(nodes[i], i);
		}
	}

	function closest(node, selector) {
		if (!node) {
			return null;
		}
		if (node.closest) {
			return node.closest(selector);
		}
		while (node && node.nodeType === 1) {
			if (matches(node, selector)) {
				return node;
			}
			node = node.parentNode;
		}
		return null;
	}

	function matches(node, selector) {
		var fn = node.matches || node.msMatchesSelector || node.webkitMatchesSelector;
		return fn ? fn.call(node, selector) : false;
	}

	function ready(callback) {
		if (document.readyState === 'loading') {
			document.addEventListener('DOMContentLoaded', callback);
			return;
		}
		callback();
	}

	function setBusy(form, busy) {
		var buttons = form.querySelectorAll('button, input[type="submit"]');
		each(buttons, function(button) {
			if (busy) {
				button.setAttribute('data-bpmt-h5-disabled', button.disabled ? '1' : '0');
				button.disabled = true;
			} else if (button.getAttribute('data-bpmt-h5-disabled') === '0') {
				button.disabled = false;
				button.removeAttribute('data-bpmt-h5-disabled');
			}
		});
	}

	var BpmtH5 = window.BpmtH5 || {};

	BpmtH5.showLoading = BpmtH5.showLoading || function(message) {
			removeById('bpmt_h5_loading');
			var loading = document.createElement('div');
			var content = document.createElement('div');
			loading.id = 'bpmt_h5_loading';
			loading.className = 'bpmt-h5-loading';
			content.textContent = message || '数据加载中';
			loading.appendChild(content);
			document.body.appendChild(loading);
		};

	BpmtH5.hideLoading = BpmtH5.hideLoading || function() {
			removeById('bpmt_h5_loading');
		};

	BpmtH5.toast = BpmtH5.toast || function(message) {
			removeById('bpmt_h5_toast');
			var toast = document.createElement('div');
			toast.id = 'bpmt_h5_toast';
			toast.className = 'bpmt-h5-toast';
			toast.textContent = message || '';
			document.body.appendChild(toast);
			window.setTimeout(function() {
				removeById('bpmt_h5_toast');
			}, 2400);
		};

	BpmtH5.confirm = BpmtH5.confirm || function(message, onOk) {
		if (window.Wxui && typeof window.Wxui.confirm === 'function') {
			window.Wxui.confirm(message, onOk);
			return;
		}
		if (window.confirm(message) && typeof onOk === 'function') {
			onOk();
		}
	};

	BpmtH5.notice = BpmtH5.notice || function(message) {
		if (window.Wxui && typeof window.Wxui.alert === 'function') {
			window.Wxui.alert(message);
			return;
		}
		window.alert(message);
	};

	BpmtH5.toggle = BpmtH5.toggle || function(target, forceOpen) {
		if (!target) {
			return;
		}
		var open = typeof forceOpen === 'boolean' ? forceOpen : target.className.indexOf('bpmt-h5-hidden') >= 0 || target.className.indexOf('is-collapsed') >= 0;
		if (open) {
			target.className = target.className.replace(/\bbpmt-h5-hidden\b/g, '').replace(/\bis-collapsed\b/g, '').replace(/\bbpmt-h5-collapsed\b/g, '');
		} else if (target.className.indexOf('bpmt-h5-hidden') < 0) {
			target.className += ' bpmt-h5-hidden';
		}
	};

	BpmtH5.bindQueryToggles = BpmtH5.bindQueryToggles || function(root) {
		root = root || document;
		each(root.querySelectorAll('[data-bpmt-h5-toggle="query"]'), function(trigger) {
			if (trigger.getAttribute('data-bpmt-h5-bound') === '1') {
				return;
			}
			trigger.setAttribute('data-bpmt-h5-bound', '1');
			trigger.addEventListener('click', function(event) {
				var selector = trigger.getAttribute('data-target') || trigger.getAttribute('href');
				var target = selector && selector.charAt(0) === '#' ? document.getElementById(selector.substring(1)) : closest(trigger, '.bpmt-h5-page');
				if (target && (!selector || selector.charAt(0) !== '#')) {
					target = target.querySelector('.bpmt-h5-query');
				}
				if (target) {
					event.preventDefault();
					BpmtH5.toggle(target);
					trigger.setAttribute('aria-expanded', target.className.indexOf('bpmt-h5-hidden') < 0 ? 'true' : 'false');
				}
			});
		});
	};

	BpmtH5.bindFormFeedback = BpmtH5.bindFormFeedback || function(root) {
		root = root || document;
		each(root.querySelectorAll('form.bpmt-h5-form, .bpmt-h5-form form'), function(form) {
			if (form.getAttribute('data-bpmt-h5-form-bound') === '1') {
				return;
			}
			form.setAttribute('data-bpmt-h5-form-bound', '1');
			form.addEventListener('submit', function() {
				setBusy(form, true);
				window.setTimeout(function() {
					setBusy(form, false);
				}, 8000);
			});
		});
	};

	BpmtH5.normalizeTables = BpmtH5.normalizeTables || function(root) {
		root = root || document;
		each(root.querySelectorAll('table.am-table, .bpmt-h5-page table'), function(table) {
			if (closest(table, '.bpmt-h5-table-scroll')) {
				return;
			}
			var wrapper = document.createElement('div');
			wrapper.className = 'bpmt-h5-table-scroll';
			table.parentNode.insertBefore(wrapper, table);
			wrapper.appendChild(table);
		});
	};

	BpmtH5.normalizeCards = BpmtH5.normalizeCards || function(root) {
		root = root || document;
		each(root.querySelectorAll('.bpmt-h5-list > li, .bpmt-h5-list .am-list-item, .bpmt-h5-page .am-panel'), function(node) {
			if (node.className.indexOf('bpmt-h5-card') < 0) {
				node.className += ' bpmt-h5-card';
			}
		});
	};

	BpmtH5.init = BpmtH5.init || function(root) {
		root = root || document;
		BpmtH5.bindQueryToggles(root);
		BpmtH5.bindFormFeedback(root);
		BpmtH5.normalizeTables(root);
		BpmtH5.normalizeCards(root);
	};

	window.BpmtH5 = BpmtH5;
	ready(function() {
		window.BpmtH5.init(document);
	});
})(window, document);
