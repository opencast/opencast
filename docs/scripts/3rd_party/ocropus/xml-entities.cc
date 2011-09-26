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
// Project:
// File: xml-entities.cc
// Purpose: routines for parsing XML predefined and numeric entities
// Responsible: mezhirov
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

#include <string.h>
#include <errno.h>
#include <limits.h>
#include "ocropus.h"

using namespace iulib;
using namespace colib;
using namespace ocropus;

namespace {

    void append_to_nustring(nustring &s, const char *p, int n = -1) {
        nustring t;
        if(n == -1) {
            t.utf8Decode(p, strlen(p));
        } else {
            t.utf8Decode(p, n);
        }
        for(int i = 0; i < t.length(); i++)
            s.push(t[i]);
    }

    long parse_integer(char **pend, char *start, int base) {
        errno = 0;
        long result = strtol(start, pend, base);
        if ((errno == ERANGE && (result == LONG_MAX || result == LONG_MIN))
             || (errno != 0 && result == 0)
             || *pend == start) {
               throw_fmt("number parsing error: %s", start);
        }
        return result;
    }

    // @param start     A string starting with an XML entity, e.g. "&amp;foo".
    // @param dest      A nustring that the parsed entity will be appended to.
    // @returns         The pointer to the position after the entity.
    static char *append_XML_entity(nustring &dest, char *start) {
        CHECK_ARG(*start == '&');
        start++;
        if(*start == '#') {
            // numeric entity
            start++;
            char *end;
            if(*start == 'x') {
                // hexadecimal entity
                start++;
                dest.push(nuchar(parse_integer(&end, start, 16)));
                if(*end != ';')
                    throw_fmt("expected ';', found: %s", end);
            } else {
                // decimal entity;
                dest.push(nuchar(parse_integer(&end, start, 10)));
            }
            if(*end != ';')
                throw_fmt("expected ';', found: %s", end);
            return end + 1;
        } else {
            // symbolic entity
            static struct Entity {
                const char *name;
                int code;
            } entities[] = {
                {"amp", '&'},
                {"lt", '<'},
                {"gt", '>'},
                {"apos", '\''},
                {"quot", '"'},
                {NULL, 0}
            };
            for(int i = 0; entities[i].name; i++) {
                int n = strlen(entities[i].name);
                if(!strncmp(start, entities[i].name, n) && start[n] == ';') {
                    dest.push(nuchar(entities[i].code));
                    return start + n + 1;
                }
            }
            throw_fmt("unknown entity name: %s", start);
        }
        throw "shouldn't get here";
    }

}

namespace ocropus {

    // This function probably logically belongs to Lua
    // but due to the lack of good string manipulation functions there
    // it's actually simpler in C.
    void xml_unescape(nustring &dest, const char *src) {
        CHECK_ARG(src);
        dest.clear();
        const char *begin = src;
        char *p;
        while((p = (char *)strchr(begin, '&'))) {
            append_to_nustring(dest, begin, p - begin);
            begin = append_XML_entity(dest, p);
            CHECK_CONDITION(begin);
        }
        append_to_nustring(dest, begin);
    }

};
