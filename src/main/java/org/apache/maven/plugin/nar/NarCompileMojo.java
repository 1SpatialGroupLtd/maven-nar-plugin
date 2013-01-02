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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.antcontrib.cpptasks.CCTask;
import net.sf.antcontrib.cpptasks.CUtil;
import net.sf.antcontrib.cpptasks.CompilerDef;
import net.sf.antcontrib.cpptasks.LinkerDef;
import net.sf.antcontrib.cpptasks.OutputTypeEnum;
import net.sf.antcontrib.cpptasks.RuntimeType;
import net.sf.antcontrib.cpptasks.SubsystemEnum;
import net.sf.antcontrib.cpptasks.types.LibrarySet;
import net.sf.antcontrib.cpptasks.types.LinkerArgument;
import net.sf.antcontrib.cpptasks.types.SystemLibrarySet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Compiles native source files.
 *
 * @goal nar-compile
 * @phase compile
 * @requiresSession
 * @requiresProject
 * @requiresDependencyResolution compile
 * @author Mark Donszelmann
 */
public class NarCompileMojo
    extends AbstractCompileMojo
{
    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {

        // make sure destination is there
        getDestinationDirectory().mkdirs();

        // check for source files
        int noOfSources = 0;
        noOfSources += getSourcesFor(getCpp()).size();
        noOfSources += getSourcesFor(getC()).size();
        noOfSources += getSourcesFor(getFortran()).size();
        if ( noOfSources > 0 )
        {
            getLog().info(getSourcesMessage(noOfSources));
            for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
            {
                createLibrary(getAntProject(), (Library) i.next());
            }
        }
        else
        {
            getLog().info(getSourcesMessage(noOfSources));
        }

        try
        {
            //if we only contain pch libraries we don't want to copy the headers
            boolean copyHeaders = false;
            for ( Iterator i = getLibraries().iterator(); i.hasNext(); )
                if(!((Library) i.next()).getType().equals(Library.PCH))
                {
                    copyHeaders = true;
                    break;
                }

            // FIXME, should the include paths be defined at a higher level ?
            if(copyHeaders)
            {
                getCpp().copyIncludeFiles(
                                       getMavenProject(),
                                       getLayout().getIncludeDirectory( getDestinationDirectory(),
                                                                        getMavenProject().getArtifactId(),
                                                                        getMavenProject().getVersion() ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "NAR: could not copy include files", e );
        }
    }

    private void createLibrary(Project antProject, Library library)
        throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Creating Library " + library );
        // configure task
        CCTask task = new CCTask();
        task.setProject(antProject);

        // subsystem
        SubsystemEnum subSystem = new SubsystemEnum();
        subSystem.setValue( library.getSubSystem() );
        task.setSubsystem( subSystem );

        // set max cores
        task.setMaxCores(getMaxCores(getAOL()));

        // outtype
        String type = library.getType();

        if(!type.equals(Library.PCH)) //we only need this if we are linking
        {
            OutputTypeEnum outTypeEnum = new OutputTypeEnum();
            outTypeEnum.setValue(type);
            task.setOuttype(outTypeEnum);
        }

        setLanguageLinkers(task, library);

        // outDir
        File outDir;
        if ( type.equals( Library.EXECUTABLE ) )
        {
            outDir =
                getLayout().getBinDirectory( getDestinationDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion(), getAOL().toString() );
        }
        else
        {
            outDir =
                getLayout().getLibDirectory( getDestinationDirectory(), getMavenProject().getArtifactId(),
                                             getMavenProject().getVersion(), getAOL().toString(), type );
        }
        outDir.mkdirs();

        // outFile
        // FIXME NAR-90 we could get the final name from layout
        File outFile;
        if ( type.equals( Library.EXECUTABLE ) )
        {
            // executable has no version number
            outFile = new File(outDir, getMavenProject().getArtifactId());
        }
        else if(type.equals(Library.PCH))
        {
            outFile = null; //This stops us linking
        }
        else
        {
            outFile = new File(outDir, getOutput(getAOL()));
        }
        if(outFile != null)
            getLog().debug("NAR - output: '" + outFile + "'");
        else
            getLog().debug("NAR - no output; not linking");

        task.setOutfile(outFile);

        // object directory
        if(type.equals(Library.PCH))
        {
            task.setObjdir(outDir); //we want the pch as the output
        }
        else
        {
            File objDir = new File(getDestinationDirectory(), getObjectDirectoryName());
            objDir = new File(objDir, getAOL().toString());
            objDir.mkdirs();
            task.setObjdir(objDir);
        }

        // failOnError, libtool
        task.setFailonerror(failOnError(getAOL()));
        task.setLibtool(useLibtool(getAOL()));

        // runtime
        RuntimeType runtimeType = new RuntimeType();
        runtimeType.setValue(getRuntime(getAOL()));
        task.setRuntime(runtimeType);

        setCompilerOptions(task, type);

        // add dependency include paths
        for ( Iterator i = getNarManager().getNarDependencies(getScope()).iterator(); i.hasNext(); )
        {
            // FIXME, handle multiple includes from one NAR
            NarArtifact narDependency = (NarArtifact) i.next();

            if (!excludeDependency(narDependency))
            {
                String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
                getLog().debug( "Looking for " + narDependency + " found binding " + binding);
                if ( !binding.equals(Library.JNI ) && !binding.equals(Library.PCH) )
                {
                    //File unpackDirectory = getUnpackDirectory();
                    File include =
                        getLayout().getIncludeDirectory( getDestinationUnpackDirectory(), narDependency.getArtifactId(),
                                                         narDependency.getVersion() );

                    getLog().debug( "Looking for include directory: " + include );
                    if ( include.exists() )
                    {
                        task.createIncludePath().setPath(include.getPath());
                    }
                    else
                    {
                        throw new MojoExecutionException(
                            "NAR: unable to locate include path: " + include);
                    }
                }
            }
        }

        // add linker
        LinkerDef linkerDefinition =
            getLinker().getLinker( this, antProject, getOS(), getAOL().getKey() + ".linker.", type );
        task.addConfiguredLinker(linkerDefinition);

        // add dependency libraries
        // FIXME: what about PLUGIN and STATIC, depending on STATIC, should we
        // not add all libraries, see NARPLUGIN-96
        if ( type.equals( Library.SHARED ) || type.equals( Library.JNI ) || type.equals( Library.EXECUTABLE ) )
        {

            List depLibOrder = getDependencyLibOrder();
            List depLibs = getNarManager().getNarDependencies(getScope());

            // reorder the libraries that come from the nar dependencies
            // to comply with the order specified by the user
            if ( ( depLibOrder != null ) && !depLibOrder.isEmpty() )
            {
                List tmp = new LinkedList();

                for ( Iterator i = depLibOrder.iterator(); i.hasNext(); )
                {
                    String depToOrderName = (String) i.next();

                    for ( Iterator j = depLibs.iterator(); j.hasNext(); )
                    {
                        NarArtifact dep = (NarArtifact) j.next();
                        String depName = dep.getGroupId() + ":" + dep.getArtifactId();

                        if (depName.equals(depToOrderName))
                        {
                            tmp.add(dep);
                            j.remove();
                        }
                    }
                }

                tmp.addAll(depLibs);
                depLibs = tmp;
            }

            for ( Iterator i = depLibs.iterator(); i.hasNext(); )
            {
                NarArtifact dependency = (NarArtifact) i.next();

                if (!excludeDependency(dependency))
                {
                    // FIXME no handling of "local"

                    // FIXME, no way to override this at this stage
                    String binding = dependency.getNarInfo().getBinding( getAOL(), Library.NONE );
                    getLog().debug("Using Binding: " + binding);
                    AOL aol = getAOL();
                    aol = dependency.getNarInfo().getAOL(getAOL());
                    getLog().debug("Using Library AOL: " + aol.toString());

                    if ( !binding.equals( Library.JNI ) && !binding.equals( Library.NONE ) && !binding.equals( Library.EXECUTABLE) )
                    {
                        //File unpackDirectory = getUnpackDirectory();

                        File dir =
                        getLayout().getLibDirectory( getDestinationUnpackDirectory(), dependency.getArtifactId(),
                                                     dependency.getVersion(), aol.toString(), binding );

                        getLog().debug("Looking for Library Directory: " + dir);
                        if ( dir.exists() )
                        {
                            LibrarySet libSet = new LibrarySet();
                            libSet.setProject(antProject);

                            // FIXME, no way to override
                            String libs = dependency.getNarInfo().getLibs(getAOL());
                            if ( ( libs != null ) && !libs.equals( "" ) )
                            {
                                getLog().debug("Using LIBS = " + libs);
                                libSet.setLibs(new CUtil.StringArrayBuilder(libs));
                                libSet.setDir(dir);
                                task.addLibset(libSet);
                            }
                        }
                        else
                        {
                            getLog().debug( "Library Directory " + dir + " does NOT exist." );
                        }

                        // FIXME, look again at this, for multiple dependencies we may need to remove duplicates
                        String options = dependency.getNarInfo().getOptions( getAOL() );
                        if ( ( options != null ) && !options.equals( "" ) )
                        {
                            getLog().debug("Using OPTIONS = " + options);
                            LinkerArgument arg = new LinkerArgument();
                            arg.setValue(options);
                            linkerDefinition.addConfiguredLinkerArg(arg);
                        }

                        String sysLibs = dependency.getNarInfo().getSysLibs( getAOL() );
                        if ( ( sysLibs != null ) && !sysLibs.equals( "" ) )
                        {
                            getLog().debug("Using SYSLIBS = " + sysLibs);
                            SystemLibrarySet sysLibSet = new SystemLibrarySet();
                            sysLibSet.setProject(antProject);

                            sysLibSet.setLibs( new CUtil.StringArrayBuilder( sysLibs ) );
                            task.addSyslibset(sysLibSet);
                        }

                        //Add obj files for pre compiled headers to the linker
                        addPchObjFiles(linkerDefinition, binding, dir);
                    }
                }
            }
        }

        addObjectFilesToLinker(linkerDefinition);

        // Add JVM to linker
        getJava().addRuntime( task, getJavaHome( getAOL() ), getOS(), getAOL().getKey() + ".java." );

        // execute
        try
        {
            task.execute();
        }
        catch ( BuildException e )
        {
            throw new MojoExecutionException("NAR: Compile failed", e);
        }

        // FIXME, this should be done in CPPTasks at some point
        if ( getRuntime( getAOL() ).equals( "dynamic" ) && getOS().equals( OS.WINDOWS )
            && getLinker().getName( null, null ).equals( "msvc" ) && !getLinker().getVersion().startsWith( "6." ) )
        {
            String libType = library.getType();
            if ( libType.equals( Library.JNI ) || libType.equals( Library.SHARED ) )
            {
                String dll = outFile.getPath() + ".dll";
                String manifest = dll + ".manifest";
                int result =
                    NarUtil.runCommand( "mt.exe", new String[] { "/manifest", manifest,
                        "/outputresource:" + dll + ";#2" }, null, null, getLog() );
                if (result != 0)
                {
                    throw new MojoFailureException("MT.EXE failed with exit code: " + result);
                }
            } else if (libType.equals(Library.EXECUTABLE)) {
                String exe = outFile.getPath() + ".exe";
                String manifest = exe + ".manifest";
                int result = NarUtil.runCommand("mt.exe",
                        new String[] { "/manifest", manifest,
                                "/outputresource:" + exe + ";#1" }, null, null, getLog());
                if (result != 0)
                    throw new MojoFailureException(
                            "MT.EXE failed with exit code: " + result);
            }
        }
    }

    protected File getDestinationDirectory()
    {
        return getTargetDirectory();
    }

    protected String getSourcesMessage(int noOfSources)
    {
        if (noOfSources > 0)
        {
            return "Compiling " + noOfSources + " native files";
        }
        else
        {
            return "Nothing to compile";
        }
    }

    protected void setLanguageLinkers(CCTask task, Library library)
    {
        // stdc++
        task.setLinkCPP(library.linkCPP());

        // fortran
        task.setLinkFortran(library.linkFortran());
        task.setLinkFortranMain( library.linkFortranMain() );
    }

    protected String getObjectDirectoryName()
    {
        return "obj";
    }

    protected void setCompilerOptions(CCTask task, String type) throws MojoFailureException, MojoExecutionException
    {
        Compiler cppCompiler = getCpp();
        //Add options for compiling against a winmd file
        getLog().info("Looking for WinRT dependencies");
        for (Iterator i = getNarManager().getNarDependencies(Artifact.SCOPE_COMPILE).iterator(); i.hasNext();)
        {
            NarArtifact dependency = (NarArtifact)i.next();
            if(dependency.getNarInfo().isTargetWinRT(getAOL()))
            {
                getLog().debug("Found WinRT dependency " + dependency.getArtifactId());
                String binding = dependency.getNarInfo().getBinding(getAOL(), "static");
                if(!binding.equals("shared"))
                {
                    getLog().debug("Binding is not shared, ignoring");
                    continue;
                }
                File libDir = getLayout().getLibDirectory(getUnpackDirectory(),
                        dependency.getArtifactId(), dependency.getVersion(),
                        getAOL().toString(), binding);
                File[] winmdFiles = libDir.listFiles(new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".winmd");
                    }
                });
                for(int index = 0; index < winmdFiles.length; index++)
                {
                    addCompileOption(cppCompiler, "/FU"+winmdFiles[index].getPath());
                }
            }
        }

        //Add precompiled header options
        addPrecompiledHeaderOptions(cppCompiler, "compile");

        // Darren Sargent Feb 11 2010: Use Compiler.MAIN for "type"...appears the wrong "type" variable was being used
        // since getCompiler() expects "main" or "test", whereas the "type" variable here is "executable", "shared" etc.
        // add C++ compiler
        CompilerDef cpp = getCpp().getCompiler( Compiler.MAIN, getOutput( getAOL() ) );
        if ( cpp != null )
        {
            task.addConfiguredCompiler( cpp );
        }

        // add C compiler
        CompilerDef c = getC().getCompiler( Compiler.MAIN, getOutput( getAOL() ) );
        if ( c != null )
        {
            task.addConfiguredCompiler( c );
        }

        // add Fortran compiler
        CompilerDef fortran = getFortran().getCompiler( Compiler.MAIN, getOutput( getAOL() ) );
        if ( fortran != null )
        {
            task.addConfiguredCompiler( fortran );
        }
        // end Darren

        // add javah include path
        File jniDirectory = getJavah().getJniDirectory();
        if (jniDirectory.exists())
        {
            task.createIncludePath().setPath(jniDirectory.getPath());
        }

        // add java include paths
        getJava().addIncludePaths(task, type);

    }

    protected String getScope()
    {
        return "compile";
    }

    protected File getDestinationUnpackDirectory()
    {
        return getUnpackDirectory();
    }

    protected boolean excludeDependency(NarArtifact narDependency) throws MojoExecutionException, MojoFailureException
    {
        for(Iterator i = getCompileExcludeDependencies().iterator(); i.hasNext();)
        {
            String exclude = (String) i.next();
            if(narDependency.getArtifactId().equals(exclude))
            {
                getLog().info( "Excluding dependency " + exclude );
                return true;
            }
        }
        return false;
    }

    protected void addObjectFilesToLinker(LinkerDef linkerDefinition) throws MojoFailureException, MojoExecutionException
    {
        // do nothing
    }
}
