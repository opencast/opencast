// Copyright 2006-2007 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
// or its licensors, as applicable.
//
// You may not use this file except under the terms of the accompanying license.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you
// may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Project: ocr-tesseract
// File: tesseract.cc
// Purpose: interfaces to Tesseract
// Responsible: mezhirov
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

#ifndef UNSAFE
#define ISOLATE_TESSERACT
#endif

#include <signal.h>
#include <string>  // otherwise `baseapi.h' will break CODE-OK--mezhirov

// Tess includes
#include "tesseract/tordvars.h"
#include "tesseract/control.h"
#include "tesseract/tessvars.h"
#include "tesseract/tessbox.h"
#include "tesseract/tessedit.h"
#include "tesseract/imgs.h"
#include "tesseract/edgblob.h"
#include "tesseract/makerow.h"
#include "tesseract/wordseg.h"
#include "tesseract/output.h"
#include "tesseract/tstruct.h"
#include "tesseract/tessout.h"
#include "tesseract/tface.h"
#include "tesseract/adaptmatch.h"
#include "tesseract/baseapi.h"
#include "tesseract/globals.h"

#ifdef GOOGLE_INTERNAL
#include "ccutil.h"
#include "unicharset.h"
using namespace tesseract;
#endif

// these Tess-defined macros interfere with IUPR names
#undef rectangle
#undef min
#undef max

// IUPR includes
#include "ocropus.h"
#include "ocr-tesseract.h"

using namespace colib;
using namespace iulib;
using namespace ocropus;

namespace ocropus {
    param_string tesslanguage("tesslanguage", "eng", "Specify the language for Tesseract");
}

extern BOOL_VAR_H(textord_ocropus_mode, FALSE, "Make baselines for ocropus");

namespace {
    Logger log_tesseract("tesseract");
    Logger log_split("tesseract.split");
    static int area(rectangle rect) {
        if (rect.width() <= 0 || rect.height() <= 0)
            return 0;
        return rect.width() * rect.height();
    }

    int arg_max_overlap(narray<rectangle> &line_boxes, rectangle char_box) {
        int best_so_far = -1;
        int best_overlap = 0;
        for(int i = 0; i < line_boxes.length(); i++) {
            int overlap = area(line_boxes[i].intersection(char_box));
            if(overlap > best_overlap) {
                best_so_far = i;
                best_overlap = overlap;
            }
        }
        return best_so_far;
    }

    void extract_bboxes(narray<rectangle> &result, RegionExtractor &e) {
        result.resize(max(e.length() - 1, 0));
        for(int i = 1; i < e.length(); i++)
            result[i-1] = rectangle(e.x0(i),e.y0(i),e.x1(i),e.y1(i));
    }

    /*static bool fits_into(rectangle inner, rectangle outer) {
        if (outer.empty())
            return false;
        if (inner.empty())
            return false;
        rectangle common = inner.intersection(outer);
        return 2 * area(common) > area(inner);
    }*/

    template<class T>
    void fill_rectangle(narray<T> &a, rectangle r, T value) {
        r.intersect(rectangle(0, 0, a.dim(0), a.dim(1)));
        for(int x = r.x0; x < r.x1; x++)
            for(int y = r.y0; y < r.y1; y++)
                a(x,y) = value;
    }

    // produce a crude segmentation by simply coloring bounding boxes
    void color_boxes(intarray &segmentation, rectarray &bboxes) {
        fill(segmentation, 0);
        for(int i = 0; i < bboxes.length(); i++)
            fill_rectangle(segmentation, bboxes[i], i + 1);
    }

    void fill_lattice(IGenericFst &lattice, nustring &text) {
        floatarray costs;
        intarray ids;
        makelike(costs, text);
        makelike(ids, text);
        for(int i = 0; i < text.length(); i++) {
            costs[i] = 1;
            ids[i] = i + 1;
        }
        lattice.setString(text, costs, ids);
    }

    rectangle unite_rectangles(narray<rectangle> &rects) {
        rectangle result;
        for(int i = 0; i < rects.length(); i++) {
            if(!rects[i].empty())
                result.include(rects[i]);
        }
        return result;
    }
};


namespace ocropus {
    colib::rectangle RecognizedPage::bbox(int index) {
        return unite_rectangles(line_bboxes[index]);
    }


