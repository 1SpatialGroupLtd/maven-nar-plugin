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

package org.apache.maven.plugin.nar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;
import org.codehaus.plexus.archiver.AbstractUnArchiver;

/**
 * @author Mark Donszelmann (Mark.Donszelmann@gmail.com)
 * @version $Id$
 */
public abstract class AbstractNarLayout
    implements NarLayout, NarConstants
{
    private Log log;

    protected AbstractNarLayout( AbstractNarMojo abstractNarMojo )
    {
        this.log = abstractNarMojo.getLog();
    }

    protected Log getLog()
    {
        return log;
    }

    protected final void attachNar( ArchiverManager archiverManager, MavenProjectHelper projectHelper,
                                    MavenProject project, String classifier, File dir, String include )
        throws MojoExecutionException
    {
        File narFile =
            new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-" + classifier + "."
                + NarConstants.NAR_EXTENSION );
        if ( narFile.exists() )
        {
            narFile.delete();
        }
        try
        {
            Archiver archiver = archiverManager.getArchiver( NarConstants.NAR_ROLE_HINT );
            archiver.addDirectory( dir, new String[] { include }, null );
            archiver.setDestFile( narFile );
            archiver.createArchive();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "NAR: cannot find archiver", e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "NAR: cannot create NAR archive '" + narFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: cannot create NAR archive '" + narFile + "'", e );
        }
        projectHelper.attachArtifact( project, NarConstants.NAR_TYPE, classifier, narFile );
    }

    protected void unpackNarAndProcess( ArchiverManager archiverManager
                                      , File file
                                      , File narLocation
                                      , String os
                                      , String linkerName
                                      , AOL defaultAOL
                                      )
      throws MojoExecutionException, MojoFailureException
    {
        System.out.println("************* Entered unpackNarAndProcess ****************");
        final String gpp = "g++";
        final String gcc = "gcc";

        narLocation.mkdirs();

        // unpack
        try
        {
            UnArchiver unArchiver;
            unArchiver = archiverManager.getUnArchiver( NarConstants.NAR_ROLE_HINT );
            unArchiver.setSourceFile( file );
            unArchiver.setDestDirectory( narLocation );
            AbstractUnArchiver arch = (AbstractUnArchiver)unArchiver;
            System.out.println("************* Overwrite: " + arch.isOverwrite() + "****************");
            unArchiver.setOverwrite(false);
            FileSelector[] fss = unArchiver.getFileSelectors();
            if (fss == null)
            {
              System.out.println("************* Found file selectors ****************");
            }
            else
            {
              System.out.println("************* NO file selectors ****************");
            }

            // Set file selectors.
            IncludeExcludeFileSelector fs_new = new IncludeExcludeFileSelector();
            fs_new.setIncludes(null);
            fs_new.setExcludes(null);
            IncludeExcludeFileSelector[] fss_new = {fs_new};
            unArchiver.setFileSelectors(fss_new);

            unArchiver.extract();
        }
        catch ( NoSuchArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + narLocation, e );
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "Error unpacking file: " + file + " to: " + narLocation, e );
        }

        // process
        if ( !NarUtil.getOS( os ).equals( OS.WINDOWS ) )
        {
            NarUtil.makeExecutable( new File( narLocation, "bin/" + defaultAOL ), log );
            // FIXME clumsy
            if ( defaultAOL.hasLinker( gpp ) )
            {
                NarUtil.makeExecutable( new File( narLocation, "bin/"
                    + NarUtil.replace( gpp, gcc, defaultAOL.toString() ) ), log );
            }
            // add link to versioned so files
            NarUtil.makeLink( new File( narLocation, "lib/" + defaultAOL ), log );
        }
        if ( linkerName.equals( gcc ) || linkerName.equals( gpp ) )
        {
            NarUtil.runRanlib( new File( narLocation, "lib/" + defaultAOL ), log );
            // FIXME clumsy
            if ( defaultAOL.hasLinker( gpp ) )
            {
                NarUtil.runRanlib(
                                   new File( narLocation, "lib/" + NarUtil.replace( gpp, gcc, defaultAOL.toString() ) ),
                                   log );
            }
        }
        if ( NarUtil.getOS( os ).equals( OS.MACOSX ) )
        {
            File[] dylibDirs = new File[2];
            dylibDirs[0] = new File( narLocation, "lib/" + defaultAOL + "/" + Library.SHARED );
            dylibDirs[1] = new File( narLocation, "lib/" + defaultAOL + "/" + Library.JNI );

            NarUtil.runInstallNameTool( dylibDirs, log );
        }

        System.out.println("************* Exiting unpackNarAndProcess ****************");
    }

    public String getConfiguration()
    {
        return "";
    }

    /**
     * @return
     * @throws MojoExecutionException
     */
    public static NarLayout getLayout( AbstractNarMojo abstractNarMojo )
        throws MojoExecutionException
    {
        String layoutName = abstractNarMojo.getLayoutName();
        String className =
            layoutName.indexOf( '.' ) < 0 ? NarLayout21.class.getPackage().getName() + "." + layoutName : layoutName;
        abstractNarMojo.getLog().debug( "Using " + className );
        Class cls;
        try
        {
            cls = Class.forName( className );
            Constructor ctor = cls.getConstructor( new Class[] { AbstractNarMojo.class } );
            return (NarLayout) ctor.newInstance( new Object[] { abstractNarMojo } );
        }
        catch ( ClassNotFoundException e )
        {
            throw new MojoExecutionException( "Cannot find class for layout " + className, e );
        }
        catch ( InstantiationException e )
        {
            throw new MojoExecutionException( "Cannot instantiate class for layout " + className, e );
        }
        catch ( IllegalAccessException e )
        {
            throw new MojoExecutionException( "Cannot access class for layout " + className, e );
        }
        catch ( SecurityException e )
        {
            throw new MojoExecutionException( "Cannot access class for layout " + className, e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new MojoExecutionException( "Cannot find ctor(Log) for layout " + className, e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new MojoExecutionException( "Wrong arguments ctor(Log) for layout " + className, e );
        }
        catch ( InvocationTargetException e )
        {
            throw new MojoExecutionException( "Cannot invokector(Log) for layout " + className, e );
        }
    }
}
