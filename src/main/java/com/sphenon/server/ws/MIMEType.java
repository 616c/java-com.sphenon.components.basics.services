package com.sphenon.server.ws;

/****************************************************************************
  Copyright 2001-2018 Sphenon GmbH

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  License for the specific language governing permissions and limitations
  under the License.
*****************************************************************************/

import com.sphenon.basics.context.*;
import com.sphenon.basics.cache.*;
import com.sphenon.basics.exception.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;
import com.sphenon.basics.data.*;
import com.sphenon.basics.system.*;
import com.sphenon.basics.services.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import java.util.regex.*;

public class MIMEType implements ContextAware {

    public MIMEType(CallContext context) {
        this.media_range_parameters = new ArrayList<Named<String>>();
        this.accept_parameters = new ArrayList<Named<String>>();
        this.quality = 1.0F;
    }

    static protected String WILDCARD = "*";
    static protected String MIME_RE = "([A-Za-z0-9!#$%&'*+.^_`|~-]+)/([A-Za-z0-9!#$%&'*+.^_`|~-]+)";
    static protected Pattern mime_re;

    public MIMEType(CallContext context, String mime_type) {
        this(context);
        if (mime_re == null) {
            try {
                mime_re = Pattern.compile(MIME_RE);
            } catch (PatternSyntaxException pse) {
                CustomaryContext.create(Context.create(context)).throwAssertionProvedFalse(context, pse, "Syntax error in regular expression '%(regexp)'", "regexp", MIME_RE);
                throw (ExceptionAssertionProvedFalse) null; // compiler insists
            }
        }

        Matcher matcher;
        if (mime_type == null || (matcher = mime_re.matcher(mime_type)).find() == false) {
            CustomaryContext.create((Context)context).throwPreConditionViolation(context, "Invalid MIME type '%(mimetype)'", "mimetype", mime_type);
            throw (ExceptionPreConditionViolation) null; // compiler insists
        }

        this.type = matcher.group(1);
        this.sub_type = matcher.group(2);
    }

    protected int position;

    public int getPosition (CallContext context) {
        return this.position;
    }

    public void setPosition (CallContext context, int position) {
        this.position = position;
    }

    protected String type;

    public String getType (CallContext context) {
        return this.type;
    }

    public void setType (CallContext context, String type) {
        this.type = type;
    }

    protected String sub_type;

    public String getSubType (CallContext context) {
        return this.sub_type;
    }

    public void setSubType (CallContext context, String sub_type) {
        this.sub_type = sub_type;
    }

    protected float quality;

    public float getQuality (CallContext context) {
        return this.quality;
    }

    public void setQuality (CallContext context, float quality) {
        this.quality = quality;
    }

    protected List<Named<String>> media_range_parameters;

    public List<Named<String>> getMediaRangeParameters (CallContext context) {
        return this.media_range_parameters;
    }

    public void setMediaRangeParameters (CallContext context, List<Named<String>> media_range_parameters) {
        this.media_range_parameters = media_range_parameters;
    }

    protected List<Named<String>> accept_parameters;

    public List<Named<String>> getAcceptParameters (CallContext context) {
        return this.accept_parameters;
    }

    public void setAcceptParameters (CallContext context, List<Named<String>> accept_parameters) {
        this.accept_parameters = accept_parameters;
    }

    public boolean matches(CallContext context, String other) {
        return matches(context, new MIMEType(context, other));
    }

    public boolean matches(CallContext context, MIMEType other) {
        boolean matches = (    (    WILDCARD.equals(this.type)
                                 || WILDCARD.equals(other.getType(context))
                                 || this.type.equals(other.getType(context))
                               )
                            && (    WILDCARD.equals(this.sub_type)
                                 || WILDCARD.equals(other.getSubType(context))
                                 || this.sub_type.equals(other.getSubType(context))
                               )
                          );
        System.err.println("matches: " + this.toString(context) + " and " + this.toString(context) + " => " + matches);
        return matches;
    }

    static protected class MIMETypeComparator implements java.util.Comparator {
        protected CallContext context;
        public MIMETypeComparator(CallContext context) {
            this.context = context;
        }
        public int compare(Object o1, Object o2) {
            MIMEType mt1  = (MIMEType) o1;
            MIMEType mt2  = (MIMEType) o2;
            String   t1   = mt1.getType(context);
            String   st1  = mt1.getSubType(context);
            boolean  wt1  = t1.equals(WILDCARD);
            boolean  wst1 = st1.equals(WILDCARD);
            String   t2   = mt2.getType(context);
            String   st2  = mt2.getSubType(context);
            boolean  wt2  = t2.equals(WILDCARD);
            boolean  wst2 = st2.equals(WILDCARD);
            int      mrp1 = mt1.getMediaRangeParameters(context).size();
            int      mrp2 = mt2.getMediaRangeParameters(context).size();
            float    q1   = mt1.getQuality(context);
            float    q2   = mt2.getQuality(context);
            int      p1   = mt1.getPosition(context);
            int      p2   = mt2.getPosition(context);

            return   ! wt1 &&   wt2    ? -1
                   :   wt1 && ! wt2    ?  1
                   : ! t1.equals(t2)   ? (p1 < p2 ? -1 : 1)
                   :  ! wst1 &&   wst2  ? -1
                   :    wst1 && ! wst2  ?  1
                   : ! st1.equals(st2) ? (p1 < p2 ? -1 : 1)
                   :  mrp1 >  0 && mrp2 == 0  ? -1
                   :  mrp1 == 0 && mrp2 >  0  ?  1
                   :  q1 > q2 ? -1 : 1;
        }
    }

    static public void sort(CallContext context, List<MIMEType> list) {
        Collections.sort(list, new MIMETypeComparator(context));
    }

    public String toString(CallContext context) {
        return   this.type + "/" + this.sub_type
               + StringUtilities.join(context, this.media_range_parameters, null, null, null, ";", null, true, true)
               + ";q=" + String.format("%.3f", this.quality)
               + StringUtilities.join(context, this.accept_parameters, null, null, null, ";", null, true, true);
    }
}