    enum {MIN_HEIGHT = 30};

    class TesseractWrapper;
    TesseractWrapper *tesseract_singleton = NULL;


    void oops_tesseract_died(int signal) {
        fprintf(stderr, "ERROR: got signal from Tesseract (bug in Tesseract?)\n");
        exit(1);
    }

#ifdef ISOLATE_TESSERACT
    static bool inside_tesseract = false;
static struct sigaction SIGSEGV_old;
    static struct sigaction SIGFPE_old;
    static struct sigaction SIGABRT_old;
    static struct sigaction oops_sigaction;
#endif

    void enter_tesseract() {
#ifdef ISOLATE_TESSERACT
        ASSERT(!inside_tesseract);
        inside_tesseract = true;
        oops_sigaction.sa_handler = oops_tesseract_died;
        sigemptyset(&oops_sigaction.sa_mask);
        oops_sigaction.sa_flags = 0;
        sigaction(SIGSEGV, &oops_sigaction, &SIGSEGV_old);
        sigaction(SIGFPE,  &oops_sigaction, &SIGFPE_old);
        sigaction(SIGABRT, &oops_sigaction, &SIGABRT_old);
#endif
    }

    void leave_tesseract() {
#ifdef ISOLATE_TESSERACT
        ASSERT(inside_tesseract);
        sigaction(SIGSEGV, &SIGSEGV_old, NULL);
        sigaction(SIGFPE,  &SIGFPE_old, NULL);
        sigaction(SIGABRT, &SIGABRT_old, NULL);
        inside_tesseract = false;
#endif
    }

    static void set_line_number(intarray &a, int lnum) {
        lnum <<= 12;
        for(int i = 0; i < a.length1d(); i++) {
            if (a.at1d(i) && a.at1d(i) != 0xFFFFFF)
                a.at1d(i) = (a.at1d(i) & 0xFFF) | lnum;
        }
    }

    static int pick_threshold(intarray &segmentation, bytearray &image, int k) {
        int min = 255;//, max = 0;
        int n = segmentation.length1d();
        for(int i = 0; i < n; i++) {
            if(segmentation.at1d(i) == k) {
                int pixel = image.at1d(i);
                if(pixel < min) min = pixel;
                //if(pixel > max) max = pixel;
            }
        }
        if(min > 128)
            return min;
        else
            return 128;
    }

    static void binarize_in_segmentation(intarray &segmentation, bytearray &gray_image) {
        CHECK_ARG(samedims(segmentation, gray_image));
        // should be passed a valid segmentation
        check_line_segmentation(segmentation);
        int n = segmentation.length1d();
        intarray thresholds(1);
        for(int i = 0; i < n; i++) {
            int c = segmentation.at1d(i);
            if(c == 0 || c == 0xFFFFFF)
                continue;
            c &= 0xFFF; // clear the line number
            while(c >= thresholds.length()) {
                thresholds.push(pick_threshold(segmentation,
                                               gray_image,
                                               thresholds.length()));
            }
            if(gray_image.at1d(i) > thresholds[c])
                 segmentation.at1d(i) = 0xFFFFFF;
        }
        // should return a valid segmentation
        check_line_segmentation(segmentation);
    }


    ROW *tessy_make_ocrrow(float baseline, float xheight, float descender, float ascender) {
        int xstarts[] = {-32000};
        double quad_coeffs[] = {0,0,baseline};
        return new ROW(
            1,
            xstarts,
            quad_coeffs,
            xheight,
            ascender - (baseline + xheight),
            descender - baseline,
            0,
            0
            );
    }


// _______________________   getting Tesseract output   _______________________


/// Convert Tess rectangle to IUPR one
/// These (-1)s are strange; but they work on c_blobs' bboxes.
    /*rectangle tessy_rectangle(const BOX &b) {
        return rectangle(b.left() - 1, b.bottom() - 1, b.right() - 1, b.top() - 1);
    }*/


    static int counter = 0;
    inline double sqr(double x) {return x * x;}
    class TesseractWrapper : TessBaseAPI /* , public ISimpleLineOCR */{
        // tesseract_ is an instance of Tesseract; use that for everything
        int pass;
        bytearray pixels;

