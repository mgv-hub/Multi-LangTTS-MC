# Multi LangTTS MC

> **Multi-language Text-to-Speech Mod for Minecraft (Fabric)**
> *Version 1.0.1 | Minecraft 1.21.10 | Java 21*

---

## 📋 Overview

**Multi LangTTS MC** is a client-side Fabric modification that delivers real-time, multi-language Text-to-Speech (TTS) synthesis for in-game chat. Designed for accessibility, immersion, and multilingual server environments, the mod intercepts chat messages, processes them asynchronously, and converts them into high-quality speech using configurable external providers—all without impacting game performance.

---


## ✨ Core Features

### 🔊 Text-to-Speech Engine
- **Dual Provider Support**:
  - **Google Translate TTS** (Free, 100+ languages)
  - **ElevenLabs API** (Premium, expressive voices, 29+ languages)
- **Non-Blocking Architecture**: All synthesis, network requests, and audio decoding run on dedicated background threads.
- **Audio Caching**: Reduces redundant API calls and latency for repeated phrases.
- **MP3 Validation**: Strict byte-signature detection filters malformed or error responses.
- **Rate Limiting**: Configurable minimum interval between synthesis requests prevents queue saturation.

### 💬 Chat Processing
- **Real-Time Synthesis**: Automatically converts player messages, server broadcasts, and system notifications into spoken audio.
- **Smart Spam Filtering**: Configurable cooldown thresholds prevent repetitive messages from triggering excessive playback.
- **Text Sanitization**: Strips Minecraft formatting codes (`§`, `&`), handles bidirectional markers, and applies RTL formatting for Arabic scripts.
- **Player Name Handling**: Optional speaker labels with language-aware formatting (e.g., "يقول" for Arabic, "says" for English).

### 🗂️ Chat Logging System
- **Daily Rotated Logs**: Plaintext logs stored in `logs/multilangtts-mc/chat-YYYY-MM-DD.txt`.
- **In-Game Viewer**: Scrollable interface with per-message replay via TTS.
- **Export Functionality**: Export session logs to custom file paths.
- **Async I/O**: Non-blocking file writes prevent gameplay stutter.

### ⚙️ Configuration & UI
- **ModMenu Integration**: Full settings interface accessible via ModMenu or keybind.
- **Cloth Config Backend**: Live configuration reloading without restarting the game.
- **Provider-Specific Settings**:
  - Google: Language code selection
  - ElevenLabs: API key, Voice ID, Model ID, Stability, Similarity Boost, Style Exaggeration, Speed
- **Test Suite**: Custom phrase input with one-click playback verification.

### ⌨️ Keybindings
| Action | Default Key | Category |
|--------|-------------|----------|
| Toggle TTS | `B` | Multi LangTTS MC |
| Open Chat Log Viewer | `V` | Multi LangTTS MC |

*Fully customizable via Minecraft Controls menu*

---

## 🌍 Language & Translation Support

### UI Localization
The mod includes complete interface and configuration localization across **60 language variants**. All translations are managed via `en_us.json` and propagated automatically through the integrated translation pipeline (`translate_all.js`).


### Supported Locales (Markdown Table)

