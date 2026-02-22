# WASM Accelerated Solver Module for Vulcan

![License](https://img.shields.io/github/license/yoper12/WASM-for-Vulcan)

Thanks to Rust and Web Assembly you can now pass Vulcan Proof-of-Work captcha about **~20 times faster** (from 5 s to 220 ms on i7-12700KF), and even more on mobile devices!

## 🚀 Features

- **Speed:** Drastically reduces login time by accelerating the PoW calculation.
- **Privacy:** Runs entirely locally in your browser.
- **Compatibility:** Works on Chromium-based browsers (Chrome, Brave, Vivaldi) and Firefox.

## 📦 Installation

### Manual Build
To build the extension from source you need to:

1. Install [Rust](https://rust-lang.org/), [wasm-pack](https://drager.github.io/wasm-pack/) and [web-ext](https://extensionworkshop.com/documentation/develop/getting-started-with-web-ext/)
2. Run `wasm-pack build --target web`
3. Run `web-ext build`

### Pre-built
Simply download a ready build from the [releases page](https://github.com/yoper12/WASM-for-Vulcan/releases).

## 🔧 How to use

To add the extension to your browser:

1. **Firefox:**
   - Open `about:debugging#/runtime/this-firefox`
   - Click on "Add temporary extension"
   - Select `manifest.json` from your unpacked .zip.
2. **Chromium:**
   - Open `chrome://extensions/`
   - Turn on "Developer mode" (top right corner)
   - Click on "Load unpacked"
   - Select your unpacked folder.

## 🛠️ How it works
This extension intercepts the Proof-of-Work challenge from EduVulcan and offloads the heavy calculation to a WebAssembly module compiled from Rust. This is significantly faster than the default JavaScript implementation provided by the website.
