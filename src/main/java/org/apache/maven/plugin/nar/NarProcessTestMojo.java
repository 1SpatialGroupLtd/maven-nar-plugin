package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.LinkerDef;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Compiles native source files.
 *
 * @goal nar-processTestResources
 * @phase process-test-resources
 * @requiresSession
 * @requiresProject
 * @requiresDependencyResolution test
 * @author Mark Donszelmann
 */
public class NarProcessTestMojo
    extends NarCompileMojo
{

    protected File getDestinationDirectory()
    {
        return getTestTargetDirectory();
    }

    protected String getSourcesMessage(int noOfSources)
    {
        if (noOfSources > 0)
        {
            return "Preparing to link " + noOfSources + " files";
        }
        else
        {
            return "Nothing to link";
        }
    }

    protected void setLanguageLinkers(CCTask task, Library library)
    {
        // do nothing
    }

    protected String getObjectDirectoryName()
    {
        return "source_obj";
    }

    protected void setCompilerOptions(CCTask task, String type) throws MojoFailureException, MojoExecutionException
    {
        // do nothing
    }

    protected String getScope()
    {
        return "test";
    }

    protected File getDestinationUnpackDirectory()
    {
        return getTestUnpackDirectory();
    }

    protected boolean excludeDependency(NarArtifact narDependency) throws MojoExecutionException, MojoFailureException
    {
        return getTestExcludeDependencies().contains(narDependency.getArtifactId());
    }

    protected void addObjectFilesToLinker(LinkerDef linkerDefinition) throws MojoFailureException, MojoExecutionException
    {
        // Add obj files to the linker
        File sourceObjectDir = new File(getTargetDirectory(), "obj");
        sourceObjectDir = new File(sourceObjectDir, getAOL().toString());

        if (sourceObjectDir.exists())
        {
            getLog().debug( "Adding files from Library Directory " + sourceObjectDir);
            addObjFiles(linkerDefinition, sourceObjectDir);
        }
        else
        {
            getLog().debug( "Library Directory " + sourceObjectDir + " does NOT exist." );
        }
    }
}
