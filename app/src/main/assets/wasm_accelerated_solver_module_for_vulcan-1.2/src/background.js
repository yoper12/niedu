import init, { find_single_nonce } from "../pkg/wasm_for_vulcan.js";

const wasmReady = init();

chrome.runtime.onMessage.addListener((message, sender) => {
    if (message.type !== "SOLVE") return;

    const tabId = sender.tab.id;

    (async () => {
        try {
            await wasmReady;

            const { challenge, difficulty, rounds } = message.payload;
            let currentBase = challenge;
            const results = [];

            for (let i = 0; i < rounds; i++) {
                const nonce = find_single_nonce(currentBase, difficulty);
                results.push(nonce);
                currentBase += nonce;

                chrome.tabs.sendMessage(tabId, {
                    type: "PROGRESS",
                    current: i + 1,
                    total: rounds,
                });

                await new Promise((r) => setTimeout(r, 0));
            }

            chrome.tabs.sendMessage(tabId, {
                type: "SUCCESS",
                solution: results.join(";"),
            });
        } catch (err) {
            chrome.tabs.sendMessage(tabId, {
                type: "ERROR",
                error: err.toString(),
            });
        }
    })();

    return true;
});
