/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "qt_sbtl_embedder.h"


int main(int argc, char* argv[])
{
  if (argc == 2 && strcmp(argv[1], "--help") == 0) {
    print_usage(argv[0]);
    return EXIT_SUCCESS;
  }

  // parse parameters
  program_parameters* parameters = parse_parameters(argc, argv);
  if (parameters == NULL) {
    return EXIT_FAILURE;
  }

  // check srt files
  int ii, valid = 0;
  for (ii = 0; ii < parameters -> sub_number; ii++) {
    int status = check_srt_file((parameters -> subtitle_files[ii]) -> filepath);
    (parameters -> subtitle_files[ii]) -> valid = status;
    if (status) valid++;
  }
  if (valid == 0) {
    fprintf(stderr, "Error: No valid subtitle files found\n");
    for (int kk = 0; kk < parameters -> sub_number; kk++)
      free(parameters -> subtitle_files[kk]);
    free(parameters);
    return EXIT_FAILURE;
  }

  // create copy if necessary
  if (parameters -> output_file != NULL && strcmp(parameters -> output_file,
      parameters -> qt_filepath) != 0) {
    if (!copy_qt_file(parameters -> qt_filepath, parameters -> output_file)) {
      // cleanup
      for (int kk = 0; kk < parameters -> sub_number; kk++)
        free(parameters -> subtitle_files[kk]);
      free(parameters);
      return EXIT_FAILURE;
    }
    parameters -> qt_filepath = parameters -> output_file;
  }

  // open qt for modifying
  MP4FileHandle handle = open_qt_file(parameters -> qt_filepath, parameters -> verbosity);
  if (handle == NULL) {
    if (parameters -> output_file != NULL && remove(parameters -> output_file) != 0) {
      fprintf(stderr, "Error: Could not remove file copy at %s\n", parameters -> output_file);
    }
    for (int kk = 0; kk < parameters -> sub_number; kk++)
      free(parameters -> subtitle_files[kk]);
    free(parameters);
    return EXIT_FAILURE;
  }

  // embed subtitles
  for (ii = 0; ii < parameters -> sub_number; ii++) {
    if (!((parameters -> subtitle_files[ii]) -> valid))
      continue;

    // create new subtitle track
    fprintf(stdout, "Info: Embedding subtitle file %s (%s)\n",
      (parameters -> subtitle_files[ii]) -> filepath,
      (parameters -> subtitle_files[ii]) -> language);


    MP4TrackId track_id = create_subtitle_track(handle,
        (parameters -> subtitle_files[ii]) -> language,
        parameters -> track_height, parameters -> font_height,
        parameters -> bottom_offset);
    if (track_id == 0) {
      close_qt_file(handle);
      if (parameters -> output_file != NULL && remove(parameters -> output_file) != 0) {
        fprintf(stderr, "Error: Could not remove file copy at %s\n", parameters -> output_file);
      }
      for (int kk = 0; kk < parameters -> sub_number; kk++)
        free(parameters -> subtitle_files[kk]);
      free(parameters);
      return EXIT_FAILURE;
    }

    if (!parse_srt_file(
      (parameters -> subtitle_files[ii]) -> filepath, handle, track_id)) {
      // exception occurred - cleaning
      close_qt_file(handle);

      if (parameters -> output_file == NULL) {
        // attempt to clean original file
        remove_track(handle, track_id);
        optimize_qt_file(parameters -> qt_filepath);
      } else {
        // just remove file copy
        if (remove(parameters -> output_file) != 0) {
          fprintf(stderr, "Error: Could not remove file copy at %s\n", parameters -> output_file);
        }
      }
      for (int kk = 0; kk < parameters -> sub_number; kk++)
        free(parameters -> subtitle_files[kk]);
      free(parameters);
      return EXIT_FAILURE;
    }
  } // for

  // close QT file
  close_qt_file(handle);

  if (parameters -> optimize) {
    if (parameters -> output_file == NULL) {
      optimize_qt_file(parameters -> qt_filepath);
    } else {
      optimize_qt_file(parameters -> output_file);
    }
  }

  for (int kk = 0; kk < parameters -> sub_number; kk++)
    free(parameters -> subtitle_files[kk]);
  free(parameters);
  return EXIT_SUCCESS;
} // main


// QT functions

