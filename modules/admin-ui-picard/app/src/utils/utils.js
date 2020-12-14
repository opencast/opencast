import languages from "../i18n/languages";
import i18n from "../i18n/i18n";

/**
 * This File contains methods that are needed in more than one places
 */

export const getTimezoneOffset = () => {
    let d = new Date();
    let offset = d.getTimezoneOffset();

    return offset/60;
};

export const getCurrentLanguageInformation = () => {
    // Get code, flag, name and date locale of the current language
    let currentLang = languages.find(({ code }) => code === i18n.language);
    if (typeof currentLang === 'undefined') {
        currentLang = languages.find(({ code }) => code === "en-GB");
    }

    return currentLang;
}
