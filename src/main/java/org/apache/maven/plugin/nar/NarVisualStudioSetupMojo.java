package org.apache.maven.plugin.nar;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.surefire.shade.org.codehaus.plexus.util.FileUtils;

/**
 * Sets up a Visual Studio 2012 solution for a module
 *
 * @goal nar-visual-studio-setup
 * @phase compile
 * @requiresProject
 * Use test dependency resolution as this will also include compile dependencies
 * this mean we can compile tests in VS too.
 * @requiresDependencyResolution test
 * @author Mike Boyd
 */
public class NarVisualStudioSetupMojo extends AbstractCompileMojo {

    private static final String VS2012_TEST_PROJECT_TEMPLATE = "VS2012TestProjectTemplate.txt";

    private static final String VS2012_PROJECT_TEMPLATE = "VS2012ProjectTemplate.txt";

    private static final String LIB_EXTENSION = ".lib";

    private static final String EXTERNAL_LIBS_FOLDER = "external-libs";

    private VS2012Project mainProject;

    private VS2012Project testProject;

    private VS2012Project dependencyProject;

    private ProjectInfo mainProjectInfo;

    private ProjectInfo testProjectInfo;

    private ProjectInfo dependencyProjectInfo;

    private Set emptySet = new HashSet();

    public final void narExecute() throws MojoFailureException,
            MojoExecutionException
    {
        checkPermissions(getBasedir());

        File solutionDirectory = createSolutionDirectory();
        String moduleName = getMavenProject().getName().replace(' ', '_');
        mainProject = new VS2012Project(solutionDirectory, moduleName + "_Project");
        testProject = new VS2012Project(solutionDirectory, moduleName + "_TestProject");
        dependencyProject = new VS2012Project(solutionDirectory, moduleName + "_DependencyProject");

        createSolution();

        initProjectInfos();
        mainProject.createProjectFiles(mainProjectInfo);
        testProject.createProjectFiles(testProjectInfo);
        dependencyProject.createProjectFiles(dependencyProjectInfo);
    }

    private void initProjectInfos() throws MojoExecutionException,
            MojoFailureException
    {
        Set defines = getDefines();
        mainProjectInfo = buildProjectInfo(VS2012_PROJECT_TEMPLATE, getBinding(), defines, getDependencies(), getUnpackDirectory(), getCpp().getIncludePaths("main"), getSourcesFor(getCpp()), false);

        testProjectInfo = buildProjectInfo(VS2012_TEST_PROJECT_TEMPLATE, Library.EXECUTABLE, defines, getTestDependencies(), getTestUnpackDirectory(), getCpp().getIncludePaths("test"), getTestSourcesFor(getCpp()), true);

        dependencyProjectInfo = new ProjectInfo(VS2012_TEST_PROJECT_TEMPLATE, Library.SHARED, emptySet, emptySet, emptySet, emptySet, getFilesByExtension(getDirectIncludes(), ".h"), emptySet, new PchInfo(), getRuntime());
    }

    private ProjectInfo buildProjectInfo(String template, String binding, Set defines, List dependencies, File unpackDirectory, List headerLocations, List sourceFiles, boolean testBuild) throws MojoExecutionException, MojoFailureException
    {
        Set libraryPaths = getLibraryPaths(dependencies, unpackDirectory, testBuild);
        return new ProjectInfo(template,
                binding,
                defines,
                getIncludePaths(dependencies, unpackDirectory),
                libraryPaths,
                getLibraryFilePaths(libraryPaths, testBuild),
                getHeaderFilePaths(headerLocations),
                getSourceFilePaths(sourceFiles),
                getPchInfo(),
                getRuntime());
    }

    private PchInfo getPchInfo() throws MojoExecutionException, MojoFailureException
    {
        List dependencies = getNarManager().getNarDependencies(Artifact.SCOPE_COMPILE);
        PchInfo retVal = new PchInfo();
        for(Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact dependency = (NarArtifact) i.next();
            NarInfo narInfo = dependency.getNarInfo();
            String binding = narInfo.getBinding(getAOL(), "");
            //I believe there should only be one pch.
            //Certainly VS2012 only supports one so just use the first
            if(binding.equals(Library.PCH))
            {
                File pchDirectory =  getLayout().getLibDirectory(getUnpackDirectory(), dependency.getArtifactId(),
                        dependency.getVersion(), getAOL().toString(), binding);
                pchDirectory = pchDirectory.getParentFile(); //This takes us above the debug/release split, we will need both
                getLog().debug("Found pre compiled header in " + pchDirectory);
                retVal.directory = pchDirectory;
                retVal.usePch = true;
                retVal.artifactId = dependency.getArtifactId();
                String pchNames = narInfo.getPchNames(getAOL());
                //VS2012 only supports a single pre compiled header.
                if(pchNames.contains(","))
                    retVal.pchName = pchNames.substring(0, pchNames.indexOf(","));
                else
                    retVal.pchName = pchNames;
            }
        }
        return retVal;
    }

