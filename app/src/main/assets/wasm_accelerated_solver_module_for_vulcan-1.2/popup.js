document.addEventListener("DOMContentLoaded", () => {
    const extensionEnabled = document.getElementById("extensionEnabled");
    const onlyIfNeeded = document.getElementById("onlyIfNeeded");

    chrome.storage.local.get(["extensionEnabled", "onlyIfNeeded"], (result) => {
        extensionEnabled.checked = result.extensionEnabled !== false;
        onlyIfNeeded.checked = result.onlyIfNeeded !== false;
    });

    extensionEnabled.addEventListener("change", () => {
        chrome.storage.local.set({
            extensionEnabled: extensionEnabled.checked,
        });
    });

    onlyIfNeeded.addEventListener("change", () => {
        chrome.storage.local.set({ onlyIfNeeded: onlyIfNeeded.checked });
    });
});
