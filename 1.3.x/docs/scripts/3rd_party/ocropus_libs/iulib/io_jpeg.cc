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
// Project: ocropus
// File: didegrade.h
// Purpose: provide JPEG image I/O
// Responsible: mezhirov
// Reviewer: 
// Primary Repository: 
// Web Sites: www.iupr.org, www.dfki.de

#include <stdio.h>
#include <string.h>
#include "io_jpeg.h"
extern "C" {
#include <jpeglib.h>
}


using namespace colib;

bool jpeg_debug = (getenv("jpeg_debug") && atoi(getenv("jpeg_debug")));

namespace iulib {

    // This code was adapted from an example to libjpeg.

    void jpeg_print_cinfo(struct jpeg_decompress_struct &cinfo) {
        printf("Values of attributes of jpeg_decompress_struct: \n");
        printf("  image_width = \t%d\n", cinfo.image_width );
        printf("  image_height = \t%d\n", cinfo.image_height );
        printf("  num_components = \t%d\n", cinfo.num_components );
        printf("  jpeg_color_space = \t%d\n", cinfo.jpeg_color_space );
        printf("  out_color_space = \t%d\n", cinfo.out_color_space );
        printf("  scale_num = \t%d\n", cinfo.scale_num );
        printf("  scale_denom = \t%d\n", cinfo.scale_denom );
        printf("  buffered_image = \t%d\n", cinfo.buffered_image );
        printf("  raw_data_out = \t%d\n", cinfo.raw_data_out );
        printf("  quantize_colors = \t%d\n", cinfo.quantize_colors );
        printf("  desired_number_of_colors = \t%d\n", cinfo.desired_number_of_colors );
        printf("  output_width = \t%d\n", cinfo.output_width );
        printf("  output_height = \t%d\n", cinfo.output_height );
        printf("  out_color_components = \t%d\n", cinfo.out_color_components );
        printf("  output_components = \t%d\n", cinfo.output_components );
        printf("  rec_outbuf_height = \t%d\n", cinfo.rec_outbuf_height );
        printf("  actual_number_of_colors = \t%d\n", cinfo.actual_number_of_colors );
    }

    void read_jpeg_any(bytearray &a, FILE *infile) {
        struct jpeg_decompress_struct cinfo;
        JSAMPARRAY buffer;                /* Output row buffer */
        int row_stride;                /* physical row width in output buffer */
        struct jpeg_error_mgr jerr;
        memset(&jerr, 0, sizeof(jerr));
        cinfo.err = jpeg_std_error(&jerr);
        jpeg_create_decompress(&cinfo);
        jpeg_stdio_src(&cinfo, infile);
        jpeg_read_header(&cinfo, TRUE);
        cinfo.out_color_space = JCS_RGB ; // set the output color space to RGB
        jpeg_start_decompress(&cinfo);
        row_stride = cinfo.output_width * cinfo.output_components;
        a.resize(row_stride, cinfo.output_height);
        buffer = (*cinfo.mem->alloc_sarray)
            ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);
        int y = cinfo.output_height - 1;
        while (cinfo.output_scanline < cinfo.output_height) {
            jpeg_read_scanlines(&cinfo, buffer, 1);
            for(int i = 0; i < row_stride; i++)
                a(i,y) = buffer[0][i];
            y--;
        }
        if(cinfo.output_components==3) {
            int w = cinfo.output_width;
            int c = cinfo.output_components;
            int h = cinfo.output_height;
            a.reshape(w,c,h);
            bytearray temp(w,h,c);
            for(int i=0;i<w;i++) {
                for(int j=0;j<h;j++) {
                    for(int k=0;k<c;k++) temp(i,j,k) = a(i,k,j);
                }
            }
            move(a,temp);
        }
        jpeg_finish_decompress(&cinfo);
        jpeg_destroy_decompress(&cinfo);
    }

    void read_jpeg_packed(intarray &a, FILE *infile) {
        throw "testing";
        struct jpeg_decompress_struct cinfo;
        JSAMPARRAY buffer;                /* Output row buffer */
        int row_stride;                /* physical row width in output buffer */
        struct jpeg_error_mgr jerr;
        memset(&jerr, 0, sizeof(jerr));
        cinfo.err = jpeg_std_error(&jerr);
        jpeg_create_decompress(&cinfo);
        jpeg_stdio_src(&cinfo, infile);
        jpeg_read_header(&cinfo, TRUE); // sets the attributes values
        cinfo.out_color_space = JCS_RGB ; // set the output color space to RGB
        jpeg_start_decompress(&cinfo);
        row_stride = cinfo.output_width * cinfo.output_components;
        a.resize(cinfo.output_width, cinfo.output_height); // resize output array
        buffer = (*cinfo.mem->alloc_sarray)
            ((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);
        int y = cinfo.output_height - 1;
        CHECK_ARG(cinfo.output_components==3);
        while (cinfo.output_scanline < cinfo.output_height) {
            jpeg_read_scanlines(&cinfo, buffer, 1);
            int i = 0;
            while (i < row_stride) {
                int tmp = 0 ;
                tmp = buffer[0][i]; // R
                tmp = tmp<<8;
                tmp = tmp + buffer[0][i+1]; // G
                tmp = tmp<<8;
                tmp = tmp + buffer[0][i+2]; // B
                a(i/3,y) = tmp;
                i = i + 3;
            }
            y--;
        }

        jpeg_finish_decompress(&cinfo);
        jpeg_destroy_decompress(&cinfo);
    }

    void read_jpeg_gray(bytearray &a, FILE *f) {
        bytearray b;
        read_jpeg_any(b,f);
        if(b.rank() == 2) {
            move(a, b);
            return;
        }
        a.resize(b.dim(0),b.dim(1));
        for(int x = 0; x < b.dim(0); x++) {
            for(int y = 0; y < b.dim(1); y++) {
                a(x,y) = (b(x,y,0)+b(x,y,1)+b(x,y,2))/3;
            }
        }
    }

    void read_jpeg_rgb(bytearray &a,FILE *infile) {
        bytearray b;
        read_jpeg_any(a,infile);
        ASSERT(a.rank()==3);
    }
}
