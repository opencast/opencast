import { Track } from "./OpencastRest";
import { compareResolutions } from "./utils";

/**
 * Sorts an array of tracks alphabetically by their type
 * If tracks have the same type, they are then sorted by resolution
 * @param tracks the tracks to be sorted
 */
export const sortByType = (tracks: Track[]) => {
  tracks.sort(function(a, b){
    // Compare by type
    if(a.type > b.type) {
      return 1;
    }
    if(b.type > a.type) {
      // eslint-disable-next-line @typescript-eslint/no-magic-numbers
      return -1;
    }

    if (a.resolution === undefined || b.resolution === undefined) {
      return 0;
    } // Else

    return compareResolutions(a.resolution, b.resolution);

  })
  return tracks;
}