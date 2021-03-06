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
package com.okidokiteam.gouken.plugin.intern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.okidokiteam.gouken.KernelException;
import com.okidokiteam.gouken.KernelWorkflowException;
import com.okidokiteam.gouken.Vault;
import com.okidokiteam.gouken.VaultAgent;
import com.okidokiteam.gouken.VaultConfiguration;
import com.okidokiteam.gouken.VaultConfigurationSource;
import com.okidokiteam.gouken.plugin.PluginCallback;
import com.okidokiteam.gouken.plugin.PluginVault;
import com.okidokiteam.gouken.plugin.VaultPluginPoint;

/**
 *
 */
public class DefaultPluginVault implements PluginVault<Void>
{

    final private List<VaultPluginPoint> m_vaultPluginPoints;
    final private Vault<Void> m_vault;
    private List<PluginCallback> m_callbacks;

    public DefaultPluginVault( Vault<Void> vault, VaultPluginPoint... pluginPoints )
    {
        m_vaultPluginPoints = Arrays.asList( pluginPoints );
        m_vault = vault;

        m_callbacks = new ArrayList<PluginCallback>();
    }


    public void stop()
        throws KernelException, KernelWorkflowException
    {
        m_vault.stop();
    }

    public void registerCallbacks( PluginCallback... callbacks )
    {
        m_callbacks.addAll( Arrays.asList( callbacks ) );
    }

    public void unregisterCallbacks( PluginCallback... callbacks )
    {
        m_callbacks.removeAll( Arrays.asList( callbacks ) );

    }

    public Void start( VaultAgent agent ) throws KernelWorkflowException, KernelException
    {
        return m_vault.start( null );

    }
}
