import { parse as parseQuery } from "query-string";


export function parsedQueryString() {
    return parseQuery(window.location.search);
}

/**
 * Returns the given string, with the first letter capitalized
 * @param s the string to capitalize
 */
export function capitalize(s: string) {
  if (typeof s !== 'string') {
    return '';
  }
  return s.charAt(0).toUpperCase() + s.slice(1);
}



/**
 * Comparator for resolutions. Compares by width first.
 * @param resolutionA resolution of the first track
 * @param resolutionB resolution of the second track
 */
export const compareResolutions = (resolutionA: {width: number, height: number},
                                   resolutionB: {width: number, height: number}) => {
  if(resolutionA.width > resolutionB.width) {
    // eslint-disable-next-line @typescript-eslint/no-magic-numbers
    return -1;  // Returns resolutionB
  }
  if(resolutionB.width > resolutionA.width) {
    return 1;
  }

  if(resolutionA.height > resolutionB.height) {
    // eslint-disable-next-line @typescript-eslint/no-magic-numbers
    return -1;
  }
  if(resolutionB.height > resolutionA.height) {
    return 1;
  }

  return 0;
}