int copy_qt_file(char* source, char* destination)
{
  FILE* source_file;
  FILE* dest_file;

  fprintf(stdout, "Info: Copying QT file...\n");

  if ((source_file = fopen(source, "rb")) == NULL) {
    fprintf(stderr, "Error: Could not open QT file for reading\n");
    return 0;
  }
  if ((dest_file = fopen(destination, "wb")) == NULL) {
    fprintf(stderr, "Error: Could not open destination file for writing\n");
    fclose(source_file);
    return 0;
  }

  // initalize buffer
  char* buffer = (char*) malloc(sizeof(char) * BUFFER_SIZE);
  if (buffer == NULL) {
    fprintf(stderr, "Error: Could not allocate buffer\n");
    fclose(source_file);
    fclose(dest_file);
  }

  while (!feof(source_file)) {
    size_t num_r = fread(buffer, sizeof(char), BUFFER_SIZE, source_file);
    if (num_r != BUFFER_SIZE && ferror(source_file)) {
      fprintf(stderr, "Error: IOError occurred during file reading\n");
      fclose(source_file);
      fclose(dest_file);
      remove(destination);
      return 0;
    }
    size_t num_w = fwrite(buffer, 1, num_r, dest_file);
    if (num_w != num_r) {
      fprintf(stderr, "Error: IOError occurred during file writing\n");
      fclose(source_file);
      fclose(dest_file);
      remove(destination);
      return 0;
    }
  }

  fclose(source_file);
  fclose(dest_file);
  free(buffer);

  fprintf(stdout, "Info: Copying completed\n");

  return 1;
} // copy_qt_file


// SRT functions

/**
 * Checks whether SRT file contains any non UTF-8 character.
 * Returns 0 if such character is found or 1 otherwise.
 */
int check_srt_file(char* srt_filepath)
{
  FILE* srt_file = fopen(srt_filepath, "r");
  if (srt_file == NULL) {
    fprintf(stderr, "Error: Could not open SRT file %s\n", srt_filepath);
    return 0;
  }

  char buffer[BUFFER_SIZE+1];

  while (fgets(buffer, BUFFER_SIZE, srt_file) != NULL) {
    int index, len, c0, c1, c2, c3;

    len = strlen(buffer);
    for (index = 0; index < len; index++) {
      c0 = buffer[index] & 0xff;
      if ((index+1) < len) c1 = buffer[index+1] & 0xff; else c1 = 0;
      if ((index+2) < len) c2 = buffer[index+2] & 0xff; else c2 = 0;
      if ((index+3) < len) c3 = buffer[index+3] & 0xff; else c3 = 0;
      // rules for utf-8 file
      if (c0 >= 0x0 && c0 <= 0x7f) {
        // ascii character
        continue;
      } else if (c0 >= 0xc0 && c0 <= 0xdf
        && c1 >= 0x80 && c1 <= 0xbf) {
        // 2 byte character
        index += 1;
        continue;
      } else if (c0 >= 0xe0 && c0 <= 0xef
        && c1 >= 0x80 && c1 <= 0xbf
        && c2 >= 0x80 && c2 <= 0xbf) {
        // 3 byte character
        index += 2;
        continue;
      } else if (c0 >= 0xf0 && c0 <= 0xf7
        && c1 >= 0x80 && c1 <= 0xbf
        && c2 >= 0x80 && c2 <= 0xbf
        && c3 >= 0x80 && c3 <= 0xbf) {
        // 4 byte character
        index += 3;
      } else {
        fprintf(stderr, "Error: Non UTF-8 character (0x%02x) encountered in file %s\n",
          c0, srt_filepath);
        fclose(srt_file);
        return 0;
      }
    }
  }

  fclose(srt_file);
  return 1;
} // check_srt_file


/**
 * Parse SRT file and for each caption that is extracted 'add_caption_block'
 * method is called. Returns 1 if parsing was successful or 0 if unrecoverable
 * error is encountered (sample write failed or file could not be read).
 */
