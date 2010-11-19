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
package com.okidokiteam.gouken.kernel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.okidokiteam.gouken.KernelException;
import com.okidokiteam.gouken.KernelWorkflowException;
import com.okidokiteam.gouken.Vault;
import com.okidokiteam.gouken.VaultConfiguration;
import com.okidokiteam.gouken.VaultConfigurationSource;
import com.okidokiteam.gouken.kernel.ma.Activator;
import com.okidokiteam.gouken.kernel.ma.DPVaultAgent;
import org.apache.commons.discovery.tools.DiscoverSingleton;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.pax.repository.ArtifactQuery;
import org.ops4j.pax.repository.RepositoryException;
import org.ops4j.pax.repository.Resolver;
import org.ops4j.pax.swissbox.tinybundles.core.TinyBundle;

import static org.ops4j.pax.repository.base.RepositoryFactory.*;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.*;

/**
 * This Vault actually knows about OSGi, it actually boots a fw, provisions it and manages its lifecycle.
 * Beyond this, we should not have the notion of osgi other than DeploymentPackages. (also with another name as we probably just want a subset of that spec).
 * 
 * This Vault uses:
 * - Apache Felix as underlying OSGi implementation
 * - DeploymentAdmin from Felix as management agent implementation
 * - Tinybundles to transfer update() into units recognizable by the DeploymentAdmin
 * 
 * This makes the update mechanism dynamic (OSGi), atomic (DeploymentAdmin) and configuration agnostic (Tinybundles).
 * 
 * Because we deal with DP like Apache ACE does, we may or may not use the ACE MA here.
 * Difference is that we just have ONE management agent with one single server.(plus one client).. all in this one vault.
 * So we may make things simpler..
 * 
 * @author Toni Menzel
 * @since Mar 4, 2010
 */
public class CoreVault implements Vault
{

    private static final Logger LOG = LoggerFactory.getLogger( CoreVault.class );
    private static final String META_INF_GOUKEN_KERNEL_PROPERTIES = "/META-INF/gouken/kernel.properties";
    private static final String BUNDLE_DEPLOYMENTADMIN = "org.apache.felix:org.apache.felix.dependencymanager:3.0.0-SNAPSHOT";

    private static final String BUNDLE_DM = "org.apache.felix:org.apache.felix.deploymentadmin:0.9.0-SNAPSHOT";
    // private static final String BUNDLE_LOGGING_API = "org.slf4j:slf4j-api:1.6.1";
    private static final String BUNDLE_LOGGING_API = "org.ops4j.pax.logging:pax-logging-api:1.5.1";

    private static final String BUNDLE_CMPD = "org.osgi:org.osgi.compendium:4.2.0";

    // accessed by shutdownhook and remote access
    private volatile Framework m_framework;
    private final File m_workDir;
    private final Resolver m_resolver;

    public CoreVault(
            File workDir,
            Resolver resolver,
            String... extraPackages )
    {
        assert workDir != null : "workDir must not be null.";
        assert resolver != null : "resolver must not be null.";

        m_workDir = workDir;
        m_resolver = resolver;

    }

    public synchronized VaultConfigurationSource start()
        throws KernelWorkflowException, KernelException
    {
        if (isRunning())
        {
            throw new KernelWorkflowException( "Vault is already running." );
        }

        ClassLoader parent = null;
        VaultConfiguration initialConfig;
        try
        {
            final Map<String, Object> p = getFrameworkConfig();
            parent = Thread.currentThread().getContextClassLoader();

            Thread.currentThread().setContextClassLoader( null );
            loadAndStartFramework( p );
            Thread.currentThread().setContextClassLoader( parent );


        } catch (Exception e)
        {
            // kind of a clean the mess up..
            tryShutdown();
            throw new KernelException( "Problem starting the Vault", e );

        } finally
        {
            if (parent != null)
            {
                Thread.currentThread().setContextClassLoader( parent );

            }
        }

        return new DPVaultConfigurationSource();
    }

    private void install( BundleContext context, String... artifacts )
        throws RepositoryException, IOException, BundleException
    {
        for (String artifact : artifacts)
        {
            ArtifactQuery a = createQuery( artifact );
            context.installBundle( a.getQueryString(), m_resolver.find( a ).getContent().get() );
        }
        for (Bundle b : context.getBundles())
        {
            try
            {
                b.start();
                LOG.info( "Installed: " + b.getSymbolicName() + " --> " + b.getState() );
            } catch (Exception e)
            {
                LOG.warn( "Not started: " + b.getSymbolicName() + " - " + e.getMessage() );

            }
        }
    }

