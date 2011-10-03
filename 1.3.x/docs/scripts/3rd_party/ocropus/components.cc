// Copyright 2006 Deutsches Forschungszentrum fuer Kuenstliche Intelligenz
// or its licensors, as applicable.
// Copyright 1995-2005 by Thomas M. Breuel
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
// File: 
// Purpose: 
// Responsible: 
// Reviewer:
// Primary Repository:
// Web Sites: www.iupr.org, www.dfki.de

#include <unistd.h>
#include "colib/colib.h"
#include "iulib/iulib.h"
#include "components.h"

extern char **environ;

using namespace colib;

namespace {
    param_bool fatal_unknown_params("fatal_unknown_params",1,"unknown parameters for a class are a fatal error");
}

namespace ocropus {
    ////////////////////////////////////////////////////////////////
    // constructing, loading, and saving classifiers
    ////////////////////////////////////////////////////////////////

    // a global variable that can be used to override the environment
    // parameter verbose_params; mainly used for letting us write a command
    // line program to print the default parameters for components
    const char *global_verbose_params;

    void IComponent::check_parameters_() {
        // TODO/tmb rewrite this more cleanly in terms of iustring
        if(checked) return;
        checked = true;
        iucstring prefix = this->name();
        prefix += "_";
        for(int i=0;environ[i];i++) {
            iucstring entry = environ[i];
            if(!entry.compare(0, prefix.length(), prefix)) {
                int where = entry.find('=');
                if(where >= 0) {
                    entry.erase(where);
                    if(!params.find(entry.substr(prefix.length()))) {
                        if(fatal_unknown_params)
                            throwf("%s: unknown environment variable for %s\n"
                                   "(set fatal_unknown_params=0 to disable this check)\n",
                                   entry.c_str(),name());
                        else
                            debugf("info","%s: unknown environment variable for %s\n",
                                   entry.c_str(),name());
                    }
                }
            }
        }
    }

    strhash<IComponentConstructor*> constructors;

    void component_register_(const char *name,IComponentConstructor *f,bool replace) {
        if(constructors.find(name)) {
            if(!replace) throwf("%s: already registered as a component",name);
            delete constructors(name);
        }
        constructors(name) = f;
    }

    IComponentConstructor *component_lookup(const char *name) {
        if(!constructors.find(name)) throwf("%s: no such component",name);
        return constructors(name);
    }

    IComponent *component_construct(const char *name) {
        if(!strcmp(name,"null")) return 0;
        IComponentConstructor *f = component_lookup(name);
        return (*f)();
    }

    void list_components(narray<const char *> &names) {
        constructors.keys(names);
    }

    static int level = 0;

    void save_component(FILE *stream,IComponent *classifier) {
        if(!classifier) {
            debugf("iodetail","%*s[writing OBJ:NULL]\n",level,"");
            debugf("iodetail","OBJ:NULL",stream);
        } else {
            debugf("iodetail","%*s[writing OBJ:%s]\n",level,"",classifier->name());
            level++;
            fputs("OBJ:",stream);
            fputs(classifier->name(),stream);
            fputs("\n",stream);
            classifier->save(stream);
            fputs("OBJ:END\n",stream);
            level--;
            debugf("iodetail","%*s[done]\n",level,"");
        }
    }

    IComponent *load_component(FILE *stream) {
        char buf[1000];
        fgets(buf,sizeof buf,stream);
        if(strlen(buf)>0) buf[strlen(buf)-1] = 0;
        debugf("iodetail","%*s[got %s]\n",level,"",buf);
        IComponent *result = 0;
        if(strcmp(buf,"OBJ:NULL")) {
            level++;
            CHECK(!strncmp(buf,"OBJ:",4));
            result = component_construct(buf+4);
            result->load(stream);
            fgets(buf,sizeof buf,stream);
            if(strlen(buf)>0) buf[strlen(buf)-1] = 0;
            ASSERT(!strcmp(buf,"OBJ:END"));
            level--;
        }
        debugf("iodetail","%*s[done]\n",level,"");
        return result;
    }
}

