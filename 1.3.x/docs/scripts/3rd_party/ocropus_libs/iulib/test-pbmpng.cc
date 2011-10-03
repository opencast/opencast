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
// Project: roughocr -- mock OCR system exercising the interfaces and useful for testing
// File: kmeans.h
// Purpose: interface to corresponding .cc file
// Responsible: tmb
// Reviewer: 
// Primary Repository: 
// Web Sites: www.iupr.org, www.dfki.de

#include "colib.h"
#include "imgio.h"


using namespace iulib;
using namespace colib;

void get_random_color_image(bytearray &a) {
    int w = rand()%2000+10;
    int h = rand()%2000+10;
    a.resize(w,h,3);
    int n = a.length1d();
    for(int i=0;i<n;i++) a.at1d(i) = rand()%256; 
}

void read_png_and_assert_equal(bytearray &a, const char *path) {
    bytearray b;
    read_png(b,stdio(path,"rb"));
    TEST_ASSERT(equal(a,b));
}

int main(int argc,char **argv) {
    srand(0);
    for(int trial=0;trial<10;trial++) {
        bytearray image1;
        get_random_color_image(image1);
        //printf("# PNG-PNG %d %d %d\n",trial,image1.dim(0),image1.dim(1));
        write_png(stdio("%test_image.png","wb"),image1);
        read_png_and_assert_equal(image1, "%test_image.png");
    }
/*
    for(int trial=0;trial<10;trial++) {
        int w = rand()%2000+10;
        int h = rand()%2000+10;
        //fprintf(stderr,"# PNG-PPM %d %d %d\n",trial,w,h);
        bytearray image1(w,h,3);
        int n = image1.length1d();
        for(int i=0;i<n;i++) image1.at1d(i) = rand()%256;
        write_png(stdio("%test_image.png","wb"),image1);
        system("convert %test_image.png %test_image.ppm");
        bytearray image2;
        read_ppm_rgb(stdio("%test_image.ppm","rb"),image2);
        TEST_ASSERT(equal(image1,image2));
    }
    for(int trial=0;trial<10;trial++) {
        int w = rand()%2000+10;
        int h = rand()%2000+10;
        //fprintf(stderr,"# PPM-PNG %d %d %d\n",trial,w,h);
        bytearray image1(w,h,3);
        int n = image1.length1d();
        for(int i=0;i<n;i++) image1.at1d(i) = rand()%256;
        write_ppm_rgb(stdio("%test_image.ppm","wb"),image1);
        system("convert %test_image.ppm %test_image.png");
        bytearray image2;
        read_png(image2,stdio("%test_image.png","rb"));
        TEST_ASSERT(equal(image1,image2));
    }
*/
    // FIXME need more tests here, for things like detecting incorrect formats, etc.
    
    // remove temporary files
    remove("%test_image.png");
    remove("%test_image.ppm");
    return 0;
}
