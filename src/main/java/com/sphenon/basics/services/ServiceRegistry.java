package com.sphenon.basics.services;

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
import com.sphenon.basics.context.classes.*;
import com.sphenon.basics.configuration.*;
import com.sphenon.basics.message.*;
import com.sphenon.basics.notification.*;
import com.sphenon.basics.customary.*;

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

public class ServiceRegistry {

    static protected Vector<Service> services;
    static protected Vector<Service> getServices(CallContext context) {
        if (services == null) {
            services = new Vector<Service>();
        }
        return services;
    }

    static protected Vector<Consumer> consumers;
    static protected Vector<Consumer> getConsumers(CallContext context) {
        if (consumers == null) {
            consumers = new Vector<Consumer>();
        }
        return consumers;
    }

    static protected Map<Consumer,Vector<Service>> consumer_services;
    static public<S extends Service> Vector<S> getConsumerServices(CallContext context, Consumer<S> consumer) {
        Vector<Service> result = null;
        if (consumer_services == null) {
            consumer_services = new HashMap<Consumer,Vector<Service>>();
        } else {
            result = consumer_services.get(consumer);
        }
        if (result == null) {
            result = new Vector<Service>();
            consumer_services.put(consumer, result);
        }
        return (Vector<S>) result;
    }
    
    static public<S extends Service> Vector<S> getServices(CallContext context, Class<S> service_class) {
        Vector<S> result = new Vector<S>();
        SERVICES: for (Service service : getServices(context)) {
            if (service_class.isAssignableFrom(service.getClass())) {
                result.add((S) service);
            }
        }
        return result;
    }

    static synchronized public void registerService(CallContext context, Service service) {
        getServices(context).add(service);

        CONSUMERS: for (Consumer consumer : getConsumers(context)) {
            if (consumer.getServiceClass(context).isAssignableFrom(service.getClass())) {
                Vector<Service> css = getConsumerServices(context, consumer);
                for (Service cs : css) {
                    if (cs.equals(service)) {
                        continue CONSUMERS;
                    }
                }
                css.add(service);
                consumer.notifyNewService(context, service);
                service.notifyNewConsumer(context, consumer);
            }
        }
    }

    static synchronized public void registerConsumer(CallContext context, Consumer consumer) {
        getConsumers(context).add(consumer);

        Vector<Service> css = getConsumerServices(context, consumer);
        SERVICES: for (Service service : getServices(context)) {
            if (consumer.getServiceClass(context).isAssignableFrom(service.getClass())) {
                for (Service cs : css) {
                    if (cs.equals(service)) {
                        continue SERVICES;
                    }
                }
                css.add(service);
                consumer.notifyNewService(context, service);
                service.notifyNewConsumer(context, consumer);
            }
        }
    }
}
