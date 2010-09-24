/*
 * Copyright 2009 Toni Menzel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okidokiteam.gouken.config;

import org.apache.felix.dependencymanager.DependencyActivatorBase;
import org.apache.felix.dependencymanager.DependencyManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

/**
 * @author Toni Menzel
 * @since Mar 10, 2010
 */

public class Activator extends DependencyActivatorBase {

	public void init(BundleContext context, DependencyManager manager) throws Exception {
		manager.add(createService()
				.setImplementation(new Configure())
				.add(createServiceDependency().setService(ConfigurationAdmin.class).setRequired(true))
				.add(createServiceDependency().setService(LogService.class).setRequired(false)));
	}

	public void destroy(BundleContext context, DependencyManager manager) throws Exception {
		// do nothing
	}

}
