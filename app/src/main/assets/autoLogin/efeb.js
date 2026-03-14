fillLoginForm();

async function fillLoginForm() {
	const result = await chrome.storage.local.get();
	const login = result?.Login;
	const haslo = result?.Haslo;

	const triedLoggingIn = sessionStorage.getItem('triedLoggingIn');
	let lastLoginField = null;

	if (login !== undefined && haslo !== undefined && triedLoggingIn === null) {
		setInterval(() => {
			if (lastLoginField === document.getElementById('Login')) return;
			document.getElementById('Login').value = login;
			document.getElementById('Haslo').value = haslo;
			document.getElementById('btLogOn').click();
			sessionStorage.setItem('triedLoggingIn', '1');
			lastLoginField = document.getElementById('Login');
		}, 50);
	} else {
		if (triedLoggingIn === '1') {
			document.querySelectorAll('.message-error').forEach((e) => {
				e.innerHTML =
					'Wygląda na to, że niedu ma zapisane błędne dane logowania lub Vulcan wymaga uzupełnienia Captchy. Proszę zalogować się ręcznie.';
			});
		}
		document.addEventListener(
			'input',
			(e) => {
				const target = e.target;
				if (target?.id === 'Login') {
					chrome.storage.local.set({ Login: target.value });
				} else if (target?.id === 'Haslo') {
					chrome.storage.local.set({ Haslo: target.value });
				}
			},
			true,
		);
	}
}
