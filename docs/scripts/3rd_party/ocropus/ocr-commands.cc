// -*- C++ -*-

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
// Project:
// File:
// Purpose:
// Responsible: tmb
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de, www.ocropus.org

#define __warn_unused_result__ __far__

#include <cctype>
#include <sys/types.h>
#include <sys/stat.h>
#include <glob.h>
#include <unistd.h>
#include "colib/colib.h"
#include "iulib/iulib.h"
#include "ocropus.h"
#include "glinerec.h"

#ifdef WIN32
#define mkdir(a,b) mkdir(a)
#endif

namespace glinerec {
    IRecognizeLine *make_Linerec();
    const char *command = "???";
}

namespace ocropus {
    using namespace iulib;
    using namespace colib;
    using namespace ocropus;
    using namespace narray_ops;
    using namespace glinerec;

    param_bool abort_on_error("abort_on_error",0,"abort recognition if there is an unexpected error");
    param_bool save_fsts("save_fsts",1,"save the fsts (set to 0 for eval-only in lines2fsts)");
    param_bool retrain("retrain",0,"perform retraining");
    param_bool retrain_threshold("retrain_threshold",100,"only retrain on characters with a cost lower than this");
    param_int nrecognize("nrecognize",1000000,"maximum number of lines to predict (for quick testing)");
    param_int ntrain("ntrain",10000000,"max number of training examples");
    param_string eval_flags("eval_flags","space","which features to ignore during evaluation");
    param_bool continue_partial("continue_partial",0,"don't compute outputs that already exist");
    param_bool old_csegs("old_csegs",0,"use old csegs (spaces are not counted)");
    param_float maxheight("max_line_height",300,"maximum line height");
    param_float maxaspect("max_line_aspect",0.5,"maximum line aspect ratio");

#if defined(__MACOS__) || defined(__MACOSX__) || defined(__MACH__)
#define DEFAULT_DATA_DIR "/opt/local/share/ocropus/models/"
#else
#define DEFAULT_DATA_DIR "/usr/local/share/ocropus/models/"
#endif

    param_string cmodel("cmodel",DEFAULT_DATA_DIR "default.model","character model used for recognition");
    param_string lmodel("lmodel",DEFAULT_DATA_DIR "default.fst","language model used for recognition");

    // these are used for the single page recognizer
    param_int beam_width("beam_width", 100, "number of nodes in a beam generation");

    void cleanup_for_eval(iucstring &s) {
        bool space = strflag(eval_flags,"space");
        bool scase = strflag(eval_flags,"case");
        bool nonanum = strflag(eval_flags,"nonanum");
        bool nonalpha = strflag(eval_flags,"nonalpha");
        iucstring result = "";
        for(int i=0;i<s.length();i++) {
            int c = s[i];
            if(space && c==' ') continue;
            if(nonanum && !isalnum(c)) continue;
            if(nonalpha && !isalpha(c)) continue;
            if(c<32||c>127) continue;
            if(scase && isupper(c)) c = tolower(c);
            result.push_back(c);
        }
        s = result;
    }

    void chomp_extension(char *s) {
        char *p = s+strlen(s);
        while(p>s) {
            --p;
            if(*p=='/') break;
            if(*p=='.') *p = 0;
        }
    }

    void nustring_convert(iucstring &output,nustring &str) {
        output.clear();
        output.append(str);
    }

    void nustring_convert(nustring &output,iucstring &str) {
        output.clear();
        str.toNustring(output);
    }

    static void store_costs(const char *base, floatarray &costs) {
        iucstring s;
        s = base;
        s.append(".costs");
        stdio stream(s,"w");
        for(int i=0;i<costs.length();i++) {
            fprintf(stream,"%d %g\n",i,costs(i));
        }
    }

    static void rseg_to_cseg(intarray &cseg, intarray &rseg, intarray &ids) {
        intarray map(max(rseg) + 1);
        map.fill(0);
        int color = 0;
        for(int i = 0; i < ids.length(); i++) {
            if(!ids[i]) continue;
            color++;
            int start = ids[i] >> 16;
            int end = ids[i] & 0xFFFF;
            if(start > end)
                throw "segmentation encoded in IDs looks seriously broken!\n";
            if(start >= map.length() || end >= map.length())
                throw "segmentation encoded in IDs doesn't fit!\n";
            for(int j = start; j <= end; j++)
                map[j] = color;
        }
        cseg.makelike(rseg);
        for(int i = 0; i < cseg.length1d(); i++)
            cseg.at1d(i) = map[rseg.at1d(i)];
    }

    static void rseg_to_cseg(const char *base, intarray &ids) {
        iucstring s;
        s = base;
        s += ".rseg.png";
        intarray rseg;
        read_image_packed(rseg, s.c_str());
        make_line_segmentation_black(rseg);
        intarray cseg;

        rseg_to_cseg(cseg, rseg, ids);

        ::make_line_segmentation_white(cseg);
        s = base;
        s += ".cseg.png";
        write_image_packed(s, cseg);
    }

    // Read a line and make an FST out of it.
    void read_transcript(IGenericFst &fst, const char *path) {
        nustring gt;
        read_utf8_line(gt, stdio(path, "r"));
        fst_line(fst, gt);
    }

    // Reads a "ground truth" FST (with extra spaces) by basename
    void read_gt(IGenericFst &fst, const char *base) {
        strbuf gt_path;
        gt_path = base;
        gt_path += ".gt.txt";

        read_transcript(fst, gt_path);
        for(int i = 0; i < fst.nStates(); i++)
            fst.addTransition(i, i, 0, 0, ' ');
    }



    // _______________________________________________________________________


