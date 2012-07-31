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
import java.io.IOException;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Copies any resources, including AOL specific distributions, to the target area for packaging
 * 
 * @goal nar-resources
 * @phase process-resources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarResourcesMojo
    extends AbstractResourcesMojo
{
    /**
     * Use given AOL only. If false, copy for all available AOLs.
     * 
     * @parameter expression="${nar.resources.copy.aol}" default-value="true"
     * @required
     */
    private boolean resourcesCopyAOL;

    /**
     * Directory for nar resources. Defaults to src/nar/resources
     * 
     * @parameter expression="${basedir}/src/nar/resources"
     * @required
     */
    private File resourceDirectory;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // noarch resources
        String version = getMavenProject().getVersion();
		try
        {
            int copied = 0;
            File noarchDir = new File( resourceDirectory, NarConstants.NAR_NO_ARCH );
            if ( noarchDir.exists() )
            {
                File noarchDstDir = getLayout().getNoArchDirectory( getTargetDirectory(), getMavenProject().getArtifactId(), version );
                getLog().debug( "Copying noarch from " + noarchDir + " to " + noarchDstDir );
                copied += NarUtil.copyDirectoryStructure( noarchDir, noarchDstDir, null, NarUtil.DEFAULT_EXCLUDES );
            }
            getLog().info( "Copied " + copied + " resources" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: Could not copy resources", e );
        }
        
        // scan resourceDirectory for AOLs
        File aolDir = new File( resourceDirectory, NarConstants.NAR_AOL );
        if ( aolDir.exists() )
        {
            String[] aol = aolDir.list();
            for ( int i = 0; i < aol.length; i++ )
            {
                // copy only resources of current AOL
                if ( resourcesCopyAOL && ( !aol[i].equals( getAOL().toString() ) ) )
                {
                    continue;
                }

                boolean ignore = false;
                for ( Iterator j = FileUtils.getDefaultExcludesAsList().iterator(); j.hasNext(); )
                {
                    String exclude = (String) j.next();
                    if ( SelectorUtils.matchPath( exclude.replace( '/', File.separatorChar ), aol[i] ) )
                    {
                        ignore = true;
                        break;
                    }
                }
                if ( !ignore )
                {
                    File aolFile = new File( aolDir, aol[i] );
                    copyResources( aolFile, aolFile.getName() );
                    //We need to add library files that do not follow the nar naming convention to the modules properties file.
                    addLibFilesToProperties( version, aol[i] );
                }
            }
        }
    }

    // Adds library files to the narInfo properties for this module.
    // Only file names, not their extensions should be added.
    private void addLibFilesToProperties(String version, String aol)
		throws MojoExecutionException, MojoFailureException
    {
		NarInfo narInfo = getNarInfo();
		for ( Iterator it = getLibraries().iterator(); it.hasNext(); )
		{
		    Library library = (Library) it.next();
		    String type = library.getType();
		    File libDstDir =
		        getLayout().getLibDirectory( getTargetDirectory(), getMavenProject().getArtifactId(),
		                                     version, aol, type );

		    getLog().debug( "Adding lib files from " + libDstDir + " to properties");
		    File[] libFiles = libDstDir.listFiles(); //getLibFilesInDirectory(libDstDir);
		    for( int index = 0; index < libFiles.length; index++ )
		    {
				File file = libFiles[index];
				int extensionIndex = file.getName().indexOf( "." );
				narInfo.addLibrary(new AOL(aol), file.getName().substring( 0, extensionIndex ) );
		    }
		}
        saveNarInfoToFile(narInfo);
	}
}
