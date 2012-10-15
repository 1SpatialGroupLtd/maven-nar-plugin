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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author Mark Donszelmann
 */
public abstract class AbstractCompileMojo
    extends AbstractDependencyMojo
{

    /**
     * C++ Compiler
     *
     * @parameter expression=""
     */
    private Cpp cpp;

    /**
     * C Compiler
     *
     * @parameter expression=""
     */
    private C c;

    /**
     * Fortran Compiler
     *
     * @parameter expression=""
     */
    private Fortran fortran;

    /**
     * Maximum number of Cores/CPU's to use. 0 means unlimited.
     *
     * @parameter expression=""
     */
    private int maxCores = 0;

    /**
     * Name of the output
     *
     * @parameter expression="${project.artifactId}-${project.version}"
     */
    private String output;

    /**
     * List of artifact ids for dependencies to be excluded during test phases.
     *
     * @parameter expression=""
     */
    private List testExcludeDependencies;

    /**
     * Fail on compilation/linking error.
     *
     * @parameter expression="" default-value="true"
     * @required
     */
    private boolean failOnError;

    /**
     * Sets the type of runtime library, possible values "dynamic", "static".
     *
     * @parameter expression="" default-value="dynamic"
     * @required
     */
    private String runtime;

    /**
     * Set use of libtool. If set to true, the "libtool " will be prepended to the command line for compatible
     * processors.
     *
     * @parameter expression="" default-value="false"
     * @required
     */
    private boolean libtool;

    /**
     * The home of the Java system. Defaults to a derived value from ${java.home} which is OS specific.
     *
     * @parameter expression=""
     * @readonly
     */
    private File javaHome;

    /**
     * List of libraries to create
     *
     * @parameter expression=""
     */
    private List libraries;

    /**
     * List of tests to create
     *
     * @parameter expression=""
     */
    private List tests;

    /**
     * Javah info
     *
     * @parameter expression=""
     */
    private Javah javah;

    /**
     * Java info for includes and linking
     *
     * @parameter expression=""
     */
    private Java java;

    private NarInfo narInfo;

    private List/* <String> */dependencyLibOrder;

    private Project antProject;

    protected final Project getAntProject()
    {
        if ( antProject == null )
        {
            // configure ant project
            antProject = new Project();
            antProject.setName( "NARProject" );
            antProject.addBuildListener( new NarLogger( getLog() ) );
        }
        return antProject;
    }

    protected final C getC()
    {
        if ( c == null )
        {
            c = new C();
        }
        c.setAbstractCompileMojo( this );
        return c;
    }

    protected final Cpp getCpp()
    {
        if ( cpp == null )
        {
            cpp = new Cpp();
        }
        cpp.setAbstractCompileMojo( this );
        return cpp;
    }

    protected final Fortran getFortran()
    {
        if ( fortran == null )
        {
            fortran = new Fortran();
        }
        fortran.setAbstractCompileMojo( this );
        return fortran;
    }

    protected final int getMaxCores( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        return getNarInfo().getProperty( aol, "maxCores", maxCores );
    }

    protected final boolean useLibtool( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        return getNarInfo().getProperty( aol, "libtool", libtool );
    }

    protected final boolean failOnError( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        return getNarInfo().getProperty( aol, "failOnError", failOnError );
    }

    protected final String getRuntime( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        return getNarInfo().getProperty( aol, "runtime", runtime );
    }

    protected final String getOutput( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        return getNarInfo().getProperty( aol, "output", output );
    }

    protected final List getTestExcludeDependencies()
            throws MojoExecutionException, MojoFailureException
    {
        if ( testExcludeDependencies == null )
        {
            testExcludeDependencies = Collections.EMPTY_LIST;
        }
        return testExcludeDependencies;
    }

    protected final File getJavaHome( AOL aol )
        throws MojoExecutionException, MojoFailureException
    {
        // FIXME should be easier by specifying default...
        return getNarInfo().getProperty( aol, "javaHome", NarUtil.getJavaHome( javaHome, getOS() ) );
    }

    protected final List getLibraries()
    {
        if ( libraries == null )
        {
            libraries = Collections.EMPTY_LIST;
        }
        return libraries;
    }

    protected final List getTests()
    {
        if ( tests == null )
        {
            tests = Collections.EMPTY_LIST;
        }
        return tests;
    }

    protected final Javah getJavah()
    {
        if ( javah == null )
        {
            javah = new Javah();
        }
        javah.setAbstractCompileMojo( this );
        return javah;
    }

    protected final Java getJava()
    {
        if ( java == null )
        {
            java = new Java();
        }
        java.setAbstractCompileMojo( this );
        return java;
    }

    public final void setDependencyLibOrder( List/* <String> */order )
    {
        dependencyLibOrder = order;
    }

    protected final List/* <String> */getDependencyLibOrder()
    {
        return dependencyLibOrder;
    }

    /**
     * Gets the NarInfo associated with this module.
     *
     * Looks for additional properties first in target/classes/META-INF/nar/<groupId>/<artifactId>
     * then in src/main/resources/META-INF/nar/<groupId>/<artifactId>
     * @return the loaded NarInfo
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    protected final NarInfo getNarInfo()
        throws MojoExecutionException, MojoFailureException
    {
        if ( narInfo == null )
        {
            String groupId = getMavenProject().getGroupId();
            String artifactId = getMavenProject().getArtifactId();

            File propertiesDir = getTargetPropertiesDir();
            File propertiesFile = getPropertiesFile( propertiesDir );
            if( !propertiesFile.exists() )
            {
                propertiesDir = new File( getMavenProject().getBasedir(), "src/main/resources/META-INF/nar/" + groupId + "/" + artifactId );
                propertiesFile = getPropertiesFile( propertiesDir );
            }

            narInfo = new NarInfo(
                groupId, artifactId,
                getMavenProject().getVersion(),
                getLog(),
                propertiesFile );

            narInfo.addLibrary(getAOL(), output);

            Set pchNames = new HashSet();
            //Add all the source files from pch libraries to a list
            for(Iterator libraryIterator = libraries.iterator(); libraryIterator.hasNext();)
                if(((Library)libraryIterator.next()).getType().equals(Library.PCH))
                {
                    //Only supported for C++
                    List sources = getSourcesFor(getCpp());
                    for(Iterator sourcesIterator = sources.iterator(); sourcesIterator.hasNext();)
                        pchNames.add(((File)sourcesIterator.next()).getName());
                }
            //add this info to the narInfo
            narInfo.setPchNames(getAOL(), pchNames);

            narInfo.setTargetWinRT(getAOL(), isTargetWinRT());
        }
        return narInfo;
    }

    private boolean isTargetWinRT()
    {
        //We really want a better way of doing this.
        //add a parameter?  we could set WinRT specific options automatically?
        Linker linker = getLinker();
        if(linker == null)
            return false;
        List options = linker.getOptions();
        if(options == null)
            return false;
        return options.contains(WINMD_FLAG);
    }

    private File getPropertiesFile(File propertiesDir) {
        File propertiesFile = new File( propertiesDir, NarInfo.NAR_PROPERTIES );
        return propertiesFile;
    }

    /**
     * Saves nar info to file a s a properties file (nar.properties)
     * under target/classes/META-INF/nar/<groupId>/<artifactId>
     * @param narInfo the info to write
     * @throws MojoExecutionException
     */
    protected void saveNarInfoToFile(NarInfo narInfo)
        throws MojoExecutionException
    {
        try
        {
            File propertiesDir = getTargetPropertiesDir();
            if ( !propertiesDir.exists() )
            {
                propertiesDir.mkdirs();
            }
            File propertiesFile = getPropertiesFile(propertiesDir);
            narInfo.writeToFile( propertiesFile );
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "Cannot write nar properties file", ioe );
        }
    }

    private File getTargetPropertiesDir()
    {
        File propertiesDir =
            new File( getOutputDirectory(), "classes/META-INF/nar/" + getMavenProject().getGroupId() + "/"
                + getMavenProject().getArtifactId() );
        return propertiesDir;
    }

    protected List getSourcesFor(Compiler compiler) throws MojoFailureException,
            MojoExecutionException
    {
        List srcDirs = compiler.getSourceDirectories();
        return getSourcesFromSourceDirectories(compiler, srcDirs);
    }

    private List getSourcesFromSourceDirectories(Compiler compiler, List srcDirs)
            throws MojoFailureException, MojoExecutionException {
        try
        {
            List files = new ArrayList();
            for ( Iterator i = srcDirs.iterator(); i.hasNext(); )
            {
                File dir = (File) i.next();
                if ( dir.exists() )
                {
                    files.addAll( FileUtils.getFiles( dir, StringUtils.join( compiler.getIncludes().iterator(), "," ),
                            null ) );
                }
            }
            return files;
        }
        catch ( IOException e )
        {
            return Collections.EMPTY_LIST;
        }
    }

    protected List getTestSourcesFor(Compiler compiler) throws MojoFailureException, MojoExecutionException
    {
        List srcDirs = compiler.getTestSourceDirectories();
        return getSourcesFromSourceDirectories(compiler, srcDirs);
    }

    protected void addPrecompiledHeaderOptions(Compiler cppCompiler, String scope)
    throws MojoExecutionException, MojoFailureException
    {
        for ( Iterator i = getNarManager().getNarDependencies( scope ).iterator(); i.hasNext(); )
        {
            NarArtifact narDependency = (NarArtifact) i.next();
            String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
            getLog().debug( "Looking for " + narDependency + " found binding " + binding);
            if (binding.equals(Library.PCH ) )
            {
                getLog().debug("Found pch dependency " + narDependency.getArtifactId());
                File unpackDirectory = getUnpackDirectory();
                File pchDir =
                    getLayout().getLibDirectory(unpackDirectory, narDependency.getArtifactId(),
                            narDependency.getVersion(), getAOL().toString(), binding);

                File[] pchFiles = pchDir.listFiles(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".pch");
                    }
                });
                for(int index = 0; index < pchFiles.length; index++)
                {
                    String pchName = pchFiles[index].getName().replace(".pch", ".h");
                    String absolutePchName = pchDir + File.separator +  pchName;
                    addCompileOption(cppCompiler, "/Yu" + absolutePchName);
                    addCompileOption(cppCompiler, "/FI" + absolutePchName); //force inclusion of pch file
                    if(debug)
                    {
                        pchName = pchName.replace(".h", ".pdb");
                        File pdbFile = new File(pchDir, pchName);
                        if(pdbFile.exists())
                            addCompileOption(cppCompiler, "/Fd" + pdbFile.getPath());
                    }
                }
            }
        }
    }

    protected void addCompileOption(Compiler compiler, String option)
    {
        getLog().debug("Added compile option " + option);
        compiler.addOption(option);
    }

    protected void addPchObjFiles(LinkerDef linkerDefinition, String binding,
            File libraryDirectory)
    {
        if(binding.equals(Library.PCH))
        {
            getLog().debug("adding precomiled header obj file");
            addObjFiles(linkerDefinition, libraryDirectory);
        }
    }

    protected void addObjFiles(LinkerDef linkerDefinition, File libraryDirectory)
    {
        File[] objFiles = libraryDirectory.listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(".obj");
            }
        });
        for(int index = 0; index < objFiles.length; index++)
        {
            getLog().debug("adding obj file" + objFiles[index]);
            LinkerArgument arg = new LinkerArgument();
            arg.setValue(objFiles[index].getPath());
            linkerDefinition.addConfiguredLinkerArg(arg);
        }
    }
}