    private List getDependencies() throws MojoExecutionException, MojoFailureException
    {
        List dependencies = getNarManager().getNarDependencies(Artifact.SCOPE_COMPILE);
        reportOnDependencies("Found dependencies:", dependencies);
        return dependencies;
    }

    private List getTestDependencies() throws MojoExecutionException, MojoFailureException
    {
        //Use the test scope as this will include both test and compile dependencies
        List dependencies = getNarManager().getNarDependencies(Artifact.SCOPE_TEST);
        
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            NarArtifact narDependency = (NarArtifact) i.next();
            if (getTestExcludeDependencies().contains(narDependency.getArtifactId()))
            {
                getLog().debug("Excluding dependency: " + narDependency.getArtifactId());
                dependencies.remove(narDependency);
            }
        }
        reportOnDependencies("Found test dependencies:", dependencies);
        return dependencies;
    }

    private void reportOnDependencies(String message, List dependencies)
    {
        getLog().debug(message);
        for(Iterator i = dependencies.iterator(); i.hasNext();)
        {
            getLog().debug(((NarArtifact) i.next()).getArtifactId());
        }
    }

    private String getRuntime() throws MojoExecutionException, MojoFailureException
    {
        String runtime = getRuntime(getAOL());
        getLog().debug("Runtime: " + runtime);
        return runtime;
    }

    private String getBinding() throws MojoExecutionException, MojoFailureException
    {
        String binding = getNarInfo().getBinding(getAOL(), Library.STATIC);
        getLog().debug("Artifact binding: " + binding);

        return binding;
    }

    private Set getIncludePaths(List dependencies, File unpackDirectory) throws MojoExecutionException, MojoFailureException
    {
        Set includes = getIncludesFromDependencies(dependencies, unpackDirectory);
        //Add our own include locations
        includes.addAll(getCpp().getSystemIncludePaths());
        includes.add(getBasedir() + "\\src\\main\\include");
        // It is OK to add test includes even for non-test projects as files will only be included if referenced by #include or /FI
        // Files in test/include should NOT share names with files in main/include, otherwise the compiler will not know which one to reference
        includes.add(getBasedir() + "\\src\\test\\include");
        reportOnStringSet("Found include locations:", includes);
        return includes;
    }

    private Set getDirectIncludes() throws MojoExecutionException, MojoFailureException
    {
        Set absoluteIncludes = getModuleIncludes();
        absoluteIncludes.addAll(getDirectDependencyIncludes());
        reportOnStringSet("Found direct include locations:", absoluteIncludes);

        return absoluteIncludes;
    }

    private Set getModuleIncludes()
    {
        Set absoluteIncludes = new HashSet();
        //we only care about the c++ includes
        //Do we want test includes here as well?
        absoluteIncludes.addAll(getCpp().getIncludePaths("main"));
        return absoluteIncludes;
    }

    private Set getIncludesFromDependencies(List dependencies, File unpackDirectory)
            throws MojoExecutionException, MojoFailureException
    {
        Set dependencyIncludes = new HashSet();
        for(Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact narDependency = (NarArtifact) i.next();
            String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
            if (!binding.equals(Library.JNI))
            {
                File include =
                    getLayout().getIncludeDirectory(unpackDirectory, narDependency.getArtifactId(),
                                                     narDependency.getVersion());
                dependencyIncludes.add(include.getPath());
            }
        }
        return dependencyIncludes;
    }

    private Set getDirectDependencyIncludes() throws MojoExecutionException, MojoFailureException
    {
        List dependencies = getNarManager().getDirectNarDependencies(Artifact.SCOPE_COMPILE);
        return getIncludesFromDependencies(dependencies, getUnpackDirectory());
    }

    private Set getDefines() throws MojoFailureException, MojoExecutionException
    {
        Set defines = new HashSet();
        defines.addAll(getCpp().getDefines());
        reportOnStringSet("Found defines:", defines);

        return defines;
    }

    private Set getLibraryPaths(List dependencies, File unpackDirectory, boolean testBuild) throws MojoFailureException,
            MojoExecutionException
    {
        Set libraryPaths = new HashSet();
        for(Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact narDependency = (NarArtifact) i.next();
            String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
            if(!binding.equals(Library.JNI ))
            {
                File libraryPath =
                    getLayout().getLibDirectory(unpackDirectory, narDependency.getArtifactId(),
                            narDependency.getVersion(), getAOL().toString(), binding);
                libraryPaths.add(libraryPath.getPath());
            }
        }
        //Add our own output directories
        
        // Add external-libs folder
        libraryPaths.add(getBasedir() + "\\" + EXTERNAL_LIBS_FOLDER);
        
        // Add main project lib for test project to link against
        if (testBuild)
        {
            libraryPaths.add(getVisualStudioMainProjectDir());
        }
        reportOnStringSet("Found library paths:", libraryPaths);
        return libraryPaths;
    }

    private Set getLibraryFilePaths(Set libraryPaths, boolean testBuild) throws MojoExecutionException, MojoFailureException
    {
        Set libraryFiles = new HashSet();
        libraryFiles.addAll(getFilesByExtension(libraryPaths, LIB_EXTENSION));
        if (testBuild)
        {
            libraryFiles.add(getVisualStudioMainProjectLib());
        }
        reportOnStringSet("Found libraries:", libraryFiles);
        return libraryFiles;
    }

    private void reportOnStringSet(String text, Collection data) {
        getLog().debug(text);
        for ( Iterator i = data.iterator(); i.hasNext(); )
        {
            getLog().debug((String) i.next());
        }
    }

    private Set getHeaderFilePaths(List includePaths) throws MojoExecutionException
    {
        Set headerFiles = new HashSet();
        for(Iterator it = includePaths.iterator(); it.hasNext();)
        {
            File[] includeFiles = new File((String)it.next()).listFiles();
            for(int i = 0; i < includeFiles.length; i++)
                headerFiles.add(includeFiles[i].getPath());
        }
        reportOnStringSet("Found header files:", headerFiles);
        return headerFiles;
    }

    private Set getSourceFilePaths(List sourceFiles) throws MojoExecutionException, MojoFailureException
    {
        Set sourceFilePaths = getFilePaths(sourceFiles);
        reportOnStringSet("Found source files:", sourceFilePaths);
        return sourceFilePaths;
    }

    private Set getFilePaths(List files)
    {
        Set filePaths = new HashSet();
        for(Iterator i = files.iterator(); i.hasNext();)
            filePaths.add(((File)i.next()).getPath());
        return filePaths;
    }

    private String getGUID()
    {
        return UUID.randomUUID().toString();
    }

    private void createSolution() throws MojoExecutionException
    {
        File solution = new File(getSolutionDirectory(), getMavenProject().getName().replace(' ', '_') + "_Solution.sln");
        VisualStudioTemplateModifier modifier =
            new VisualStudioSolutionTemplateModifier("VS2012SolutionTemplate.txt",
                    solution, getGUID(), mainProject, testProject, dependencyProject);
        modifier.createPopulatedOutput();
    }

    private File createSolutionDirectory() throws MojoExecutionException
    {
        File solutionDirectory = getSolutionDirectory();
        if (solutionDirectory.exists())
        {
            try
            {
                // Deletes directory to prevent problems with previous VS builds.
                FileUtils.deleteDirectory(solutionDirectory);
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(e.getStackTrace().toString());
            }
        }
        return solutionDirectory;
    }

    private File getSolutionDirectory()
    {
        return new File(getBasedir(), getMavenProject().getName().replace(' ', '_') + "_Solution");
    }

    private void checkPermissions(File directory)
        throws MojoExecutionException
    {
        if(!directory.canRead())
        {
            getLog().debug("Cannot read " + directory);
            throw new MojoExecutionException("Can't read file: " + directory);
        }
        if(!directory.canWrite())
        {
            getLog().debug("Cannot write to " + directory);
            throw new MojoExecutionException("Can't write to file: " + directory.getPath());
        }
    }

    private Set getFilesByExtension(Set locations, String extension)
    {
        Set libraries = new HashSet();
        for(Iterator i = locations.iterator(); i.hasNext();)
        {
            File directory = new File((String) i.next());
            File[] files =  directory.listFiles(new ExtensionFilter(extension));

            if(files != null)
                for(int index = 0; index < files.length; index++)
                {
                    libraries.add(files[index].getPath());
                }
        }
        return libraries;
    }

    private class ExtensionFilter implements FilenameFilter
    {
        private String extension;

        ExtensionFilter(String extension)
        {
            this.extension = extension;
        }

        public boolean accept(File dir, String name)
        {
            return name.endsWith(extension);
        }
    }

    public class PchInfo
    {
        public String artifactId;
        public File directory;
        public boolean usePch = false;
        public String pchName;
    }

    private String getVisualStudioMainProjectDir()
    {
        String folder = debug ? "Debug" : "Release";
        String mainProjectLib = getSolutionDirectory().getPath() + "\\" + folder;
        return mainProjectLib;
    }

    private String getVisualStudioMainProjectLib()
    {
        String moduleLibName = getMavenProject().getName().replace(' ', '_') + "_Project.lib";
        return getVisualStudioMainProjectDir() + "\\" + moduleLibName;
    }
}
