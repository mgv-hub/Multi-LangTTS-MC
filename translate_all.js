/**
 * Multi-LangTTS-MC Language Bootstrapper
 *
 * Handles translation generation for Minecraft language files
 * using Google Translate API.
 *
 * NOTE:
 * This project mixes Java (Fabric mod) + Node.js tooling for localization.
 *
 * NOTE:
 * Automated translations are not always accurate and may contain errors or unnatural phrasing.
 * However, they are used as a fallback solution and are better than having missing translations.
 */

const fs = require('fs').promises;
const path = require('path');
const axios = require('axios');
const crypto = require('crypto');

const CONFIG = {
  inputPath: path.resolve(__dirname, 'en_us.json'),
  outputDir: path.resolve('src', 'main', 'resources', 'assets', 'multilangtts-mc', 'lang'),
  cacheFile: path.join("cache", "translation_cache.json"),
  requestDelay: 1000,
  localeDelay: 2000,
  maxRetries: 3,
  retryDelay: 1500,
  apiTimeout: 10000,
  userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
};

const TARGET_LOCALES = [
  'af_za', 'bg_bg', 'cs_cz', 'da_dk', 'de_de', 'de_at', 'de_ch', 'el_gr',
  'es_es', 'es_mx', 'es_ar', 'es_cl', 'es_ec', 'es_uy', 'es_ve', 'et_ee',
  'eu_es', 'gl_es', 'hr_hr', 'hu_hu', 'is_is', 'ga_ie', 'it_it', 'lv_lv',
  'lb_lu', 'mk_mk', 'mt_mt', 'nl_nl', 'nl_be', 'no_no', 'pl_pl', 'lt_lt',
  'pt_pt', 'pt_br', 'ro_ro', 'sk_sk', 'sl_si', 'sv_se', 'tr_tr', 'id_id',
  'uk_ua', 'cy_gb', 'fi_fi', 'ja_jp', 'ko_kr', 'hi_in', 'vi_vn', 'th_th',
  'fil_ph', 'fa_ir', 'sw_ke', 'zh_cn', 'zh_tw', 'ru_ru',
  'en_us', 'en_gb', 'ar_sa', 'fr_fr', 'fr_ca'
];

const localeMap = {
  'zh_cn': 'zh-CN', 'zh_tw': 'zh-TW', 'zh_hk': 'zh-HK',
  'pt_br': 'pt-BR', 'es_mx': 'es-MX', 'en_gb': 'en-GB',
  'fr_ca': 'fr-CA', 'de_at': 'de-AT', 'de_ch': 'de-CH',
  'ar_sa': 'ar'
};

const delay = ms => new Promise(res => setTimeout(res, ms));

function getGoogleLocale(mc) {
  return localeMap[mc] || mc.split('_')[0];
}

function hash(text) {
  return crypto.createHash('md5').update(text, 'utf8').digest('hex');
}

async function loadCache() {
  try {
    return JSON.parse(await fs.readFile(CONFIG.cacheFile, 'utf-8'));
  } catch {
    return { sourceHashes: {}, translations: {} };
  }
}

async function saveCache(cache) {
  await fs.writeFile(CONFIG.cacheFile, JSON.stringify(cache, null, 2), 'utf-8');
}

async function translate(text, locale, retries = CONFIG.maxRetries) {
  if (typeof text !== 'string' || !text.trim()) return text;

  const target = getGoogleLocale(locale);

  try {
    const res = await axios.get('https://translate.googleapis.com/translate_a/single', {
      params: { client: 'gtx', sl: 'en', tl: target, dt: 't', q: text },
      timeout: CONFIG.apiTimeout,
      headers: { 'User-Agent': CONFIG.userAgent }
    });

    if (res.data?.[0] && Array.isArray(res.data[0])) {
      const result = res.data[0].filter(i => i?.[0]).map(i => i[0]).join('');
      return result || text;
    }
    return text;
  } catch (err) {
    if (retries > 0) {
      await delay(CONFIG.retryDelay);
      return translate(text, locale, retries - 1);
    }
    console.warn(`[WARN] Failed to translate "${text.slice(0, 30)}..." → ${locale}`);
    return text;
  }
}

async function loadLocaleFile(locale) {
  try {
    return JSON.parse(await fs.readFile(path.join(CONFIG.outputDir, `${locale}.json`), 'utf-8'));
  } catch {
    return {};
  }
}

async function main() {
  console.log('Reading source file...');
  const sourceData = JSON.parse(await fs.readFile(CONFIG.inputPath, 'utf-8'));

  const cache = await loadCache();
  const newHashes = {};
  for (const [k, v] of Object.entries(sourceData)) {
    newHashes[k] = hash(v);
  }

  await fs.mkdir(CONFIG.outputDir, { recursive: true });
  console.log(`Output dir: ${CONFIG.outputDir}\n`);



  for (const locale of TARGET_LOCALES) {
    console.log(`Processing: ${locale}`);
    const existing = await loadLocaleFile(locale);
    const output = { ...existing };
    const total = Object.keys(sourceData).length;
    let done = 0, updated = 0, skipped = 0;



    for (const [key, value] of Object.entries(sourceData)) {
      const cacheKey = `${locale}:${key}`;
      const needsUpdate = newHashes[key] !== cache.sourceHashes[key] || !cache.translations[cacheKey];

      if (needsUpdate) {
        output[key] = await translate(value, locale);
        cache.translations[cacheKey] = output[key];
        updated++;
        await delay(CONFIG.requestDelay);
      } else {
        output[key] = cache.translations[cacheKey];
        skipped++;
      }

      done++;
      if (done % 50 === 0) {
        process.stdout.write(`  ${done}/${total} (↑${updated} ≈${skipped})\r`);
      }
    }


    process.stdout.write('\n');
    const outPath = path.join(CONFIG.outputDir, `${locale}.json`);
    await fs.writeFile(outPath, JSON.stringify(output, null, 2), 'utf-8');
    console.log(`Saved: ${outPath} (↑${updated} ≈${skipped})\n`);

    await delay(CONFIG.localeDelay);
  }



  cache.sourceHashes = newHashes;
  await saveCache(cache);
  console.log('Done. All languages');
}

main().catch(err => {
  console.error('error:', err.message);
  process.exit(1);
});