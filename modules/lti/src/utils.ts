import { parse as parseQuery } from "query-string";
import { Track } from "./OpencastRest";

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
 * Sorts an array of tracks alphabetically by their type
 * If tracks have the same type, they are then sorted by resolution
 * @param tracks the tracks to be sorted
 */
export const sortByType = (tracks: Track[]) => {
  tracks.sort(function(a, b){
    const nameA = a.type,
        nameB = b.type;
    const descA = a.resolution,
        descB = b.resolution;

    if(nameA > nameB) {
      return 1;
    }
    if(nameB > nameA) {
      // eslint-disable-next-line @typescript-eslint/no-magic-numbers
      return -1;
    }

    if (descA === undefined || descB === undefined) {
      return 0;
    } // Else
    return sortResolutions(descA, descB);

  })
  return tracks;
}

/**
 * Helper function for sortByType. Sorts a resolution string by the number it contains
 * @param descA resolution of the first track
 * @param descB resolution of the second track
 */
const sortResolutions = (descA: string, descB: string) => {
  const descAWidth = parseInt(descA.split('x')[0]);
  const descBWidth = parseInt(descB.split('x')[0]);
  if(descAWidth > descBWidth) {
    // eslint-disable-next-line @typescript-eslint/no-magic-numbers
    return -1;  // Returns descB
  }
  if(descBWidth > descAWidth) {
    return 1;
  }

  const descAHeight = parseInt(descA.split('x')[1]);
  const descBHeight = parseInt(descB.split('x')[1]);
  if(descAHeight > descBHeight) {
    // eslint-disable-next-line @typescript-eslint/no-magic-numbers
    return -1;
  }
  if(descBHeight > descAHeight) {
    return 1;
  }

  return 0;
}