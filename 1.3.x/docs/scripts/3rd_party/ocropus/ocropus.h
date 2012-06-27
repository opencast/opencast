// -*- C++ -*-

// Copyright 2006-2008 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
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
// Project: OCRopus
// File: ocropus.h
// Purpose: Top level header file
// Responsible:
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

#ifndef ocropus_headers_
#define ocropus_headers_

#include "colib/colib.h"
#include "iulib/iulib.h"
#include "components.h"
#include "ocrinterfaces.h"

namespace ocropus {
    using namespace colib;

    IBinarize *make_BinarizeByRange();
    IBinarize *make_BinarizeByOtsu();
    IBinarize *make_BinarizeBySauvola();

    ISegmentLine *make_SegmentLineByProjection();
    ISegmentLine *make_SegmentLineByCCS();
    ISegmentLine *make_ConnectedComponentSegmenter();
    ISegmentLine *make_CurvedCutSegmenter();
    ISegmentLine *make_CurvedCutWithCcSegmenter();
    ISegmentLine *make_SkelSegmenter();
}

#include "ocr-layout.h"
#include "line-info.h"

#ifdef HAVE_FST
//#include "ocr-openfst.h"
#endif
#include "ocr-pfst.h"

// these come from ocr-utils

#include "didegrade.h"
#include "editdist.h"
#include "enumerator.h"
#include "grid.h"
#include "grouper.h"
#include "logger.h"
#include "narray-io.h"
#include "segmentation.h"
#include "docproc.h"
#include "stringutil.h"
#include "arraypaint.h"
#include "pages.h"
#include "queue-loc.h"
#include "pagesegs.h"
#include "linesegs.h"
#include "resource-path.h"
#include "segmentation.h"
#include "sysutil.h"
#include "xml-entities.h"
#include "init-ocropus.h"

#ifndef CHECK
#define CHECK(X) CHECK_ARG(X)
#endif

#endif