        void pass_grayscale_image_to_tesseract(bytearray &image) {
            CHECK_ARG2(image.dim(0)>0 && image.dim(1)>0,"must provide non-empty image to Tesseract");
            int n = image.length1d();
            pixels.resize(n);
            for(int x=0;x<image.dim(0);x++) for(int y=0;y<image.dim(1);y++)
                pixels[(image.dim(1) - y - 1) * image.dim(0) + x] = image(x,y);

#ifdef GOOGLE_INTERNAL
            // SetImage does not copy the pixels
            SetImage(&pixels[0],image.dim(0),image.dim(1),1,image.dim(0));
            SetRectangle(0,0,image.dim(0),image.dim(1));
#else
            CopyImageToTesseract(&pixels[0], 1, image.dim(0), 0, 0, image.dim(0), image.dim(1));
            // CopyImageToTesseract copies the pixels
            pixels.resize(0);
#endif
        }


        void extract_result_from_PAGE_RES(nustring &str,
                                          narray<rectangle> &bboxes,
                                          floatarray &costs,
                                          PAGE_RES &page_res) {
            char *string;
            int *lengths;
            float *tess_costs;
            int *x0;
            int *y0;
            int *x1;
            int *y1;
            int n = TesseractExtractResult(&string, &lengths, &tess_costs,
                                           &x0, &y0, &x1, &y1,
                                           &page_res);
            // now we'll have to cope with different multichar handling by us
            // and by Tesseract. All this is way too ugly and I hope it'll be
            // better eventually. I would vote for making nuchar doing what
            // nustring now does - I.M.
            int offset = 0;
            for(int i = 0; i < n; i++) {
                nustring multichar; // the multichar sequence of the glyph
                multichar.utf8Decode(string + offset, lengths[i]);
                offset += lengths[i];

                // copy bboxes for each subcomponent, split the cost
                for(int j = 0; j < multichar.length(); j++) {
                    str.push(multichar[j]);
                    rectangle &bbox = bboxes.push();
                    bbox.x0 = x0[i] - 1; // don't ask me why -1;
                    bbox.y0 = y0[i] - 1; // I don't remember any padding --IM
                    bbox.x1 = x1[i] - 1;
                    bbox.y1 = y1[i] - 1;
                    costs.push(tess_costs[i] / multichar.length());
                }
            }
            delete [] string;
            delete [] lengths;
            delete [] tess_costs;
            delete [] x0;
            delete [] y0;
            delete [] x1;
            delete [] y1;
        }

    public:
        void adapt(bytearray &image, int truth, float baseline, float xheight, float descender, float ascender) {
            pass_grayscale_image_to_tesseract(image);
            nustring text;
            text.push(nuchar(truth));
            char buf[20];
            text.utf8Encode(buf, sizeof(buf));
#ifdef GOOGLE_INTERNAL
            if(!tesseract_->unicharset.contains_unichar(buf))
#else
            if(!unicharset.contains_unichar(buf))
#endif
            {
                //printf("Ouch! Character %s (%d) isn't known!\n", buf, truth);
                return;
            }
            AdaptToCharacter(buf, strlen(buf),
                             baseline, xheight, descender, ascender);
        }



        virtual const char *description() {
            return "a wrapper around Tesseract";
        }
        virtual void init(const char **argv=0) {
        }

        TesseractWrapper(const char *path_to_us) : pass(1) {
            if (!counter) {
#ifdef GOOGLE_INTERNAL
                if(!getenv("TESSDATA_PREFIX"))
                    throw "must set TESSDATA_PREFIX in the environment";
                Init(path_to_us, tesslanguage);
#else
                InitWithLanguage(path_to_us, NULL, tesslanguage, NULL, false, 0, NULL);
#endif

                // doesn't seem to do anything any longer
                //textord_ocropus_mode.set_value(true);

                set_pass1();
            }
            counter++;
        }

        virtual ~TesseractWrapper() {
            counter--;
            ClearAdaptiveClassifier();
            if (!counter) {
                End();
            }
        }

