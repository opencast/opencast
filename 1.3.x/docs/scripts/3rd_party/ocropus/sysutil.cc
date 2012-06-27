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
// File: sysutil.cc
// Purpose: miscelaneous routines
// Responsible: mezhirov
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

#include <time.h>
#ifndef WIN32
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/resource.h>
#else
#include <sys/stat.h>
#endif
#include "ocropus.h"

namespace ocropus {

#ifndef WIN32 //WIN32: These functions are not used in the libraries
    double user_time() {
        struct rusage usage;
        memset(&usage,0,sizeof usage);
        getrusage(RUSAGE_SELF,&usage);
        return usage.ru_utime.tv_sec + 0.000001 * usage.ru_utime.tv_usec;
    }
    double system_time() {
        struct rusage usage;
        memset(&usage,0,sizeof usage);
        getrusage(RUSAGE_SELF,&usage);
        return usage.ru_stime.tv_sec + 0.000001 * usage.ru_stime.tv_usec;
    }
    double heap_memory() {
        struct rusage usage;
        memset(&usage,0,sizeof usage);
        getrusage(RUSAGE_SELF,&usage);
        return usage.ru_idrss;
    }
    double stack_memory() {
        struct rusage usage;
        memset(&usage,0,sizeof usage);
        getrusage(RUSAGE_SELF,&usage);
        return usage.ru_isrss;
    }
    double page_faults() {
        struct rusage usage;
        memset(&usage,0,sizeof usage);
        getrusage(RUSAGE_SELF,&usage);
        return usage.ru_majflt;
    }
#endif

    double now() {
        struct timeval time;
        gettimeofday(&time,0);
        return time.tv_sec + 0.000001 * time.tv_usec;
    }

    Timer::Timer() {
        total = 0.0;
        start_time = 0.0;
    }
    void Timer::reset() {
        total = 0.0;
        start_time = 0.0;
    }
    void Timer::operator+() {
        if(start_time!=0.0) throw "timer already running";
        start_time = now();
    }
    void Timer::operator-() {
        if(start_time==0.0) throw "timer not running";
        total += now() - start_time;
        start_time = 0.0;
    }
    double Timer::operator*() {
        if(start_time==0.0) return total;
        else return total + (now()-start_time);
    }
    void mkdir_if_necessary(const char *path) {
        struct stat buf;
        if(stat(path, &buf))
#ifndef WIN32
            mkdir(path, S_IRWXU);
#else
            mkdir(path);
#endif
    }
}
