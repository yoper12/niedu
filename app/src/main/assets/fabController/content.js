let lastReportedUrl = null;

function notifyNative() {
	const currentUrl = window.location.href;
	if (currentUrl !== lastReportedUrl) {
		browser.runtime
			.sendNativeMessage('dev.andus.niedu.fab', {
				action: 'updateFab',
				url: currentUrl,
			})

		lastReportedUrl = currentUrl;
	}
}

const observer = new MutationObserver(() => {
	notifyNative();
});

observer.observe(document.body, {
    childList: true,
    subtree: true,
	attributes: true,
});