int parse_srt_file(char* srt_filepath, MP4FileHandle hanlde,
  MP4TrackId track_id)
{
  FILE* srt_file = fopen(srt_filepath, "r");
  if (srt_file == NULL) {
    fprintf(stderr, "Error: Could not open SRT file %s\n", srt_filepath);
    return 0;
  }

  // initialize buffers
  char buffer[BUFFER_SIZE+1];
  char captions_buffer[2 * (BUFFER_SIZE+1)];
  unsigned int len = 0;
  unsigned int cpb_pos = 0;
  // initialize state
  short state = 0;
  // initialize counter
  unsigned int counter = 0;
  // initialize time
  unsigned int time = 0;
  unsigned int cst = 0;
  unsigned int cet = 0;

  while (fgets(buffer, BUFFER_SIZE, srt_file) != NULL) {
    len = strlen(buffer);

    // trim trailing newline chars
    while (len > 0 && (buffer[len-1] == '\r' || buffer[len-1] == '\n')) {
      buffer[--len] = '\0';
    }

    switch (state) {
    case 0: {
      // expecting number
      unsigned int ii = 0;
      if (strpbrk(buffer, ".,:?!-") != NULL) {
        fprintf(stderr,
          "Warning: Invalid beginning of block - expected number, read %s\n", buffer);
        // invalid beginning
        state = 3;
        break;
      }
      if (sscanf(buffer, "%u", &ii) == 0) {
        fprintf(stderr,
          "Warning: Invalid beginning of block - expected number, read %s\n", buffer);
        // invalid beginning
        state = 3;
        break;
      } else if (ii != counter + 1) {
        fprintf(stderr,
          "Warning: Expected sequence %d, read sequence %d\n", counter + 1, ii);
      }
      counter = ii;
      state = 1;
      break;
    }
    case 1: {
      // expecting time code
      // format 00:00:00,000 --> 00:00:00,456
      if (len > 29) {
        // invalid length
        fprintf(stderr, "Warning: Invalid time definition - %s\n", buffer);
        state = 3;
        break;
      }
      char start_srt_time[30];
      char end_srt_time[30];
      if (sscanf(buffer, "%s --> %s", start_srt_time, end_srt_time) < 2) {
        // invalid time format
        fprintf(stderr, "Warning: Invalid time definition - %s\n", buffer);
        state = 3;
        break;
      }
      int start_time = parse_srt_time(start_srt_time);
      int end_time = parse_srt_time(end_srt_time);
      if (start_time == -1 || end_time == -1) {
        fprintf(stderr, "Warning: Invalid time definition - %s\n", buffer);
        state = 3;
        break;
      }
      // checking if time is valid
      if (start_time >= (int)time && end_time > start_time) {
        cst = start_time;
        cet = end_time;
        state = 2;
      } else {
        fprintf(stderr, "Warning: Invalid time sequence\n");
        state = 3;
      }
      break;
    }
    case 2: {
      // expected captions
      if (len == 0) {
        // blank line
        if (cpb_pos == 0) {
          // no captions
          fprintf(stderr, "Warning: missing captions\n");
          // erase times
          cst = 0;
          cet = 0;
          // jump straight to new block parsing
          state = 0;
        } else {
          // write empty sample
          if (!add_caption_block(hanlde, track_id, (char*)"", cst - time)
              || !add_caption_block(hanlde, track_id, captions_buffer, cet - cst)) {
            // exception occurred - write failed
            fprintf(stderr, "Error: Write failed\n");
            fclose(srt_file);
            return 0;
          }
          // clean
          time = cet;
          cst = 0;
          cet = 0;
          cpb_pos = 0;
          state = 0;
        }
      } else {
        // captions
        if (cpb_pos == 0) {
          // 1. line
          strncpy(captions_buffer, buffer, len);
          captions_buffer[len] = '\0';
          cpb_pos = len;
        } else {
          // 2. line, check for length
          if (len > ((2 * BUFFER_SIZE) - cpb_pos)) {
            fprintf(stderr, "Warning: Captions too long...\n");
            // clean
            cst = 0;
            cet = 0;
            cpb_pos = 0;
            state = 3;
          }
          captions_buffer[cpb_pos] = '\n';
          cpb_pos += sizeof(char);
          strncpy(captions_buffer + cpb_pos, buffer, len);
          captions_buffer[cpb_pos + len] = '\0';
          cpb_pos += len;
        }
#if 0
        fprintf(stdout, "Debug: Current caption block: %s\n", captions_buffer);
#endif
      }
      break;
    }
    case 3: {
      // error state - ignore all lines until blank line is encountered
      if (len == 0) {
        state = 0;
      }
      break;
    }
    } // switch (state)
  } // while

  fclose(srt_file);
  return 1;
} // parse_srt_file


/**
 * Returns time in milliseconds if SRT time is in valid format or -1 otherwise.
 */
