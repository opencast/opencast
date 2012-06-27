// -*- C++ -*-

// Copyright 2006 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
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

#ifndef glinerec_h__
#define glinerec_h__

#ifdef WIN32
#ifdef __cplusplus
extern "C" {
#endif
long lrand48(void);
#ifdef __cplusplus
}
#endif
#endif

#include "colib/colib.h"
#include "colib/checks.h"
#include "colib/iarith.h"
#include "iulib/iulib.h"
#include "colib/rowarrays.h"
#include "ocropus.h"
#include "narray-binio.h"

namespace glinerec {
    struct BadTextLine {};
}

namespace {
    template <class T>
    struct tempset {
        T old;
        T *location;
        tempset(T &destination,T value) {
            old = destination;
            location = &destination;
            destination = value;
        }
        ~tempset() {
            *location = old;
        }
    };
}

#include "glutils.h"
#include "gldataset.h"
#include "glclass.h"
#include "glcuts.h"
#include "glfmaps.h"

namespace glinerec {
    void init_linerec();
    void init_glclass();
}

#endif