        virtual void recognize_gray(nustring &result,
                                    floatarray &costs,
                                    narray<rectangle> &bboxes,
                                    bytearray &input_image) {

            enter_tesseract();
            bytearray image;
            copy(image, input_image);
            enum {PADDING = 3};
            pad_by(image, PADDING, PADDING, byte(255));
            pass_grayscale_image_to_tesseract(image);
#ifdef GOOGLE_INTERNAL
            ClearResults();
            Threshold();
            FindLines();
            BLOCK_LIST &blocks = *block_list_;
#else
            BLOCK_LIST blocks;
            FindLines(&blocks);
#endif
            if(getenv("TESS_DUMP")) DumpPGM(getenv("TESS_DUMP"));

            narray<autodel<WERD> > bln_words;

            // Recognize all words
            PAGE_RES page_res(&blocks);
            PAGE_RES_IT page_res_it(&page_res);
            while (page_res_it.word () != NULL) {
                WERD_RES *word = page_res_it.word();
                ROW *row = page_res_it.row()->row;

                matcher_pass = 0;
                WERD *bln_word = make_bln_copy(word->word, row, row->x_height(),
                                               &word->denorm);
                bln_words.push() = bln_word;
                BLOB_CHOICE_LIST_CLIST blob_choices;
#ifdef GOOGLE_INTERNAL
#define tess_default_matcher &Tesseract::tess_default_matcher
#define tess_segment_pass1 tesseract_->tess_segment_pass1
#define tess_segment_pass2 tesseract_->tess_segment_pass2
#endif
                if (pass == 1) {
                    word->best_choice = tess_segment_pass1(bln_word, &word->denorm,
                                                            tess_default_matcher,
                                                            word->raw_choice, &blob_choices,
                                                            word->outword);
                } else {
                    word->best_choice = tess_segment_pass2(bln_word, &word->denorm,
                                                            tess_default_matcher,
                                                            word->raw_choice, &blob_choices,
                                                            word->outword);
                }

#ifdef GOOGLE_INTERNAL
#undef tess_default_matcher
#undef tess_segment_pass1
#undef tess_segment_pass2
#endif
                //classify_word_pass1 (page_res_it.word(), page_res_it.row()->row,
                //FALSE, NULL, NULL);

                page_res_it.forward();
            }
            extract_result_from_PAGE_RES(result, bboxes, costs, page_res);

            // Correct the padding.
            for(int i = 0; i < bboxes.length(); i++) {
                bboxes[i].x0 += -PADDING;
                bboxes[i].y0 += -PADDING;
                bboxes[i].x1 += -PADDING;
                bboxes[i].y1 += -PADDING;
            }

            /*for(int i = 0; i < result.length(); i++) {
              printf("%c %d %d %d %d\n", result[i],
              bboxes[i].x0,
              input_image.dim(1) - bboxes[i].y0,
              bboxes[i].x1,
              input_image.dim(1) - bboxes[i].y1);
              }*/
            leave_tesseract();
        }


        virtual void recognize_binary(nustring &result,floatarray &costs,narray<rectangle> &bboxes,bytearray &orig_image) {
            recognize_gray(result, costs, bboxes, orig_image);
        }

        virtual void start_training() {
            pass = 2;
        }

        virtual bool supports_char_training() {
            return true;
        }

        void train(nustring &chars,intarray &orig_csegmentation) {
            intarray csegmentation;
            copy(csegmentation, orig_csegmentation);
            check_line_segmentation(csegmentation);
            make_line_segmentation_black(csegmentation);
            check_line_segmentation(csegmentation);
            set_line_number(csegmentation, 0);
            int n = max(csegmentation);
            ALWAYS_ASSERT(chars.length() >= n);
            float intercept;
            float slope;
            float xheight;
            float ascender_rise;
            float descender_sink;
            bytearray line;

            if (!get_extended_line_info(intercept, slope, xheight,
                                        descender_sink, ascender_rise, csegmentation)) {
                return;
            }

            bytearray bitmap;
            for (int i = 0; i < n; i++) {
                set_pass1(); // because Tesseract adapts on pass 1
                makelike(bitmap, csegmentation);
                // int n = csegmentation.length1d();
                rectangle bbox(0,0,-1,-1);
                for (int x = 0; x < bitmap.dim(0); x++)
                    for (int y = 0; y < bitmap.dim(1); y++) {
                        if (csegmentation(x,y) == i + 1) {
                            bitmap(x,y) = 0;
                            bbox.include(x,y);
                        } else {
                            bitmap(x,y) = 255;
                        }
                    }

                // Checking whether bbox is non-empty is a kluge.
                // It might be empty in the case of undersegmentation.
                // In this case, due to recoloring, only one
                // character will actually receive the segment.
                // Note that this situation probably suggests
                // that we'd better not train on this word.
                // But we still do. FIXME/mezhirov  --tmb
                if (bbox.width() > 0 && bbox.height() > 0)
                {
                    int center_x = (bbox.x0 + bbox.x1) / 2;
                    float baseline = intercept + center_x * slope ;

                    adapt(bitmap, chars[i].ord(), baseline, xheight,
                          baseline - descender_sink,
                          baseline + xheight + ascender_rise);
                }
            }
        }

