import languages from "../i18n/languages";
import i18n from "../i18n/i18n";

/**
 * This File contains methods that are needed in more than one places
 */

export const getTimezoneOffset = () => {
    let d = new Date();
    let offset = d.getTimezoneOffset() * -1;

    if (offset >= 0) {
        return "+" + offset/60;
    }

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

// fills an array from 00 to number of elements specified
export const initArray = (numberOfElements) => {
    let i, result = [];
    for (i = 0; i < numberOfElements; i++) {
        if (i < 10) {
            result.push({
                index: i,
                value: '0' + i
            });
        }
        else {
            result.push({
                index: i,
                value: '' + i
            });
        }
    }
    return result;
}

/*
 * transforms an object of form { id1: value1, id2: value2 }
 * to [{id: id1, value: value1},{id: id2, value: value2}]
 */
export const transformToIdValueArray = data => {
     return Object.keys(data).map(key => {
        return {
            id: key,
            value: data[key]
        }
    });
}

/*
 * transforms an object of form { id1: object1, id2: object2 }
 * to [
   {
      "id":id1,
      "objectValue":value1,
      "otherObjectValue":otherValue1
   },
   {
      "id":id2,
      "objectValue":value2,
      "otherObjectValue":otherValue2
   }
]
 */
export const transformToObjectArray = data => {
    return Object.keys(data).map(key => {
        return {
            id: key,
            ...data[key]
        }
    });
}