int parse_srt_time(char* srt_time)
{
  unsigned int h, m, s, ms;
  if (sscanf(srt_time, "%u:%u:%u,%u", &h, &m, &s, &ms) < 4) {
    // invalid format
    return -1;
  }
  // perform check
  if (h > 99 || m > 99 || s > 99 || ms > 999) {
    // invalid values
    return -1;
  }
  return (3600 * h + 60 * m + s) * 1000 + ms;
} // parse_srt_time


// mp4v2 library entry points

/**
 * Opens QT (or any other file that has the same structure as MP4) and returns
 * file handle.
 */
MP4FileHandle open_qt_file(char* qt_file_path, int verbosity)
{
  MP4FileHandle handle = MP4Modify(qt_file_path, 0, 0);
  if (handle == MP4_INVALID_FILE_HANDLE) {
    fprintf(stderr, "Error: Could not open %s as QuickTime movie\n", qt_file_path);
    return NULL;
  }
  MP4SetVerbosity(handle, verbosity);
  return handle;
} // open_qt_file


/**
 * Creates subtitle track with all required properties. At least one video
 * stream is required to determine track's width. Returns track id or 0
 * (invalid track id).
 */
MP4TrackId create_subtitle_track(MP4FileHandle handle, char* language,
  unsigned int track_height, unsigned int font_height,
  unsigned int bottom_offset)
{
  // video height/width
  uint16_t width = 0;
  uint16_t height = 0;

  // subtitle already exists -> do not mark new track as default
  int sbtl_exists = 0;

  // find video track and previous subtitle track
  uint32_t num_track = MP4GetNumberOfTracks(handle, NULL, 0);
  uint16_t ii;
  for (ii = 0; ii < num_track; ii++) {
    MP4TrackId id = MP4FindTrackId(handle, ii, (const char*)NULL, 0);
    const char* track_type = MP4GetTrackType(handle, id);
    if (strcmp(MP4_VIDEO_TRACK_TYPE, track_type) == 0) {
      // found video track - set width, height
      width = MP4GetTrackVideoWidth(handle, id);
      height = MP4GetTrackVideoHeight(handle, id);
    } else if (strcmp(MP4_SUBTITLE_TRACK_TYPE, track_type) == 0) {
      // found subtitle track
      sbtl_exists = 1;
    }
  }

  if (width == 0 || height == 0) {
    // no valid video track found - exit with error code
    fprintf(stderr, "Error: No video tracks found\n");
    return 0;
  }

  MP4TrackId track = MP4AddSubtitleTrack(handle, 90000, width, track_height);
  if (track == MP4_INVALID_TRACK_ID) {
    fprintf(stderr, "Error: Could not create subtitle track\n");
    return 0;
  }

  // set language
  MP4SetTrackLanguage(handle, track, language);

  // set subtitle alternate group
  // TODO scan for previous alternate group
  MP4SetTrackIntegerProperty(handle, track, "tkhd.alternate_group", 3);

  // calculate offset from top
  uint16_t offset = height - track_height - bottom_offset;
  // prepaire matrix
  uint8_t matrix[36] = { 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    (uint8_t) ((offset >> 8) & 0xff), (uint8_t) (offset & 0xff), 0x00,
    0x00, 0x40, 0x00, 0x00, 0x00, };

  // set matrix
  MP4SetTrackBytesProperty(handle, track, "tkhd.matrix", matrix, 36);

  // set track properties
  if (!sbtl_exists) {
    // make this subtitle track default
    MP4SetTrackIntegerProperty(handle, track, "tkhd.flags", (TRACK_ENABLED | TRACK_IN_MOVIE));
  } else {
    MP4SetTrackIntegerProperty(handle, track, "tkhd.flags", (TRACK_DISABLED | TRACK_IN_MOVIE));
  }

  // set text properties
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.dataReferenceIndex", 1);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.horizontalJustification", 1);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.verticalJustification", 0);

  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.bgColorAlpha", 255);

  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.defTextBoxBottom", height);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.defTextBoxRight", width);

  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontID", 1);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontSize", font_height);

  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontColorRed", 255);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontColorGreen", 255);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontColorBlue", 255);
  MP4SetTrackIntegerProperty(handle, track,
    "mdia.minf.stbl.stsd.tx3g.fontColorAlpha", 255);

  return track;
} // create_subtitle_track


/**
 * Adds one caption to the QT file. Will fail if 'MP4WriteSample' fails.
 */
