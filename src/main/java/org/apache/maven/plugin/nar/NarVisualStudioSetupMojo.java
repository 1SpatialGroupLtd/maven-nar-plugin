package org.apache.maven.plugin.nar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

        File solutionDirectory = getSolutionDirectory();
        String moduleName = getMavenProject().getName();
        mainProject = new VS2012Project(solutionDirectory, moduleName + "Project");
        testProject = new VS2012Project(solutionDirectory, moduleName + "TestProject");
        dependencyProject = new VS2012Project(solutionDirectory, moduleName + "DependencyProject");

        createSolution();

        initProjectInfos();
        mainProject.createProjectFiles(mainProjectInfo);
        testProject.createProjectFiles(testProjectInfo);
        dependencyProject.createProjectFiles(dependencyProjectInfo);
    }

    private void initProjectInfos() throws MojoExecutionException,
            MojoFailureException
    {
        mainProjectInfo = buildProjectInfo(VS2012_PROJECT_TEMPLATE, getBinding(), getDefines(), getDependencies(), getUnpackDirectory(), getCpp().getIncludePaths("main"), getSourcesFor(getCpp()));

        testProjectInfo = buildProjectInfo(VS2012_TEST_PROJECT_TEMPLATE, Library.EXECUTABLE, emptySet, getTestDependencies(), getTestUnpackDirectory(), getCpp().getIncludePaths("test"), getTestSourcesFor(getCpp()));;

        dependencyProjectInfo = new ProjectInfo(VS2012_TEST_PROJECT_TEMPLATE, Library.SHARED, emptySet, emptySet, emptySet, emptySet, getFilesByExtension(getDirectIncludes(), ".h"), emptySet);
    }

    private ProjectInfo buildProjectInfo(String template, String binding, Set defines, List dependencies, File unpackDirectory, List headerLocations, List sourceFiles) throws MojoExecutionException, MojoFailureException
    {
        Set libraryPaths = getLibraryPaths(dependencies, unpackDirectory);
        return new ProjectInfo(template,
                binding,
                defines,
                getIncludePaths(dependencies, unpackDirectory),
                libraryPaths,
                getLibraryFilePaths(libraryPaths),
                getHeaderFilePaths(headerLocations),
                getSourceFilePaths(sourceFiles));
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
        includes.add(getBasedir() + "\\src\\main\\include");
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

    private Set getDefines()
    {
        Set defines = new HashSet();
        defines.addAll(getCpp().getDefines());
        reportOnStringSet("Found defines:", defines);

        return defines;
    }

    private Set getLibraryPaths(List dependencies, File unpackDirectory) throws MojoFailureException,
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
        File libDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
                getMavenProject().getVersion(), getAOL().toString(), getBinding());
        //Add out own output directory
        libraryPaths.add(libDir.getPath());
        reportOnStringSet("Found library paths:", libraryPaths);
        return libraryPaths;
    }

    private Set getLibraryFilePaths(Set libraryPaths) throws MojoExecutionException, MojoFailureException
    {
        Set libraryFiles = new HashSet();
        libraryFiles.addAll(getFilesByExtension(libraryPaths, LIB_EXTENSION));
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
        File solution = new File(getSolutionDirectory(), getMavenProject().getName() + "Solution.sln");
        VisualStudioTemplateModifier modifier =
            new VisualStudioSolutionTemplateModifier("VS2012SolutionTemplate.txt",
                    solution, getGUID(), mainProject, testProject, dependencyProject);
        modifier.createPopulatedOutput();
    }

    private File getSolutionDirectory()
    {
        return new File(getBasedir(), getMavenProject().getName() + "Solution");
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
}
