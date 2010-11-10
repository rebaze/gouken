/*
 * Copyright (C) 2010 Okidokiteam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okidokiteam.gouken.plugin.remotes;

import com.okidokiteam.gouken.plugin.PluginRemote;
import org.ops4j.pax.repository.ArtifactIdentifier;

/**
 * Local instrumentation without actually having a real management agent installed inside the vault.
 */
public class LocalPluginRemote implements PluginRemote
{

    public LocalPluginRemote(  )
    {
        
    }

    public void install( ArtifactIdentifier identifier )
    {

    }

    public void uninstall( ArtifactIdentifier identifier )
    {

    }
}
