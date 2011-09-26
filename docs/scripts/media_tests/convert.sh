#!/bin/bash
###
##  Copyright 2009, 2010 The Regents of the University of California
##  Licensed under the Educational Community License, Version 2.0
##  (the "License"); you may not use this file except in compliance
##  with the License. You may obtain a copy of the License at
##
##  http://www.osedu.org/licenses/ECL-2.0
##
##  Unless required by applicable law or agreed to in writing,
##  software distributed under the License is distributed on an "AS IS"
##  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
##  or implied. See the License for the specific language governing
##  permissions and limitations under the License.
##
###

function usage {
  echo "convert.sh [-a audio file][-v camera video file][-s slide(vga) video file]"

}
VIDEO=""
AUDIO=""
SLIDE=""
while getopts “hv:a:s:” OPTION
do
	case $OPTION in
	h)
	  usage
	  exit 1
	  ;;
	v)
	  VIDEO=$OPTARG
	  ;;
	a)
	  AUDIO=$OPTARG
	  ;;
	s)
	  SLIDE=$OPTARG
	  ;;
	?)
	  usage
	  exit
	  ;;
	esac
done
if [ -d "output" ]
then
  echo "Overwriting old files"
else
  mkdir output
fi
#convert audio
if [ -f "$AUDIO" ]
then
  echo "Converting Audio File $AUDIO"
  ffmpeg -y -i "$AUDIO" -acodec libmp3lame -sameq "output/a-mp3.mp3"
  ffmpeg -y -i "$AUDIO" -acodec libfaac -sameq "output/a-aac.m4a"
  ffmpeg -y -i "$AUDIO" -acodec pcm_s16le -sameq "output/a-wav.wav"
  ffmpeg -y -i "$AUDIO" -acodec libvorbis -ac2 -sameq "output/a-vorbis.ogg"
  ffmpeg -y -i "$AUDIO" -acodec wmav2 -sameq "output/a-wma.wma"
fi
#convert video
if [ -f "$VIDEO" ]
then
  echo "Converting Lecture Video File $VIDEO"
  ffmpeg -y -i $VIDEO -vcodec libx264 -vpre medium -b 4000k "output/v-h264.mp4"          #mp4 part 10
  ffmpeg -y -i $VIDEO -vcodec mpeg2video -sameq "output/v-mpeg2.mp4"      #mpeg2
  ffmpeg -y -i $VIDEO -vcodec libxvid -sameq "output/v-xvid.mp4"          #mp4 part 2
  ffmpeg -y -i $VIDEO -vcodec libx264 -vpre medium -b 4000k "output/v-h264.mov"          #mov h264
  ffmpeg -y -i $VIDEO -vcodec libtheora -b 4000k "output/v-theora.ogg"      #theora
  ffmpeg -y -i $VIDEO -vcodec flv -sameq "output/v-h263.flv"              #flv h263
  ffmpeg -y -i $VIDEO -vcodec libx264 -vpre medium -b 4000k "output/v-h264.flv"             #flv h264
  ffmpeg -y -i $VIDEO -vcodec libvpx -b 2000k "output/v-vp8.mkv"            #webm 
  ffmpeg -y -i $VIDEO -vcodec wmv2 -sameq "output/v-wmv.wmv"              #wmv8
  #mux step
  if [ -f "$AUDIO" ]
  then
    echo "muxing video and audio files"
    ffmpeg -y -i "output/v-wmv.wmv" -vcodec copy -i "output/a-wma.wma" -acodec copy "output/v-wmv.a-wma.wmv"
    ffmpeg -y -i "output/v-theora.ogg" -vcodec copy  -i "output/a-vorbis.ogg" -acodec copy "output/v-theora.a-vorbis.ogg"

    ffmpeg -y -i "output/v-h264.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-h264.a-mp3.mp4"
    ffmpeg -y -i "output/v-h264.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-h264.a-aac.mp4"
    ffmpeg -y -i "output/v-h264.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-h264.a-wav.mp4"

    ffmpeg -y -i "output/v-mpeg2.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-mpeg2.a-mp3.mp4"
    ffmpeg -y -i "output/v-mpeg2.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-mpeg2.a-aac.mp4"
    ffmpeg -y -i "output/v-mpeg2.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-mpeg2.a-wav.mp4"

    ffmpeg -y -i "output/v-xvid.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-xvid.a-mp3.mp4"
    ffmpeg -y -i "output/v-xvid.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-xvid.a-aac.mp4"
    ffmpeg -y -i "output/v-xvid.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-xvid.a-wav.mp4"

    ffmpeg -y -i "output/v-h264.mov" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-h264.a-mp3.mov"
    ffmpeg -y -i "output/v-h264.mov" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-h264.a-aac.mov"
    ffmpeg -y -i "output/v-h264.mov" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-h264.a-wav.mov"

    ffmpeg -y -i "output/v-h263.flv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-h263.a-mp3.flv"
    ffmpeg -y -i "output/v-h263.flv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-h263.a-aac.flv"
    ffmpeg -y -i "output/v-h263.flv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-h263.a-wav.flv"

    ffmpeg -y -i "output/v-h264.flv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-h264.a-mp3.flv"
    ffmpeg -y -i "output/v-h264.flv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-h264.a-aac.flv"
    ffmpeg -y -i "output/v-h264.flv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-h264.a-wav.flv"

    ffmpeg -y -i "output/v-vp8.mkv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/v-vp8.a-mp3.mkv"
    ffmpeg -y -i "output/v-vp8.mkv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/v-vp8.a-aac.mkv"
    ffmpeg -y -i "output/v-vp8.mkv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/v-vp8.a-wav.mkv"
  fi