        virtual void train_binary_chars(nustring &chars,intarray &csegmentation) {
            enter_tesseract();
            train(chars, csegmentation);
            leave_tesseract();
        }


        virtual void train_gray_chars(nustring &chars,intarray &csegmentation,bytearray &image) {
            enter_tesseract();
            check_line_segmentation(csegmentation);
            intarray new_segmentation;
            copy(new_segmentation, csegmentation);
            binarize_in_segmentation(new_segmentation, image);
            check_line_segmentation(new_segmentation);
            train(chars, new_segmentation);
            leave_tesseract();
        }

        virtual bool supports_line_training() {
            return false;
        }
        virtual void train_binary(nustring &chars,bytearray &bimage) {
            throw "TesseractWrapper: linewise training is not supported";
        }
        virtual void train_gray(nustring &chars,bytearray &image) {
            throw "TesseractWrapper: linewise training is not supported";
        }

        // TODO/mezhirov beautify
        void tesseract_recognize_blockwise(
            narray<rectangle> &zone_bboxes,
            narray<nustring> &text,
            narray<narray<rectangle> > &bboxes,
            narray<floatarray> &costs,
            bytearray &gray,
            intarray &pageseg) {

            RegionExtractor e;
            e.setPageColumns(pageseg);
            extract_bboxes(zone_bboxes, e);

            narray<BLOCK_LIST *> block_lists(e.length());
            narray<PAGE_RES *> block_results(e.length());
            fill(block_lists, static_cast<BLOCK_LIST *>(NULL));
            fill(block_results, static_cast<PAGE_RES *>(NULL));
            // pass 1
            for(int i = 1 /* RegionExtractor weirdness */; i < e.length(); i++) {
                bytearray block_image;
                if(gray.length1d()) {
                    e.extract(block_image, gray, i, /* margin: */ 1);
                    bytearray mask;
                    e.mask(mask, i, /* margin: */ 1);
                    log_tesseract("pass 1 block before cleaning", block_image);
                    optional_check_background_is_lighter(block_image);

                    bytearray dilated_mask;
                    copy(dilated_mask, mask);
                    binary_dilate_circle(dilated_mask, 3);

                    ASSERT(samedims(mask, block_image));
                    ASSERT(samedims(dilated_mask, block_image));
                    for(int k = 0; k < block_image.length1d(); k++) {
                        if(!dilated_mask.at1d(k))
                            block_image.at1d(k) = 255;
                    }
                } else {
                    e.mask(block_image, i, /* margin: */ 1);
                    invert(block_image);
                }
                log_tesseract("pass 1 block after cleaning", block_image);
                pass_grayscale_image_to_tesseract(block_image);
                block_lists[i-1] = TessBaseAPI::FindLinesCreateBlockList();
                block_results[i-1] = TessBaseAPI::RecognitionPass1(block_lists[i-1]);
            }

            int n = e.length() - 1;
            text.resize(n);
            bboxes.resize(n);
            costs.resize(n);

            // pass 2
            for(int i = 1 /* RegionExtractor weirdness */; i < e.length(); i++) {
                bytearray block_image;
                if(gray.length1d()) {
                    e.extract(block_image, gray, i, /* margin: */ 1);
                } else {
                    e.mask(block_image, i, /* margin: */ 1);
                    invert(block_image);
                }
                log_tesseract("pass 2 block", block_image);
                pass_grayscale_image_to_tesseract(block_image);
                block_results[i-1] = TessBaseAPI::RecognitionPass2(block_lists[i-1], block_results[i-1]);
                extract_result_from_PAGE_RES(text[i-1],
                                             bboxes[i-1],
                                             costs[i-1],
                                             *block_results[i-1]);
                DeleteBlockList(block_lists[i-1]);
            }
        }

