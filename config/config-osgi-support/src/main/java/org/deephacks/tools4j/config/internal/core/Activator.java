/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.config.internal.core;

import java.util.Dictionary;
import java.util.Properties;

import org.deephacks.tools4j.config.RuntimeContext;
import org.deephacks.tools4j.config.admin.AdminContext;
import org.deephacks.tools4j.config.internal.core.admin.AdminCoreContext;
import org.deephacks.tools4j.config.internal.core.runtime.RuntimeCoreContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    private Logger logger = LoggerFactory.getLogger(Activator.class);
    ServiceRegistration adminContext;
    ServiceRegistration runtimeContext;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Starting " + this);
        Dictionary d = new Properties();
        adminContext = context.registerService(AdminContext.class, new AdminCoreContext(), d);
        logger.debug("Registered {}", adminContext);
        runtimeContext = context.registerService(RuntimeContext.class, new RuntimeCoreContext(), d);
        logger.debug("Registered {}", runtimeContext);
    }

    @Override
    public void stop(BundleContext context) throws Exception {

    }

}
