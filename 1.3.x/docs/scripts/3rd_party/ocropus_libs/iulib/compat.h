// -*- C++ -*-

// Copyright 2007 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz 
// or its licensors, as applicable.
// Copyright 1995-2005 Thomas M. Breuel.
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
// Project: iulib -- image understanding library
// File: compat.h
// Purpose: Compatibility for Microsoft Windows
// Responsible: lakshmesha
// Reviewer: kofler
// Primary Repository: 
// Web Sites: www.iupr.org, www.dfki.de

/// \file compat.h
/// \brief Compatibility header for Microsoft Windows.

#ifndef _INC_COMPAT_HDR
#define _INC_COMPAT_HDR

#ifdef WIN32
#include <float.h>
#include <basetsd.h>//Required for Tesseract
#include <io.h>//Required for pipe,close,dup
#include <process.h>
#include <time.h>

//DLL imports
#define DLLSYM

#undef ISOLATE_TESSERACT
#ifndef M_PI
#define M_PI            3.14159265358979323846
#endif
#ifndef NAN
#define NAN _FPCLASS_QNAN //defining the NAN as quiet nan.
#endif

#define fmax max //fmax is not defined, using the template max instead.
#define round(x) floor((x)+0.5)
#ifndef isnan
#define isnan _isnan
#endif
#define strcasecmp _stricmp
#define pipe _pipe
#define close _close
#define dup2 _dup2
#define execvp _execvp

#define int32_t int
#define int64_t __int64

#ifndef isinf
inline int isinf(double x) {
    if ( _fpclass(x) == _FPCLASS_NINF )
        return -1;
    else if ( _fpclass(x) == _FPCLASS_PINF)
        return 1;
    else
        return 0;
}
#endif

#define popen _popen
#define pclose _pclose

inline double drand48() {
    return rand() / (RAND_MAX + 1.0);
}

typedef unsigned char byte;

#endif /* WIN32 */

#endif /* _INC_COMPAT */