int add_caption_block(MP4FileHandle handle, MP4TrackId track_id, char* caption,
  unsigned int duration)
{
  int cp_len = strlen(caption);
#if 0
  if (cp_len > 0)
    fprintf(stdout, "Debug: Adding caption block: %s\n", caption);
#endif
  if (cp_len > 2046) {
    fprintf(stderr, "Warning: caption too long. Truncating...\n");
    cp_len = 2046;
  }
  uint8_t output[2 * (BUFFER_SIZE+1) + 2];
  memcpy(output + 2, caption, cp_len);
  output[0] = (cp_len >> 8) & 0xff;
  output[1] = cp_len & 0xff;
  output[cp_len + 2] = '\0';
  return MP4WriteSample(handle, track_id, output, cp_len + 2, duration * (90000 / 1000), 0, 1);
} // add_caption_block


/**
 * Removes track from QT file. This does not remove data already written to data
 * section of QT file. To remove this data as well, optimization has to be performed.
 */
void remove_track(MP4FileHandle handle, MP4TrackId track_id)
{
  MP4DeleteTrack(handle, track_id);
} // remove_track


/**
 * Closes QT file.
 */
void close_qt_file(MP4FileHandle handle)
{
  MP4Close(handle);
} // close_qt_file


/**
 * Optimize QT file. This includes moving all file information to beginning of the file,
 * removing all unnecessary data in the file and interleaving samples for faster access.
 */
void optimize_qt_file(char* filename)
{
  fprintf(stdout, "Info: Optimizing QT file %s\n", filename);
  if (!MP4Optimize(filename, NULL, 0)) {
    fprintf(stderr, "Warning: Optimization of %s failed\n", filename);
  }
} // optimize_qt_file


// General

/**
 * Parses command line and constructs structure that holds all arguments.
 * Returns NULL if parsing fails.
 */
