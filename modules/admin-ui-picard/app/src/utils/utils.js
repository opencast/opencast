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

export const getTimezoneString = (offset) => {
    return 'UTC' + (offset < 0 ? '-' : '+') + offset;
}

export const getCurrentLanguageInformation = () => {
    // Get code, flag, name and date locale of the current language
    let currentLang = languages.find(({ code }) => code === i18n.language);
    if (typeof currentLang === 'undefined') {
        currentLang = languages.find(({ code }) => code === "en-GB");
    }

    return currentLang;
}

// fills an array from 00 to number of elements specified
export const initArray = numberOfElements => {
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
};

// insert leading 0 for numbers smaller 10
export const makeTwoDigits = number => {
    if (number < 10) {
        return '0' + number;
    } else {
        return '' + number;
    }
};

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
 * Compare two arrays
 * returns true, if content of sorted arrays is the same,
 * returns false if length or at least one entry is different
 */
const isArrayChanged = (oldArray, newArray) => {
    if (newArray.length !== oldArray.length) {
        return true;
    }

    const sortedNewArray = [...newArray].sort();
    const sortedOldArray = [...oldArray].sort();
    for (let i = 0; i < sortedNewArray.length; i++){
        if (sortedNewArray[i] !== sortedOldArray[i]) {
            return true;
        }
    }
    return false;
};

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

/*
* iterates trough all attributes in an object and switches 'true'- and 'false'-Strings
* to their corresponding boolean value. All other values stay the same.
*/
export const parseBooleanInObject = baseObject => {
    let parsedObject = {};

    Object.keys(baseObject).forEach(config => {
        parsedObject[config] = parseValueForBooleanStrings(baseObject[config]);
    });

    return parsedObject;
}

/*
* switches 'true'- and 'false'-Strings
* to their corresponding boolean value. All other kinds of values stay the same.
*/
export const parseValueForBooleanStrings = value => {
    let parsedValue = value;
    if (parsedValue === 'true') {
        parsedValue = true;
    } else if (parsedValue === 'false') {
        parsedValue = false;
    }

    return parsedValue;
}

/*
* checks if an user is admin or has the required role to access an ui element
*/
export const hasAccess = (role, userInfo) =>  {

    return !!(userInfo.isAdmin || userInfo.roles.includes(role));
}

// checks, if a String is proper JSON
export const isJson = text => {
    try {
        const json = JSON.parse(text);
        const type = Object.prototype.toString.call(json);
        return type === '[object Object]' || type === '[object Array]';
    } catch (e) {
        return false;
    }
}
