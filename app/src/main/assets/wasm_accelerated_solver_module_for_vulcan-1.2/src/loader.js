(async () => {
    const settings = await new Promise((resolve) =>
        chrome.storage.local.get(["extensionEnabled", "onlyIfNeeded"], resolve),
    );

    if (settings.extensionEnabled === false) return;

    const onlyIfNeeded = settings.onlyIfNeeded !== false;

    function isolateElement(id, newId) {
        const el = document.getElementById(id);
        if (!el) return;
        el.id = newId;
        const dummy = document.createElement(el.tagName);
        dummy.id = id;
        dummy.style.display = "none";
        el.parentNode.insertBefore(dummy, el);
    }

    function restoreElement(tempId, originalId) {
        const el = document.getElementById(tempId);
        if (!el) return null;
        document.getElementById(originalId)?.remove();
        el.id = originalId;
        return el;
    }

    function sabotage(wrapper) {
        if (wrapper.dataset.sabotaged) return;

        if (!wrapper.dataset.originalRounds)
            wrapper.dataset.originalRounds = wrapper.dataset.rounds;

        wrapper.dataset.rounds = "0";

        isolateElement("captcha-response", "captcha-response-hijacked");
        isolateElement(
            "captcha-progress-wrapper",
            "captcha-progress-wrapper-wasm",
        );
        isolateElement(
            "captcha-success-wrapper",
            "captcha-success-wrapper-wasm",
        );

        document.querySelectorAll(".captcha-input").forEach((el) => {
            el.classList.replace("captcha-input", "captcha-input-wasm");
            el.disabled = true;
        });

        wrapper.dataset.sabotaged = "true";
    }

    function solve() {
        const wrapper = document.querySelector("div.captcha-wrapper");
        if (!wrapper) return;

        sabotage(wrapper);
        wrapper.parentElement?.classList.add("visible");

        const { challenge, difficulty } = wrapper.dataset;
        const rounds = parseInt(
            wrapper.dataset.originalRounds || wrapper.dataset.rounds,
        );

        const progressFill = document.getElementById("captcha-progress-fill");
        const progressText = document.getElementById("captcha-progress-text");
        if (progressFill) progressFill.style.width = "0%";
        if (progressText) progressText.innerText = "0%";

        const startTime = performance.now();

        const listener = (message) => {
            const { type, solution, current, total } = message;

            if (type === "PROGRESS") {
                const percent = Math.floor((current / total) * 100) + "%";
                requestAnimationFrame(() => {
                    if (progressFill) progressFill.style.width = percent;
                    if (progressText) progressText.innerText = percent;
                });
                return;
            }

            chrome.runtime.onMessage.removeListener(listener);

            if (type === "SUCCESS") {
                const duration = (performance.now() - startTime).toFixed(2);

                if (progressFill) progressFill.style.width = "100%";
                if (progressText) progressText.innerText = "100%";

                const label = document.querySelector(
                    ".captcha-wrapper .captcha-success-label",
                );
                if (label)
                    label.textContent = `Pomyślnie rozwiązano w ${duration} ms dzięki WASM!`;

                const responseInput = restoreElement(
                    "captcha-response-hijacked",
                    "captcha-response",
                );
                if (responseInput) responseInput.value = solution;

                const progress = restoreElement(
                    "captcha-progress-wrapper-wasm",
                    "captcha-progress-wrapper",
                );
                if (progress) progress.classList.remove("active");

                const success = restoreElement(
                    "captcha-success-wrapper-wasm",
                    "captcha-success-wrapper",
                );
                if (success) success.classList.add("active");

                document
                    .querySelectorAll(".captcha-input-wasm")
                    .forEach((el) => {
                        el.disabled = false;
                        el.classList.replace(
                            "captcha-input-wasm",
                            "captcha-input",
                        );
                    });
            } else if (type === "ERROR") {
                sessionStorage.setItem("wasm_failed", "true");
                window.location.reload();
            }
        };

        chrome.runtime.onMessage.addListener(listener);
        chrome.runtime.sendMessage({
            type: "SOLVE",
            payload: { challenge, difficulty: parseInt(difficulty), rounds },
        });
    }

    if (sessionStorage.getItem("wasm_failed")) {
        sessionStorage.removeItem("wasm_failed");
        return;
    }

    let started = false;

    function tryLaunch() {
        const wrapper = document.querySelector("div.captcha-wrapper");
        if (!wrapper) return;
        if (onlyIfNeeded && !wrapper.offsetParent) return;
        solve();
        started = true;
    }

    tryLaunch();

    const observer = new MutationObserver(() => {
        const wrapper = document.querySelector("div.captcha-wrapper");
        if (wrapper && (!onlyIfNeeded || wrapper.offsetParent))
            sabotage(wrapper);
        if (!started) tryLaunch();
        if (started) observer.disconnect();
    });

    observer.observe(document.body, {
        attributes: true,
        childList: true,
        subtree: true,
        attributeFilter: ["style", "class"],
    });
})();