        void tesseract_recognize_blockwise_and_split_to_lines(
                narray<nustring> &text,
                narray<narray<rectangle> > &bboxes,
                narray<floatarray> &costs,
                bytearray &gray,
                intarray &pseg) {

            // the output of tesseract_recognize_blockwise
            narray<rectangle> whole_zone_bboxes;
            narray<nustring> zone_text;
            narray<narray<rectangle> > zone_bboxes;
            narray<floatarray> zone_costs;

            RegionExtractor e;
            e.setPageLines(pseg);
            narray<rectangle> line_bboxes;
            extract_bboxes(line_bboxes, e);
            int nlines = max(e.length() - 1, 0);

            text.resize(nlines);
            bboxes.resize(nlines);
            costs.resize(nlines);

            narray<bool> pending_space(nlines);
            fill(pending_space, false);

            tesseract_recognize_blockwise(whole_zone_bboxes, zone_text,
                                          zone_bboxes, zone_costs, gray, pseg);

            for(int zone = 0; zone < whole_zone_bboxes.length(); zone++) {
                int line = -1;
                int nchars = zone_text[zone].length();
                for(int i = 0; i < nchars; i++) {
                    if(zone_text[zone][i].ord() == ' ') {
                        if(line != -1)
                            pending_space[line] = true;
                        continue;
                    }
                    rectangle abs_charbox = rectangle(
                        whole_zone_bboxes[zone].x0 + zone_bboxes[zone][i].x0,
                        whole_zone_bboxes[zone].y0 + zone_bboxes[zone][i].y0,
                        whole_zone_bboxes[zone].x0 + zone_bboxes[zone][i].x1,
                        whole_zone_bboxes[zone].y0 + zone_bboxes[zone][i].y1);
                    log_split("char rectangle (page coords)", abs_charbox);
                    line = arg_max_overlap(line_bboxes, abs_charbox);
                    log_split.format("char %d (%lc) gets into line %d", i,
                                     zone_text[zone][i].ord(),
                                     line);
                    if(line == -1) continue;
                    if(pending_space[line]) {
                        text[line].push(nuchar(' '));
                        bboxes[line].push(abs_charbox);
                        costs[line].push(1000);
                        pending_space[line] = false;
                    }
                    text[line].push(zone_text[zone][i]);
                    bboxes[line].push(abs_charbox);
                    costs[line].push(zone_costs[zone][i]);
                }
            }
        }

    };

    void tesseract_recognize_blockwise_and_split_to_lines(
            narray<nustring> &text,
            narray<narray<rectangle> > &bboxes,
            narray<floatarray> &costs,
            bytearray &gray,
            intarray &pseg) {
        autodel<TesseractWrapper> tess(new TesseractWrapper(""));
        tess->tesseract_recognize_blockwise_and_split_to_lines(text, bboxes, costs, gray, pseg);
    }

    void tesseract_recognize_blockwise_and_dump(bytearray &gray,
                                                intarray &pageseg) {
        autodel<TesseractWrapper> tess(new TesseractWrapper(""));
        //narray<rectangle> zone_boxes;
        narray<nustring> text;
        narray<narray<rectangle> > bboxes;
        narray<floatarray> costs;

        tess->tesseract_recognize_blockwise_and_split_to_lines(text, bboxes, costs, gray, pageseg);
        for(int zone = 0; zone < text.length(); zone++) {
            char *s = text[zone].newUtf8Encode();
            printf("[zone %d] %s\n", zone + 1, s);
            delete[] s;
        }
    }

    struct TesseractRecognizeLine : IRecognizeLine {
        autodel<TesseractWrapper> tess;
        autodel<ISegmentLine> lineseg;
        bool training;

        const char *description() {
            return "Tesseract Wrapper";
        }

        const char *name() {
            return "tessline";
        }

        TesseractRecognizeLine() {
            tess = new TesseractWrapper(0);
            lineseg = make_CurvedCutWithCcSegmenter();
            lineseg->set("min_thresh", 300);
            training = false;
        }

