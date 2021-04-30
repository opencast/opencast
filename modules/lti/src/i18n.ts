import i18n from "i18next";
import LanguageDetector from "i18next-browser-languagedetector";
import { initReactI18next } from "react-i18next";
import daJson from "./i18n/lang-da_DK.json";
import deJson from "./i18n/lang-de_DE.json";
import elJson from "./i18n/lang-el_GR.json";
import enJson from "./i18n/lang-en_US.json";
import esJson from "./i18n/lang-es_ES.json";
import frJson from "./i18n/lang-fr_FR.json";
import glJson from "./i18n/lang-gl_ES.json";
import heJson from "./i18n/lang-he_IL.json";
import itJson from "./i18n/lang-it_IT.json";
import nlJson from "./i18n/lang-nl_NL.json";
import plJson from "./i18n/lang-pl_PL.json";
import slJson from "./i18n/lang-sl_SI.json";
import svJson from "./i18n/lang-sv_SE.json";
import trJson from "./i18n/lang-tr_TR.json";
import zhJson from "./i18n/lang-zh_CN.json";

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        // we init with resources
        resources: {
            en: {
                translations: enJson
            },
            da: {
                translations: daJson
            },
            de: {
                translations: deJson
            },
            el: {
                translations: elJson
            },
            es: {
                translations: esJson
            },
            fr: {
                translations: frJson
            },
            gl: {
                translations: glJson
            },
            he: {
                translations: heJson
            },
            it: {
                translations: itJson
            },
            nl: {
                translations: nlJson
            },
            pl: {
                translations: plJson
            },
            sl: {
                translations: slJson
            },
            sv: {
                translations: svJson
            },
            tr: {
                translations: trJson
            },
            zh: {
                translations: zhJson
            }
        },
        fallbackLng: "en",
        debug: true,

        // have a common namespace used around the full app
        ns: ["translations"],
        defaultNS: "translations",

        interpolation: {
            escapeValue: false
        }
    }).then((_) => console.log("i18n init complete"), (_) => console.log("i18n init failed"));

export default i18n;