| Locale Code | Language | Region |
|-------------|----------|--------|
| `af_za` | Afrikaans | South Africa |
| `ar_sa` | Arabic | Saudi Arabia |
| `bg_bg` | Bulgarian | Bulgaria |
| `cs_cz` | Czech | Czech Republic |
| `cy_gb` | Welsh | United Kingdom |
| `da_dk` | Danish | Denmark |
| `de_de` | German | Germany |
| `de_at` | German | Austria |
| `de_ch` | German | Switzerland |
| `el_gr` | Greek | Greece |
| `en_us` | English | United States |
| `en_gb` | English | United Kingdom |
| `es_es` | Spanish | Spain |
| `es_mx` | Spanish | Mexico |
| `es_ar` | Spanish | Argentina |
| `es_cl` | Spanish | Chile |
| `es_ec` | Spanish | Ecuador |
| `es_uy` | Spanish | Uruguay |
| `es_ve` | Spanish | Venezuela |
| `et_ee` | Estonian | Estonia |
| `eu_es` | Basque | Spain |
| `fa_ir` | Persian | Iran |
| `fi_fi` | Finnish | Finland |
| `fil_ph` | Filipino | Philippines |
| `fr_fr` | French | France |
| `fr_ca` | French | Canada |
| `ga_ie` | Irish | Ireland |
| `gl_es` | Galician | Spain |
| `hi_in` | Hindi | India |
| `hr_hr` | Croatian | Croatia |
| `hu_hu` | Hungarian | Hungary |
| `id_id` | Indonesian | Indonesia |
| `is_is` | Icelandic | Iceland |
| `it_it` | Italian | Italy |
| `ja_jp` | Japanese | Japan |
| `ko_kr` | Korean | South Korea |
| `lb_lu` | Luxembourgish | Luxembourg |
| `lt_lt` | Lithuanian | Lithuania |
| `lv_lv` | Latvian | Latvia |
| `mk_mk` | Macedonian | North Macedonia |
| `mt_mt` | Maltese | Malta |
| `nl_nl` | Dutch | Netherlands |
| `nl_be` | Dutch | Belgium |
| `no_no` | Norwegian | Norway |
| `pl_pl` | Polish | Poland |
| `pt_pt` | Portuguese | Portugal |
| `pt_br` | Portuguese | Brazil |
| `ro_ro` | Romanian | Romania |
| `ru_ru` | Russian | Russia |
| `sk_sk` | Slovak | Slovakia |
| `sl_si` | Slovenian | Slovenia |
| `sv_se` | Swedish | Sweden |
| `sw_ke` | Swahili | Kenya |
| `th_th` | Thai | Thailand |
| `tr_tr` | Turkish | Turkey |
| `uk_ua` | Ukrainian | Ukraine |
| `vi_vn` | Vietnamese | Vietnam |
| `zh_cn` | Chinese (Simplified) | China |
| `zh_tw` | Chinese (Traditional) | Taiwan |

### TTS Language Coverage
| Provider | Languages | Notes |
|----------|-----------|-------|
| **Google Translate TTS** | 100+ | Configurable via language code; default: `ar` |
| **ElevenLabs** | 29+ | Via `eleven_multilingual_v2` model; requires API key |

---

## 🔧 Installation

1. Install **Fabric Loader** for Minecraft **1.21.10**.
2. Download the latest release JAR from the [official repository](https://github.com/mgv-hub/Multi-LangTTS-MC).
3. Place the JAR file into your `mods/` directory.
4. Ensure the following dependencies are installed:
   - [Fabric API](https://modrinth.com/mod/fabric-api) ≥ 0.115.0
   - [Cloth Config API](https://modrinth.com/mod/cloth-config) ≥ 18.0.0 (Fabric)
5. Launch the client. Access settings via **ModMenu** or press the configured keybindings (`B` / `V`).

---

## 🛠️ Technical Specifications

| Component | Specification |
|-----------|--------------|
| **Runtime** | Java 21 |
| **Build System** | Gradle 8.14 + Fabric Loom 1.11-SNAPSHOT |
| **Networking** | OkHttp 4.12.0 with custom interceptors |
| **Audio Decoding** | JLayer 1.0.1.4 |
| **Configuration** | Cloth Config 2 + ModMenu 16.0.0 |
| **Serialization** | Gson (pretty-printed JSON) |
| **Threading** | ExecutorService worker pool + CopyOnWriteArrayList + ReentrantLock |
| **License** | GNU GPL v3.0 |

---

## 👥 Contributors

| Contributor | Role |
|-------------|------|
| **mgv** | Lead Development, Architecture, Core Systems |
| **DFD3** | Co-Development, Testing, Integration |

---

---

## ⚠️ Important Notes

- **ElevenLabs API Key**: Required for premium voice synthesis. Obtain it from https://elevenlabs.io
  The API key is stored locally in the Minecraft config files on the user’s device
  It is only used to access Text-to-Speech (TTS) services and is not shared externally by the mod
  **Recommended setup:**
  https://elevenlabs.io/app/developers/api-keys > Create Key > set **Access** to **Text to Speech**
  Avoid using full-access API keys for better security


- **Google TTS Rate Limits**: Public endpoint usage is subject to undocumented throttling.
- **Arabic Text Handling**: Automatic RTL embedding via Unicode bidi markers; stripped in logs for readability.
- **Audio Playback**: Blocking on worker thread only—never impacts main game loop.

---

## 📄 License

This project is distributed under the **GNU General Public License v3.0**.
See [`LICENSE`](LICENSE) for full terms and conditions.


---

> 🌐 **Repository**: https://github.com/mgv-hub/Multi-LangTTS-MC
> 🐛 **Issues**: https://github.com/mgv-hub/Multi-LangTTS-MC/issues
> 📦 **Releases**: https://github.com/mgv-hub/Multi-LangTTS-MC/releases