        virtual void recognizeLine(IGenericFst &result,bytearray &image) {
            nustring text;
            floatarray costs;
            rectarray bboxes;
            tess->recognize_gray(text, costs, bboxes, image);
            fill_lattice(result, text);
        }

        virtual void recognizeLine(intarray &segmentation,IGenericFst &result,bytearray &image) {
            nustring text;
            floatarray costs;
            rectarray bboxes;
            tess->recognize_gray(text, costs, bboxes, image);
            fill_lattice(result, text);

            makelike(segmentation, image);
            color_boxes(segmentation, bboxes);
            // FIXME?/mezhirov crude binarization --mezhirov
            bytearray binarized;
            binarize_simple(binarized, image);
            optional_check_background_is_lighter(binarized);

            for(int i = 0; i < segmentation.length1d(); i++) {
                if(binarized.at1d(i))
                    segmentation.at1d(i) = 0;
            }
        }

        virtual void align(nustring &chars,intarray &segmentation,floatarray &costs,bytearray &image,IGenericFst &transcription) {
            rectarray bboxes;
            tess->recognize_gray(chars, costs, bboxes, image);

            intarray overseg;
            lineseg->charseg(overseg, image);
            ocr_bboxes_to_charseg(segmentation, bboxes, overseg);
        }

    };



    IRecognizeLine *make_TesseractRecognizeLine() {
        return new TesseractRecognizeLine();
    }
}

namespace ocropus {
    void tesseract_recognize_blockwise(RecognizedPage &result, colib::bytearray &gray, colib::intarray &pageseg) {
        //double start = now();
        narray<nustring> text;
        narray<narray<rectangle> > bboxes;
        narray<floatarray> costs;
        tesseract_recognize_blockwise_and_split_to_lines(text, bboxes, costs, gray, pageseg);
        result.setWidth(gray.dim(0));
        result.setHeight(gray.dim(1));
        int nonempty_count = 0;
        for(int i = 0; i < text.length(); i++) {
            if(text[i].length())
                nonempty_count++;
        }

        result.setLinesCount(nonempty_count);
        int j = 0;
        for(int i = 0; i < text.length(); i++) {
            if(!text[i].length())
                continue;
            CHECK_CONDITION(bboxes.length() == text.length());
            result.setText(text[i], j);
            result.setBboxes(bboxes[i], j);
            result.setCosts(costs[i], j);
            j++;
        }
        // description
        // Time report
    }

#ifdef GOOGLE_INTERNAL
    bytearray tess_pixels;
    autodel<TessBaseAPI> tess_instance;
#endif

    void tesseract_init_with_language(const char *language) {
#ifdef GOOGLE_INTERNAL
        tess_instance = new TessBaseAPI();
        if(!getenv("TESSDATA_PREFIX"))
            throw "you must set TESSDATA_PREFIX when running in the Google environment";
        tess_instance->Init(0,language);
#else
        TessBaseAPI::InitWithLanguage(0,0,language,0,false,0,0);
#endif
    }

    char *tesseract_rectangle(bytearray &image,int x0,int y0,int x1,int y1) {
        CHECK_ARG(0<=x0 && x0<x1 && x1<=image.dim(0));
        CHECK_ARG(0<=y0 && y0<y1 && y1<=image.dim(1));
#ifdef GOOGLE_INTERNAL
        math2raster(tess_pixels,image);
        tess_instance->SetImage(&tess_pixels(0,0),tess_pixels.dim(1),tess_pixels.dim(0),
                                1,tess_pixels.dim(1));
        tess_instance->SetRectangle(x0,y0,x1,y1);
        char *text = tess_instance->GetUTF8Text();
        if(getenv("TESS_DUMP")) tess_instance->DumpPGM(getenv("TESS_DUMP"));
#else
        bytearray temp;
        math2raster(temp,image);
        char *text = TessBaseAPI::TesseractRect(&temp(0,0),1,temp.dim(1),x0,y0,x1,y1);
#endif
        return text;
    }

    char *tesseract_block(bytearray &image) {
        return tesseract_rectangle(image,0,0,image.dim(0),image.dim(1));
    }

    void tesseract_end() {
#ifdef GOOGLE_INTERNAL
        tess_instance->End();
        tess_instance = 0;
#else
        TessBaseAPI::End();
#endif
    }
}