    struct Glob {
        glob_t g;
        Glob(const char *pattern,int flags=0) {
            glob(pattern,flags,0,&g);
        }
        ~Glob() {
            globfree(&g);
        }
        int length() {
            return g.gl_pathc;
        }
        const char *operator()(int i) {
            CHECK(i>=0 && i<length());
            return g.gl_pathv[i];
        }
    };

    // _______________________________________________________________________

    void hocr_dump_preamble(FILE *output) {
        fprintf(output, "<!DOCTYPE html\n");
        fprintf(output, "   PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\n");
        fprintf(output, "   http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
    }

    void hocr_dump_head(FILE *output) {
        fprintf(output, "<head>\n");
        fprintf(output, "<meta name=\"ocr-capabilities\" content=\"ocr_line ocr_page\" />\n");
        fprintf(output, "<meta name=\"ocr-langs\" content=\"en\" />\n");
        fprintf(output, "<meta name=\"ocr-scripts\" content=\"Latn\" />\n");
        fprintf(output, "<meta name=\"ocr-microformats\" content=\"\" />\n");
        fprintf(output, "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />");
        fprintf(output, "<title>OCR Output</title>\n");
        fprintf(output, "</head>\n");
    }

    void hocr_dump_line(FILE *output, const char *path,
                        RegionExtractor &r, int index, int h) {
        fprintf(output, "<span class=\"ocr_line\"");
        if(index > 0 && index < r.length()) {
            fprintf(output, " title=\"bbox %d %d %d %d\"",
                        r.x0(index), h - 1 - r.y0(index),
                        r.x1(index), h - 1 - r.y1(index));
        }
        fprintf(output, ">\n");
        nustring s;
        read_utf8_line(s, stdio(path, "r"));
        write_utf8(output, s);
        fprintf(output, "</span>");
    }

    void hocr_dump_page(FILE *output, const char *path) {
        iucstring pattern;

        sprintf(pattern,"%s.seg.png",path);
        intarray page_seg;
        read_image_packed(page_seg, pattern);
        int h = page_seg.dim(1);

        RegionExtractor regions;
        regions.setPageLines(page_seg);
        rectarray bboxes;

        sprintf(pattern,"%s/[0-9][0-9][0-9][0-9].txt",path);
        Glob lines(pattern);
        fprintf(output, "<div class=\"ocr_page\">\n");
        for(int i = 0; i < lines.length(); i++) {
            // we have to figure out line number from the path because
            // the loop index is unreliable: it skips lines that didn't work
            pattern = lines(i);
            pattern.erase(pattern.length() - 4); // cut .txt
            int line_index = atoi(pattern.substr(pattern.length() - 4));
            hocr_dump_line(output, lines(i), regions, line_index, h);
        }
        fprintf(output, "</div>\n");
    }

    // _______________________________________________________________________

    int main_book2lines(int argc,char **argv) {
        int pageno = 0;
        autodel<ISegmentPage> segmenter;
        segmenter = make_SegmentPageByRAST();
        const char *outdir = argv[1];
        if(mkdir(outdir,0777)) {
            fprintf(stderr,"error creating OCR working directory\n");
            perror(outdir);
            exit(1);
        }
        iucstring s;
        for(int arg=2;arg<argc;arg++) {
            Pages pages;
            pages.parseSpec(argv[arg]);
            while(pages.nextPage()) {
                pageno++;
                debugf("info","page %d\n",pageno);
                sprintf(s,"%s/%04d",outdir,pageno);
                mkdir(s,0777);
                bytearray page_binary,page_gray;
                pages.getBinary(page_binary);
                pages.getGray(page_gray);
                intarray page_seg;
                segmenter->segment(page_seg,page_binary);
                RegionExtractor regions;
                regions.setPageLines(page_seg);
                for(int lineno=1;lineno<regions.length();lineno++) {
                    bytearray line_image;
                    regions.extract(line_image,page_gray,lineno,1);
                    // MAYBE output log of coordinates here
                    sprintf(s,"%s/%04d/%04d.png",outdir,pageno,lineno);
                    write_image_gray(s,line_image);
                }
                debugf("info","#lines = %d\n",regions.length());
                // MAYBE output images here
            }
        }
        return 0;
    }

    int main_book2pages(int argc,char **argv) {
        int pageno = 0;
        const char *outdir = argv[1];
        if(mkdir(outdir,0777)) {
            fprintf(stderr,"error creating OCR working directory\n");
            perror(outdir);
            exit(1);
        }
        iucstring s;
        for(int arg=2;arg<argc;arg++) {
            Pages pages;
            pages.parseSpec(argv[arg]);
            while(pages.nextPage()) {
                pageno++;
                debugf("info","page %d\n",pageno);
                mkdir(s,0777);
                bytearray page_binary,page_gray;

                // TODO/mezhirov make binarizer settable
                sprintf(s,"%s/%04d.png",outdir,pageno);
                pages.getGray(page_gray);
                write_image_gray(s,page_gray);

                pages.getBinary(page_binary);
                sprintf(s,"%s/%04d.bin.png",outdir,pageno);
                write_image_binary(s,page_binary);
            }
        }
        return 0;
    }

    int main_lines2fsts(int argc,char **argv) {
        if(argc!=2) throw "usage: cmodel=... ocropus lines2fsts dir";
        dinit(512,512);
        autodel<IRecognizeLine> linerec;
        iucstring pattern;
        sprintf(pattern,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].png",argv[1]);
        Glob files(pattern);
        int finished = 0;
        int nfiles = min(files.length(),nrecognize);
        int eval_total=0,eval_tchars=0,eval_pchars=0,eval_lines=0,eval_no_ground_truth=0;
#pragma omp parallel for private(linerec) shared(finished) schedule(dynamic,20)
        for(int index=0;index<nfiles;index++) {
#pragma omp critical
            {
                if(!linerec) {
                    linerec = glinerec::make_Linerec();
                    try {
                        linerec->load(cmodel);
                    } catch(const char *s) {
                        throwf("%s: failed to load (%s)",(const char*)cmodel,s);
                    } catch(...) {
                        throwf("%s: failed to load character model",(const char*)cmodel);
                    }
                }
            }
            iucstring base;
            base = files(index);
            base.erase(base.length()-4);
            debugf("progress","line %s\n",base.c_str());
            if(continue_partial) {
                iucstring s;
                sprintf(s,"%s.fst",base.c_str());
                FILE *stream = fopen(s.c_str(),"r");
                if(stream) {
                    fclose(stream);
                    // debugf("info","skipping line %s\n",base.c_str());
#pragma omp atomic
                    finished++;
#pragma omp atomic
                    eval_lines++;
                    continue;
                } 
            }
            bytearray image;
            // FIXME output binary versions, intermediate results for debugging
            read_image_gray(image,files(index));
            autodel<IGenericFst> result(make_OcroFST());
            intarray segmentation;
            try {
                CHECK_ARG(image.dim(1)<maxheight);
                CHECK_ARG(image.dim(1)*1.0/image.dim(0)<maxaspect);
                try {
                    linerec->recognizeLine(segmentation,*result,image);
                } catch(Unimplemented unimplemented) {
                    linerec->recognizeLine(*result,image);
                }
            } catch(BadTextLine &error) {
                fprintf(stderr,"ERROR: %s (bad text line)\n",base.c_str());
                continue;
            } catch(const char *error) {
                fprintf(stderr,"ERROR in recognizeLine: %s\n",error);
                if(abort_on_error) abort();
                continue;
            } catch(...) {
                fprintf(stderr,"ERROR in recognizeLine\n");
                if(abort_on_error) abort();
                continue;
            }

            if(save_fsts) {
                iucstring s;
                sprintf(s,"%s.fst",base.c_str());
                result->save(s);
                if(segmentation.length()>0) {
                    dsection("line_segmentation");
                    make_line_segmentation_white(segmentation);
                    sprintf(s,"%s.rseg.png",base.c_str());
                    write_image_packed(s,segmentation);
                    dshowr(segmentation);
                    dwait();
                }
            }

            nustring str;
            iucstring predicted;
            try {
                result->bestpath(str);
                nustring_convert(predicted,str);
                debugf("transcript","%s\t%s\n",files(index),predicted.c_str());
                if(save_fsts) {
                    iucstring s;
                    s = base;
                    s += ".txt";
                    fprintf(stdio(s.c_str(),"w"),"%s",predicted.c_str());
                }
            } catch(const char *error) {
                fprintf(stderr,"ERROR in bestpath: %s\n",error);
                if(abort_on_error) abort();
                continue;
            }

            try {
                iucstring s;
                s = base;
                s += ".gt.txt";
                char buf[100000];
                fgets(buf,sizeof buf,stdio(s,"r"));
                iucstring truth;
                truth = buf;
                cleanup_for_eval(truth);
                cleanup_for_eval(predicted);
                debugf("truth","%s\t%s\n",files(index),truth.c_str());
                nustring ntruth,npredicted;
                nustring_convert(ntruth,truth);
                nustring_convert(npredicted,predicted);
                float dist = edit_distance(ntruth,npredicted);
#pragma omp atomic
                eval_total += dist;
#pragma omp atomic
                eval_tchars += truth.length();
#pragma omp atomic
                eval_pchars += predicted.length();
#pragma omp atomic
                eval_lines++;
            } catch(...) {
#pragma omp atomic
                eval_no_ground_truth++;
            }

#pragma omp critical (finished_counter)
            {
                finished++;
                if(finished%100==0) {
                    if(eval_total>0)
                        debugf("info","finished %d/%d estimate %g errs %d ntrue %d npred %d lines %d nogt %d\n",
                                finished,files.length(),
                                eval_total/float(eval_tchars),eval_total,eval_tchars,eval_pchars,
                                eval_lines,eval_no_ground_truth);
                    else
                        debugf("info","finished %d/%d\n",finished,files.length());
                }
            }
        }

        debugf("info","rate %g errs %d ntrue %d npred %d lines %d nogt %d\n",
                eval_total/float(eval_tchars),eval_total,eval_tchars,eval_pchars,
                eval_lines,eval_no_ground_truth);
        return 0;
    }

    int main_pages2images(int argc,char **argv) {
        throw Unimplemented();
    }

    int main_pages2lines(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        dinit(1000,1000);
        const char *outdir = argv[1];
        autodel<ISegmentPage> segmenter;
        segmenter = make_SegmentPageByRAST();
        iucstring s;
        sprintf(s,"%s/[0-9][0-9][0-9][0-9].png",outdir);
        Glob files(s);
        if(files.length()<1)
            throw "no pages found";
        for(int index=0;index<files.length();index++) {
            char buf[1000];
            int pageno=9999;

            CHECK(sscanf(files(index),"%[^/]/%d.png",buf,&pageno)==2);
            debugf("info","page %d\n",pageno);

            sprintf(s,"%s/%04d",outdir,pageno);
            mkdir(s,0777);          // ignore errors

            bytearray page_gray;
            read_image_gray(page_gray,files(index));
            bytearray page_binary;
            sprintf(s,"%s/%04d.bin.png",outdir,pageno);
            read_image_binary(page_binary,s);

            intarray page_seg;
            segmenter->segment(page_seg,page_binary);
            sprintf(s,"%s/%04d.seg.png",outdir,pageno);
            write_image_packed(s, page_seg);

            RegionExtractor regions;
            regions.setPageLines(page_seg);
            for(int lineno=1;lineno<regions.length();lineno++) {
                try {
                    bytearray line_image;
                    regions.extract(line_image,page_gray,lineno,1);
                    CHECK_ARG(line_image.dim(1)<maxheight);
                    CHECK_ARG(line_image.dim(1)*1.0/line_image.dim(0)<maxaspect);
                    // TODO/mezhirov output log of coordinates here
                    sprintf(s,"%s/%04d/%04d.png",outdir,pageno,lineno);
                    write_image_gray(s,line_image);
                } catch(const char *s) {
                    fprintf(stderr,"ERROR: %s\n",s);
                    if(abort_on_error) abort();
                } catch(BadTextLine &err) {
                    fprintf(stderr,"ERROR: BadTextLine returned by recognizer\n");
                    if(abort_on_error) abort();
                } catch(...) {
                    fprintf(stderr,"ERROR: (no details)\n");
                    if(abort_on_error) abort();
                }
            }
            debugf("info","#lines = %d\n",regions.length());
            // TODO/mezhirov output other blocks here
        }
        return 0;
    }

    int main_evaluate(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        iucstring s;
        sprintf(s, "%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].gt.txt",argv[1]);
        Glob files(s);
        float total = 0.0, tchars = 0, pchars = 0, lines = 0;
        for(int index=0;index<files.length();index++) {
            if(index%1000==0)
                debugf("info","%s (%d/%d)\n",files(index),index,files.length());

            iucstring base = files(index);
            base.erase(base.find("."));

            iucstring truth;
            try {
                fgets(truth, stdio(files(index),"r"));
            } catch(const char *error) {
                continue;
            }

            iucstring s = base + ".txt";
            iucstring predicted;
            try {
                fgets(predicted, stdio(s,"r"));
            } catch(const char *error) {
                continue;
            }

            cleanup_for_eval(truth);
            cleanup_for_eval(predicted);
            nustring ntruth,npredicted;
            truth.toNustring(ntruth);
            predicted.toNustring(npredicted);
            float dist = edit_distance(ntruth,npredicted);

            total += dist;
            tchars += truth.length();
            pchars += predicted.length();
            lines++;

            debugf("transcript",
                    "%g\t%s\t%s\t%s\n",
                    dist,
                    files(index),
                    truth.c_str(),
                    predicted.c_str());
        }
        printf("rate %g total_error %g true_chars %g predicted_chars %g lines %g\n",
                total/float(tchars),total,tchars,pchars,lines);
        return 0;
    }

    int main_evalconf(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        iucstring s;
        sprintf(s,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].gt.txt",argv[1]);
        Glob files(s);
        float total = 0.0, tchars = 0, pchars = 0, lines = 0;
        intarray confusion(256,256); // FIXME/tmb limited to 256x256, replace with int2hash
        confusion = 0;
        for(int index=0;index<files.length();index++) {
            if(index%1000==0)
                debugf("info","%s (%d/%d)\n",files(index),index,files.length());

            iucstring base = files(index);
            base.erase(base.length()-7);

            iucstring truth;
            try {
                fgets(truth, stdio(files(index),"r"));
            } catch(const char *error) {
                continue;
            }

            iucstring s = base + ".txt";
            iucstring predicted;
            try {
                fgets(predicted, stdio(s,"r"));
            } catch(const char *error) {
                continue;
            }

            cleanup_for_eval(truth);
            cleanup_for_eval(predicted);
            nustring ntruth,npredicted;
            truth.toNustring(ntruth);
            predicted.toNustring(npredicted);
            float dist = edit_distance(confusion,ntruth,npredicted,1,1,1);

            total += dist;
            tchars += truth.length();
            pchars += predicted.length();
            lines++;

            debugf("transcript",
                    "%g\t%s\t%s\t%s\n",
                    dist,
                    files(index),
                    truth.c_str(),
                    predicted.c_str());
        }
        intarray list(65536,3); // FIXME/tmb replace with hash table when we move to Unicode
        int row = 0;
        for(int i=0;i<confusion.dim(0);i++) {
            for(int j=0;j<confusion.dim(1);j++) {
                if(confusion(i,j)==0) continue;
                if(i==j) continue;
                list(row,0) = confusion(i,j);
                list(row,1) = i;
                list(row,2) = j;
                row++;
            }
        }
        intarray perm;
        rowsort(perm,list);
        for(int k=0;k<perm.length();k++) {
            int index = perm(k);
            int count = list(index,0);
            int i = list(index,1);
            int j = list(index,2);
            if(count==0) continue;
            printf("%6d   %3d %3d   %c %c\n",
                    count,i,j,
                    i==0?'_':(i>32&&i<128)?i:'?',
                    j==0?'_':(j>32&&j<128)?j:'?');
        }
        return 0;
    }

    int main_findconf(int argc,char **argv) {
        if(argc!=4) throw "usage: ... dir from to";
        int from,to;
        if(sscanf(argv[2],"%d",&from)<1) {
            char c;
            sscanf(argv[2],"%c",&c);
            from = c;
        }
        if(sscanf(argv[3],"%d",&to)<1) {
            char c;
            sscanf(argv[3],"%c",&c);
            to = c;
        }
        iucstring s;
        sprintf(s,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].gt.txt",argv[1]);
        Glob files(s);
        intarray confusion(256,256);
        for(int index=0;index<files.length();index++) {
            iucstring base = files(index);
            base.erase(base.length()-7);


            iucstring truth;
            try {
                fgets(truth, stdio(files(index),"r"));
            } catch(const char *error) {
                continue;
            }

            iucstring s = base + ".txt";
            iucstring predicted;
            try {
                fgets(predicted, stdio(s,"r"));
            } catch(const char *error) {
                continue;
            }

            cleanup_for_eval(truth);
            cleanup_for_eval(predicted);
            nustring ntruth,npredicted;
            truth.toNustring(ntruth);
            predicted.toNustring(npredicted);
            confusion = 0;
            edit_distance(confusion,ntruth,npredicted,1,1,1);
            if(confusion(from,to)>0) {
                printf("%s.png\n",base.c_str());
            }
        }
        return 0;
    }
    int main_evalfiles(int argc,char **argv) {
        if(argc!=3) throw "usage: ... file1 file2";
        iucstring truth;
        fread(truth, stdio(argv[1],"r"));
        iucstring predicted;
        fread(predicted, stdio(argv[2],"r"));

        cleanup_for_eval(truth);
        cleanup_for_eval(predicted);
        nustring ntruth,npredicted;
        truth.toNustring(ntruth);
        predicted.toNustring(npredicted);

        float dist = edit_distance(ntruth,npredicted);
        printf("dist %g tchars %d pchars %d\n",
                dist,truth.length(),predicted.length());
        return 0;
    }

    int main_fsts2bestpaths(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        iucstring s;
        sprintf(s,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].fst",argv[1]);
        Glob files(s);
        for(int index=0;index<files.length();index++) {
            if(index%1000==0)
                debugf("info","%s (%d/%d)\n",files(index),index,files.length());
            autodel<IGenericFst> fst(make_OcroFST());
            fst->load(files(index));
            nustring str;
            try {
                fst->bestpath(str);
                iucstring output = str;
                debugf("transcript","%s\t%s\n",files(index),output.c_str());
                iucstring base = files(index);
                base.erase(base.length()-4);
                base += ".txt";
                fprintf(stdio(base,"w"),"%s",output.c_str());
            } catch(const char *error) {
                fprintf(stderr,"ERROR in bestpath: %s\n",error);
                if(abort_on_error) abort();
            }
        }
        return 0;
    }

    int main_cinfo(int argc,char **argv) {
        // FIXME use components to load/save line recognizers
        autodel<IRecognizeLine> linerec(make_Linerec());
        stdio model(argv[1],"r");
        if(!model) {
            fprintf(stderr,"%s: could not open\n",argv[1]);
            return 1;
        }
        linerec->load(model);
        if(!linerec) {
            fprintf(stderr,"%s: load failed\n",argv[1]);
        } else {
            linerec->info();
        }
        return 0;
    }

    int main_params(int argc,char **argv) {
        if(argc<2) throwf("usage: %s classname\n",argv[0]);
        ocropus::global_verbose_params = "";
        // FIXME why do we need the ocropus:: qualifier?
        autodel<IComponent> result;
        try {
            result = component_construct(argv[1]);
            printf("\n");
            printf("name=%s\n",result->name());
            printf("description=%s\n",result->description());
        } catch(const char *err) {
            fprintf(stderr,"%s: %s\n",argv[1],err);
        }
        return 0;
    }

    int main_components(int argc,char **argv) {
        narray<const char *> names;
        list_components(names);
        for(int i=0;i<names.length();i++) {
            autodel<IComponent> p;
            p = component_construct(names[i]);
            iucstring desc(p->description());
            int where = desc.find("\n");
            if(where!=desc.npos) desc = desc.substr(0,where);
            if(desc.length()>60) desc = desc.substr(0,60);
            printf("%-32s %-32s\n    %s\n",names[i],p->name(),desc.c_str());
        }
        return 0;
    }

    int main_page(int argc,char **argv) {
        // create the segmenter
        autodel<ISegmentPage> segmenter;
        segmenter = make_SegmentPageByRAST();
        // load the line recognizer
        autodel<IRecognizeLine> linerec;
        linerec = make_Linerec();
        try {
            linerec->load(cmodel);
        } catch(const char *s) {
            throwf("%s: failed to load (%s)",(const char*)cmodel,s);
        } catch(...) {
            throwf("%s: failed to load character model",(const char*)cmodel);
        }
        // load the language model
        autodel<OcroFST> langmod(make_OcroFST());
        try {
            langmod->load(lmodel);
        } catch(const char *s) {
            throwf("%s: failed to load (%s)",(const char*)lmodel,s);
        } catch(...) {
            throwf("%s: failed to load language model",(const char*)lmodel);
        }
        // now iterate through the pages
        for(int arg=1;arg<argc;arg++) {
            Pages pages;
            pages.parseSpec(argv[arg]);
            while(pages.nextPage()) {
                bytearray page_binary,page_gray;
                intarray page_seg;
                pages.getBinary(page_binary);
                pages.getGray(page_gray);
                segmenter->segment(page_seg,page_binary);
                RegionExtractor regions;
                regions.setPageLines(page_seg);
                for(int i=1;i<regions.length();i++) {
                    try {
                        bytearray line_image;
                        regions.extract(line_image,page_gray,i,1);
                        autodel<OcroFST> result(make_OcroFST());
                        linerec->recognizeLine(*result,line_image);
                        nustring str;
                        if(0) {
                            result->bestpath(str);
                        } else {
                            double cost = beam_search(str,*result,*langmod,beam_width);
                            if(cost>1e10) throw "beam search failed";
                        }
                        iucstring output;
                        nustring_convert(output,str);
                        printf("%s\n",output.c_str());
                    } catch(const char *error) {
                        fprintf(stderr,"[%s]\n",error);
                    }
                }
            }
        }
        return 0;
    }

    int main_recognize1(int argc,char **argv) {
        if(argc<3) throwf("usage: %s %s model image ...",command,argv[0]);
        if(!getenv("ocrolog"))
            throwf("please set ocrolog=glr in the environment");
        if(!getenv("ocrologdir"))
            throwf("please set ocrologdir to the target directory for the log");
        Logger logger("glr");
        dinit(512,512);
        autodel<IRecognizeLine> linerecp(make_Linerec());
        IRecognizeLine &linerec = *linerecp;
        stdio model(argv[1],"r");
        linerec.load(model);
        for(int i=3;i<argc;i++) {
            logger.html("<hr><br>");
            logger.format("Recognizing %s",argv[i]);
            bytearray image;
            read_image_gray(image,argv[i]);
            autodel<IGenericFst> result(make_OcroFST());
            linerec.recognizeLine(*result,image);

            // dump the corresponding FST for display
            // print_fst_simple(*result);
            // result->save("rec_temp.fst");
            dump_fst("rec_temp.dot",*result);
            intarray fst_image;
            fst_to_image(fst_image,*result);
            logger.log("fst",fst_image);

            // output the result
            nustring str;
            str.clear();
            result->bestpath(str);
            if(str.length()<1) throw "recognition failed";
            if(debug("detail")) {
                debugf("detail","nustring result: ");
                for(int i=0;i<str.length();i++)
                    printf(" %d",str(i).ord());
                printf("\n");
            }
            narray<char> s;
            str.utf8Encode(s);
            s.push(0);
            printf("%s\t%s\n",argv[i],&s[0]);

            // log the string to the output file
            char buffer[10000];
            sprintf(buffer,"<font style='color: green; font-size: 36pt;'>%s</font>\n",&s[0]);
            //logger.log("result",&s[0]);
            logger.html(buffer);
        }
        return 0;
    }

    int main_fsts2text(int argc,char **argv) {
        if(argc!=2) throw "usage: lmodel=... ocropus fsts2text dir";
        autodel<OcroFST> langmod(make_OcroFST());
        try {
            langmod->load(lmodel);
        } catch(const char *s) {
            throwf("%s: failed to load (%s)",(const char*)lmodel,s);
        } catch(...) {
            throwf("%s: failed to load language model",(const char*)lmodel);
        }
        iucstring s;
        sprintf(s,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].fst",argv[1]);
        Glob files(s);
#pragma omp parallel for schedule(dynamic,20)
        for(int index=0;index<files.length();index++) {
            if(index%1000==0)
                debugf("info","%s (%d/%d)\n",files(index),index,files.length());
            autodel<OcroFST> fst(make_OcroFST());
            fst->load(files(index));
            nustring str;
            try {
                intarray v1;
                intarray v2;
                intarray in;
                intarray out;
                floatarray costs;
                beam_search(v1, v2, in, out, costs,
                            *fst, *langmod, beam_width);
                double cost = sum(costs);
                remove_epsilons(str, out);
                if(cost < 1e10) {
                    iucstring output;
                    nustring_convert(output,str);
                    debugf("transcript","%s\t%s\n",files(index), output.c_str());
                    iucstring base;
                    base = files(index);
                    base.erase(base.length()-4);
                    try {
                        rseg_to_cseg(base, in);
                        store_costs(base, costs);
                    } catch(const char *err) {
                        fprintf(stderr,"ERROR in cseg reconstruction: %s\n",err);
                        if(abort_on_error) abort();
                    }
                    base += ".txt";
                    fprintf(stdio(base,"w"),"%s\n",output.c_str());
                } else {
                    debugf("info","%s\t%f\n",files(index), cost);
                }
            } catch(const char *error) {
                fprintf(stderr,"ERROR in bestpath: %s\n",error);
                if(abort_on_error) abort();
            }
        }

        return 0;
    }


    int main_align(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        iucstring s;
        s = argv[1];
        s += "/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].fst";
        Glob files(s);
        for(int index=0;index<files.length();index++) {
            if(index%1000==0)
                debugf("info","%s (%d/%d)\n",files(index),index,files.length());

            iucstring base;
            base = files(index);
            base.erase(base.length()-4);

            autodel<OcroFST> gt_fst(make_OcroFST());
            read_gt(*gt_fst, base);

            autodel<OcroFST> fst(make_OcroFST());
            fst->load(files(index));
            nustring str;
            intarray v1;
            intarray v2;
            intarray in;
            intarray out;
            floatarray costs;
            try {
                beam_search(v1, v2, in, out, costs,
                            *fst, *gt_fst, beam_width);
                // recolor rseg to cseg
            } catch(const char *error) {
                fprintf(stderr,"ERROR in bestpath: %s\n",error);
                if(abort_on_error) abort();
            }
            try {
                rseg_to_cseg(base, in);
                store_costs(base, costs);
                debugf("dcost","--------------------------------\n");
                for(int i=0;i<out.length();i++) {
                    debugf("dcost","%3d %10g %c\n",i,costs(i),out(i));
                }
            } catch(const char *err) {
                fprintf(stderr,"ERROR in cseg reconstruction: %s\n",err);
                if(abort_on_error) abort();
            }
        }
        return 0;
    }


    int main_loadseg(int argc,char **argv) {
        if(argc!=3) throw "usage: ... model dir";
        dinit(512,512);
        autodel<IRecognizeLine> linerecp(make_Linerec());
        IRecognizeLine &linerec = *linerecp;
        struct stat sbuf;
        if(argv[1][0]!='.' && !stat(argv[1],&sbuf))
            throw "output model file already exists; please remove first";
        fprintf(stderr,"loading %s\n",argv[2]);
        linerec.startTraining("");
        linerec.set("load_ds8",argv[2]);
        linerec.finishTraining();
        fprintf(stderr,"saving %s\n",argv[1]);
        stdio stream(argv[1],"w");
        linerec.save(stream);
        return 0;
    }

    int main_trainseg_or_saveseg(int argc,char **argv) {
        if(argc!=3) throw "usage: ... model dir";
        dinit(512,512);
        autodel<IRecognizeLine> linerecp(make_Linerec());
        IRecognizeLine &linerec = *linerecp;
        struct stat sbuf;
        if(argv[1][0]!='.' && !stat(argv[1],&sbuf))
            throw "output model file already exists; please remove first";
        stdio lines(argv[2],"r");
        char base[10000],filename[10000],current[10000];
        intarray cseg;
        bytearray image;
        int total_chars = 0;
        int total_lines = 0;
        linerec.startTraining("");
        iucstring pattern;
        if(retrain)
            sprintf(pattern,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].cseg.png",argv[2]);
        else
            sprintf(pattern,"%s/[0-9][0-9][0-9][0-9]/[0-9][0-9][0-9][0-9].cseg.gt.png",argv[2]);
        debugf("info","%s\n",pattern.c_str());
        Glob files(pattern);
        if(files.length()<1) throw "no pages found";
        int next = 1000;
        floatarray costs;
        for(int index=0;index<files.length();index++) {
            try {
                strcpy(base,files(index));
                chomp_extension(base);

                strcpy(filename,base);
                if(retrain)
                    strcat(filename,".cseg.png");
                else
                    strcat(filename,".cseg.gt.png");
                strcpy(current,filename);
                read_line_segmentation(cseg,stdio(filename,"r"));
                image.makelike(cseg);
                for(int i=0;i<image.length1d();i++)
                    image.at1d(i) = 255*!cseg.at1d(i);


                // read the ground truth segmentation

                nustring nutranscript;
                {
                    strcpy(filename,base);
                    if(retrain)
                        strcat(filename,".txt");
                    else
                        strcat(filename,".gt.txt");
                    // FIXME this doesn't work with Unicode characters
                    char transcript[10000];
                    stdio line(filename,"r");
                    CHECK_ARG(fgets(transcript,sizeof(transcript),line)>0);
                    line.close();
                    chomp(transcript);
                    if(old_csegs) remove_spaces(transcript);
                    nutranscript = transcript;
                    if(nutranscript.length()!=max(cseg))
                        throwf("transcript doesn't agree with cseg (transcript %d, cseg %d)",
                            nutranscript.length(),max(cseg));
                }

                // for retraining, read the cost file

                if(retrain) {
                    strcpy(filename,base);
                    strcat(filename,".costs");
                    costs.resize(10000) = 1e38;
                    stdio stream(filename,"r");
                    int index;
                    float cost;
                    while(fscanf(stream,"%d %g\n",&index,&cost)==2) {
                        costs(index) = cost;
                    }
                    // remove all segments whose costs are too high
                    int old_length = nutranscript.length();
                    for(int i=0;i<nutranscript.length();i++) {
                        if(costs[i]>retrain_threshold)
                            nutranscript[i] = nuchar(' ');
                    }
                    for(int i=0;i<cseg.length();i++) {
                        if(costs(cseg[i])>retrain_threshold)
                            cseg[i] = 0;
                    }
                    int delta = old_length - nutranscript.length();
                    if(delta>0) {
                        debugf("info","removed %d exceeding cost\n",delta);
                    }
                    debugf("dcost","--------------------------------\n");
                    for(int i=0;i<nutranscript.length();i++) {
                        debugf("dcost","%3d %10g %c\n",i,costs(i),nutranscript(i).ord());
                    }
                    {
                        dsection("dcost");
                        dshowr(cseg);
                        dwait();
                    }
                }

                // let the user know about progress

                {
                    char *transcript = nutranscript.mallocUtf8Encode();
                    debugf("transcript","%s (%d) [%2d,%2d] %s\n",filename,total_chars,
                           nutranscript.length(),max(cseg),transcript);
                    free(transcript);
                }
                if(total_chars>=next) {
                    debugf("info","loaded %d chars, %s total\n",
                            total_chars,linerec.command("total"));
                    next += 1000;
                }

                // now, actually add the segmented characters to the line recognizer

                try {
                    linerec.addTrainingLine(cseg,image,nutranscript);
                    total_chars += max(cseg);
                    total_lines++;
                } catch(const char *s) {
                    fprintf(stderr,"ERROR: %s\n",s);
                } catch(BadTextLine &err) {
                    fprintf(stderr,"ERROR: BadTextLine\n");
                } catch(...) {
                    fprintf(stderr,"ERROR: (no details)\n");
                }
            } catch(const char *msg) {
                printf("%s: %s FIXME\n",filename,msg);
            }
            if(total_chars>=ntrain) break;
        }
        if(!strcmp(argv[0],"trainseg")) {
            linerec.finishTraining();
            fprintf(stderr,"trained %d characters, %d lines\n",
                    total_chars,total_lines);
            fprintf(stderr,"saving %s\n",argv[1]);
            stdio stream(argv[1],"w");
            linerec.save(stream);
            return 0;
        } else if(!strcmp(argv[0],"saveseg")) {
            fprintf(stderr,"saving %d characters, %d lines\n",
                    total_chars,total_lines);
            fprintf(stderr,"saving %s\n",argv[1]);
            linerec.set("save_ds8",argv[1]);
            // stdio stream(argv[1],"w");
            // linerec.save(stream);
            return 0;
        } else throw "oops";
    }

    int main_buildhtml(int argc,char **argv) {
        if(argc!=2) throw "usage: ... dir";
        iucstring pattern;
        sprintf(pattern,"%s/[0-9][0-9][0-9][0-9]",argv[1]);
        Glob pages(pattern);
        FILE *output = stdout;
        hocr_dump_preamble(output);
        fprintf(output, "<html>\n");
        hocr_dump_head(output);
        fprintf(output, "<body>\n");
        for(int i = 0; i < pages.length(); i++) {
            hocr_dump_page(output, pages(i));
        }
        fprintf(output, "</body>\n");
        fprintf(output, "</html>\n");
        return 0;
    }

    int main_cleanhtml(int argc,char **argv) {
        throw Unimplemented();
    }

    void usage(const char *program) {
        fprintf(stderr,"usage:\n");
#define D(s,s2) {fprintf(stderr,"    %s %s\n",program,s); fprintf(stderr,"        %s\n",s2); }
#define P(s) {fprintf(stderr,"%s\n",s);}
#define SECTION(x) {fprintf(stderr,"\n*** %s\n\n",x);}
        SECTION("splitting books");
        D("book2pages dir image image ...",
                "convert the input into a set of pages under dir/...");
        D("pages2lines dir",
                "convert the pages in dir/... into lines");
        SECTION("line recognition and language modeling")
        D("lines2fsts dir",
                    "convert the lines in dir/... into fsts (lattices); cmodel=...")
        D("fsts2bestpaths dir",
                "find the best interpretation of the fsts in dir/... without a language model");
        D("fsts2textdir",
                "find the best interpretation of the fsts in dir/...; lmodel=...");
        SECTION("evaluation");
        D("evaluate dir",
                "evaluate the quality of the OCR output in dir/...");
        D("evalconf dir",
                "evaluate the quality of the OCR output in dir/... and outputs confusion matrix");
        D("findconf dir from to",
                "finds instances of confusion of from to to (according to edit distance)");
        D("evaluate1 file1 file2",
                "compute the edit distance between the two files");
        SECTION("training");
        D("align dir",
                "align fsts with ground truth transcripts");
        D("trainseg model dir",
                "train a model for the ground truth in dir/...");
        D("saveseg dataset dir",
                "perform dataset extraction on the book directory and save it");
        D("loadseg model dataset",
                "perform training on the dataset (saveseg + loadseg is the same as trainseg)");
        SECTION("other recognizers");
        D("recognize1 logdir model line1 line2...",
                "recognize images of individual lines of text given on the command line; ocrolog=glr ocrologdir=...");
        D("page image.png",
                "recognize a single page of text without adaptivity, but with a language model");
        SECTION("components");
        D("components",
                "list available components (of any type)");
        D("params component",
                "output the available parameters for the given component");
        D("cinfo model",
                "load the classifier model and print information on it");
        SECTION("results");
        D("buildhtml dir",
                "creates an HTML representation of the OCR output in dir/...");
#if 0
        D("cleanhtml dir",
                "removes all files from dir/... that aren't needed for the HTML output");
#endif
        exit(1);
    }

    int main_ocropus(int argc,char **argv) {
        try {
            command = argv[0];
            init_ocropus_components();
            init_glclass();
            init_glfmaps();
            init_linerec();
            if(argc<2) usage(argv[0]);
            if(!strcmp(argv[1],"book2lines")) return main_pages2lines(argc-1,argv+1);
            if(!strcmp(argv[1],"book2pages")) return main_book2pages(argc-1,argv+1);
            if(!strcmp(argv[1],"buildhtml")) return main_buildhtml(argc-1,argv+1);
            if(!strcmp(argv[1],"cinfo")) return main_cinfo(argc-1,argv+1);
            if(!strcmp(argv[1],"cleanhtml")) return main_buildhtml(argc-1,argv+1);
            if(!strcmp(argv[1],"components")) return main_components(argc-1,argv+1);
            if(!strcmp(argv[1],"evalconf")) return main_evalconf(argc-1,argv+1);
            if(!strcmp(argv[1],"evaluate")) return main_evaluate(argc-1,argv+1);
            if(!strcmp(argv[1],"evaluate1")) return main_evalfiles(argc-1,argv+1);
            if(!strcmp(argv[1],"findconf")) return main_findconf(argc-1,argv+1);
            if(!strcmp(argv[1],"fsts2bestpaths")) return main_fsts2bestpaths(argc-1,argv+1);
            if(!strcmp(argv[1],"fsts2text")) return main_fsts2text(argc-1,argv+1);
            if(!strcmp(argv[1],"lines2fsts")) return main_lines2fsts(argc-1,argv+1);
            if(!strcmp(argv[1],"loadseg")) return main_loadseg(argc-1,argv+1);
            if(!strcmp(argv[1],"align")) return main_align(argc-1,argv+1);
            if(!strcmp(argv[1],"page")) return main_page(argc-1,argv+1);
            if(!strcmp(argv[1],"pages2images")) return main_pages2images(argc-1,argv+1);
            if(!strcmp(argv[1],"pages2lines")) return main_pages2lines(argc-1,argv+1);
            if(!strcmp(argv[1],"params")) return main_params(argc-1,argv+1);
            if(!strcmp(argv[1],"recognize1")) return main_recognize1(argc-1,argv+1);
            if(!strcmp(argv[1],"saveseg")) return main_trainseg_or_saveseg(argc-1,argv+1);
            if(!strcmp(argv[1],"trainseg")) return main_trainseg_or_saveseg(argc-1,argv+1);
            usage(argv[0]);
        } catch(const char *s) {
            fprintf(stderr,"FATAL: %s\n",s);
        }
        return 0;
    }
}