    public void update( VaultConfiguration configuration )
        throws KernelException
    {
        // access the underlying, osgi based agent / delegate
        new VaultAgentClient( m_framework.getBundleContext() ).update( configuration );
    }

    public synchronized void stop()
        throws KernelException
    {
        try
        {
            LOG.info( "Stop hook triggered." );
            if (m_framework != null)
            {
                BundleContext ctx = m_framework.getBundleContext();
                Bundle systemBundle = ctx.getBundle( 0 );
                systemBundle.stop();
                m_framework = null;
            }
            System.gc();
            LOG.info( "Shutdown complete." );
        } catch (BundleException e)
        {
            LOG.error( "Problem stopping framework.", e );
        }

    }

    private Map<String, Object> getFrameworkConfig()
        throws IOException
    {
        InputStream ins = getClass().getResourceAsStream( META_INF_GOUKEN_KERNEL_PROPERTIES );
        Properties descriptor = new Properties();
        if (ins != null)
        {
            descriptor.load( ins );
        }

        final Map<String, Object> p = new HashMap<String, Object>();
        File worker = new File( m_workDir, "framework" );

        p.put( "org.osgi.framework.storage", worker.getAbsolutePath() );
        // p.put( "felix.log.level", "1" );

        // TODO: This is shared stuff. We (the host) may load classes. The underlying FW must the same classes or you will be penetrated with ClassCastExceptions.
        p.put( "org.osgi.framework.system.packages.extra", "com.okidokiteam.gouken,org.ops4j.pax.repository,org.ops4j.base.io" );

        for (Object key : descriptor.keySet())
        {
            p.put( (String) key, descriptor.getProperty( (String) key ) );
        }
        return p;
    }

    private void loadAndStartFramework( Map<String, Object> p )
        throws BundleException, IOException, RepositoryException, KernelException
    {
        FrameworkFactory factory = (FrameworkFactory) DiscoverSingleton.find( FrameworkFactory.class );
        m_framework = factory.newFramework( p );

        // p.put( FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, Arrays.asList( new Activator() ) );
        // m_framework = new Felix( p );
        m_framework.init();
        m_framework.start();

        installManagementAgent();

    }

    private void installManagementAgent() throws RepositoryException, IOException, BundleException
    {
        install( m_framework.getBundleContext()
                , BUNDLE_DM
                , BUNDLE_DEPLOYMENTADMIN
                , BUNDLE_CMPD
                , BUNDLE_LOGGING_API
                , "org.apache.felix:org.apache.felix.eventadmin:1.2.2"
                , "javax.servlet:servlet-api:2.4"
                , "org.apache.felix:org.apache.felix.configadmin:1.2.4"

                , "org.apache.ace:ace-deployment-api:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-range-api:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-discovery-api:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-identification-api:0.8.0-SNAPSHOT"

                , "org.apache.ace:ace-deployment-deploymentadmin:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-deployment-task:0.8.0-SNAPSHOT"

                , "org.apache.ace:ace-consolelogger:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-discovery-property:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-identification-property:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-scheduler:0.8.0-SNAPSHOT"

                , "org.apache.ace:ace-log:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-log-listener:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-gateway-log:0.8.0-SNAPSHOT"
                , "org.apache.ace:ace-gateway-log-store:0.8.0-SNAPSHOT"

        );
        // MA
        // installDynamicBundle( "GoukenManagementAgent", Activator.class, DPVaultAgent.class ).start();

    }

    private Bundle installDynamicBundle( String name, Class a, Class... extraContent )
        throws BundleException
    {
        TinyBundle tb = newBundle();

        for (Class c : extraContent)
        {
            tb.add( c );
        }

        if (a != null)
        {
            tb.add( a );
            tb.set( "Bundle-Activator", a.getName() );
        }

        return m_framework.getBundleContext().installBundle( name, tb.build( withBnd() ) );
    }

    private void tryShutdown()
    {
        if (m_framework != null)
        {
            try
            {
                m_framework.stop();

            } catch (Exception e)
            {
                // dont care.
            }
        }
    }

    private boolean isRunning()
    {
        return ( m_framework != null );
    }
}