program_parameters* parse_parameters(int argc, char* argv[])
{
  if (argc < 3) {
    fprintf(stderr, "Error: Arguments missing\n");
    print_usage(argv[0]);
    return NULL;
  }

  // default param init
  program_parameters* parameters = (program_parameters*) malloc(sizeof(program_parameters));
  if (parameters == NULL) {
    fprintf(stderr, "Error: Out of memory\n");
    return NULL;
  }
  parameters -> qt_filepath = NULL;
  parameters -> output_file = NULL;
  parameters -> track_height = 50;
  parameters -> font_height = 24;
  parameters -> bottom_offset = 0;
  parameters -> optimize = 1;
  parameters -> verbosity = 0;
  parameters -> sub_number = 0;

  int sub_buffer_size = 10;
  int error_flag = 0;

  int mode;
  if (argv[1][0] == '-') {
    mode = 1;
  } else {
    mode = 2;
  }

  int ii;
  for (ii = 1; ii < argc && error_flag == 0; ii++) {
    switch (mode) {
    case 1: {
      // switch mode
      if (argv[ii][0] == '-' && ii < argc - 1) {
        if (strcmp(argv[ii], "-fonth") == 0) {
          int font_height = atoi(argv[ii + 1]);
          if (font_height == 0) {
            fprintf(stderr, "Error: Invalid argument for -fonth: %s\n", argv[ii + 1]);
            error_flag = 1;
          } else {
            parameters -> font_height = font_height;
            ii++;
          }

        } else if (strcmp(argv[ii], "-trackh") == 0) {
          int track_height = atoi(argv[ii + 1]);
          if (track_height == 0) {
            fprintf(stderr, "Error: Invalid argument for -trackh: %s\n", argv[ii + 1]);
            error_flag = 1;
          } else {
            parameters -> track_height = track_height;
            ii++;
          }

        } else if (strcmp(argv[ii], "-offset") == 0) {
          int bottom_offset = atoi(argv[ii + 1]);
          if (bottom_offset == 0) {
            fprintf(stderr, "Error: Invalid argument for -offset: %s\n", argv[ii + 1]);
            error_flag = 1;
          } else {
            parameters -> bottom_offset = bottom_offset;
            ii++;
          }

        } else if (strcmp(argv[ii], "-opt") == 0) {
          if (*argv[ii + 1] == 'Y' || *argv[ii + 1] == 'y') {
            parameters -> optimize = 1;
            ii++;
          } else if (*argv[ii + 1] == 'N' || *argv[ii + 1] == 'n') {
            parameters -> optimize = 0;
            ii++;
          } else {
            fprintf(stderr, "Error: Invalid argument for -opt: %s\n", argv[ii + 1]);
            error_flag = 1;
          }

        } else if (strcmp(argv[ii], "-verbose") == 0) {
          parameters -> verbosity = MP4_DETAILS_ALL;

        } else if (strcmp(argv[ii], "-out") == 0) {
          parameters -> output_file = argv[ii + 1];
          ii++;

        } else {
          // unrecognized switch
          fprintf(stderr, "Error: Unrecognized switch: %s\n", argv[ii]);
          error_flag = 1;
          break;
        }

      } else if (argv[ii][0] != '-') {
        // switch to mode 2
        mode = 2;
        ii--;
      } else {
        // error
        fprintf(stderr, "Error: Invalid command line syntax\n");
        error_flag = 1;
        break;
      }
      break;
    }
    case 2: {
      // qt file mode
      parameters -> qt_filepath = argv[ii];
      mode = 3;
      break;
    }
    case 3: {
      // subtitle files mode
      // parse <path>@<lang>
      char* sub_path = strtok(argv[ii], "@");
      char* lang = strtok(NULL, "@");
      if (lang == NULL) {
        // default language
        lang = (char*) "eng";
      }
      if (strtok(NULL, "@") != NULL) {
        fprintf(stderr,
          "Error: Subtitle parameter %s is not correctly formated\n", argv[ii]);
        error_flag = 1;
        break;
      } else if (strlen(lang) != 3) {
        fprintf(stderr,
          "Error: Language parameter in %s has to be a 3 letter ISO 639 language code\n",
          argv[ii]);
        error_flag = 1;
        break;
      } else {
        // subtitle file
        int jj = parameters -> sub_number;

        if (jj >= MAX_SBTL) {
          fprintf(stderr,
            "Error: Too many subtitle files specified (max %d)\n", MAX_SBTL);
          error_flag = 1;
          break;
        }

        parameters -> subtitle_files[jj] = (sub_file*) malloc(sizeof(sub_file));
        if (parameters -> subtitle_files[jj] == NULL) {
          fprintf(stderr, "Error: Out of memory\n");
          error_flag = 1;
          break;
        }

        char* p = (char*) malloc(strlen(sub_path)+1);
        if (p == NULL) {
          fprintf(stderr, "Error: Out of memory\n");
          error_flag = 1;
          break;
        }
        strcpy(p, sub_path);
        (parameters -> subtitle_files[jj]) -> filepath = p;

        char* s = (char*) malloc(strlen(lang)+1);
        if (s == NULL) {
          fprintf(stderr, "Error: Out of memory\n");
          error_flag = 1;
          break;
        }
        strcpy(s, lang);
        (parameters -> subtitle_files[jj]) -> language = s;

        (parameters -> subtitle_files[jj]) -> valid = 0;

        parameters -> sub_number++;
      }
      break;
    }
    } // switch (mode)
  } // for

  if (error_flag == 0 && parameters -> qt_filepath == NULL) {
    fprintf(stderr, "Error: Missing QT file parameter\n");
    error_flag = 1;
  }
  if (error_flag == 0 && parameters -> sub_number == 0) {
    fprintf(stderr, "Error: No subtitle files to embedd\n");
    error_flag = 1;
  }

  // cleanup if error flag set
  if (error_flag == 1) {
    for (int kk = 0; kk < parameters -> sub_number; kk++)
      free(parameters -> subtitle_files[kk]);
    free(parameters);
    return NULL;
  }

  return parameters;
} // parse_parameters


/**
 * Prints out application usage.
 */
void print_usage(char* program_name) {
  printf("Usage: %s [options] qt_file srt_file@lng [srt_file2@lng ...]\n\n",
    program_name);
  printf("Options:\n");
  printf("  -fonth <num>     set font height (default 24)\n");
  printf("  -trackh <num>    set track height (default 50)\n");
  printf("  -offset <num>    set offset from bottom (default 0)\n");
  printf("  -opt <y|n>       should QT file be optimized (default y)\n");
  printf("  -out <filename>  write to output file (default is overwrite input)\n");
  printf("  -verbose         print detailed MP4 diagnostic information\n");
  printf("  --help           displays this message\n\n");
  printf("Note: Each SRT file must be in the following form: srt_filepath@srt_language\n");
  printf("      SRT language is a 3 letter language code as defined by ISO 639-2.\n");
  printf("      If language is not specified, \"eng\" is taken as default.\n");
}