fi
#convert slide
if [ -f "$SLIDE" ]
then
  echo "Converting Slide Video File $SLIDE"
  ffmpeg -y -i $SLIDE -vcodec libx264 -vpre medium -b 4000k "output/s-h264.mp4"          #mp4 part 10
  ffmpeg -y -i $SLIDE -vcodec mpeg2video -sameq "output/s-mpeg2.mp4"      #mpeg2
  ffmpeg -y -i $SLIDE -vcodec libxvid -sameq "output/s-xvid.mp4"          #mp4 part 2
  ffmpeg -y -i $SLIDE -vcodec libx264 -vpre medium -b 4000k "output/s-h264.mov"    #mov h264
  ffmpeg -y -i $SLIDE -vcodec libtheora -b 4000k "output/s-theora.ogg"      #theora
  ffmpeg -y -i $SLIDE -vcodec flv -sameq "output/s-h263.flv"              #flv h263
  ffmpeg -y -i $SLIDE -vcodec libx264 -vpre medium -b 4000k "output/s-h264.flv"             #flv h264
  ffmpeg -y -i $SLIDE -vcodec libvpx -b 2000k"output/s-vp8.mkv"            #webm
  ffmpeg -y -i $SLIDE -vcodec wmv2 -sameq "output/s-wmv.wmv"              #wmv8
  #mux step
  if [ -f "$AUDIO" ]
  then
    echo "muxing slides and audio files"
    ffmpeg -y -i "output/s-wmv.wmv" -vcodec copy -i "output/a-wma.wma" -acodec copy "output/s-wmv.a-wma.wmv"
    ffmpeg -y -i "output/s-theora.ogg" -i "output/a-vorbis.ogg" -map 0:0 -map 1:0 "output/s-theora.a-vorbis.ogg"

    ffmpeg -y -i "output/s-h264.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-h264.a-mp3.mp4"
    ffmpeg -y -i "output/s-h264.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-h264.a-aac.mp4"
    ffmpeg -y -i "output/s-h264.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-h264.a-wav.mp4"

    ffmpeg -y -i "output/s-mpeg2.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-mpeg2.a-mp3.mp4"
    ffmpeg -y -i "output/s-mpeg2.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-mpeg2.a-aac.mp4"
    ffmpeg -y -i "output/s-mpeg2.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-mpeg2.a-wav.mp4"

    ffmpeg -y -i "output/s-xvid.mp4" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-xvid.a-mp3.mp4"
    ffmpeg -y -i "output/s-xvid.mp4" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-xvid.a-aac.mp4"
    ffmpeg -y -i "output/s-xvid.mp4" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-xvid.a-wav.mp4"

    ffmpeg -y -i "output/s-h264.mov" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-h264.a-mp3.mov"
    ffmpeg -y -i "output/s-h264.mov" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-h264.a-aac.mov"
    ffmpeg -y -i "output/s-h264.mov" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-h264.a-wav.mov"

    ffmpeg -y -i "output/s-h263.flv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-h263.a-mp3.flv"
    ffmpeg -y -i "output/s-h263.flv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-h263.a-aac.flv"
    ffmpeg -y -i "output/s-h263.flv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-h263.a-wav.flv"

    ffmpeg -y -i "output/s-h264.flv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-h264.a-mp3.flv"
    ffmpeg -y -i "output/s-h264.flv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-h264.a-aac.flv"
    ffmpeg -y -i "output/s-h264.flv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-h264.a-wav.flv"

    ffmpeg -y -i "output/s-vp8.mkv" -vcodec copy  -i "output/a-mp3.mp3" -acodec copy "output/s-vp8.a-mp3.mkv"
    ffmpeg -y -i "output/s-vp8.mkv" -vcodec copy  -i "output/a-aac.m4a" -acodec copy "output/s-vp8.a-aac.mkv"
    ffmpeg -y -i "output/s-vp8.mkv" -vcodec copy  -i "output/a-wav.wav" -acodec copy "output/s-vp8.a-wav.mkv"
  fi
fi

exit 0;
