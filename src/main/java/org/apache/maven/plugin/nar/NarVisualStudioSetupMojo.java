package org.apache.maven.plugin.nar;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

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

	/**
	 * The directory the solution resides in
	 */
	private File solutionDir;

	/**
	 * The directory the main project resides in
	 */
	private File mainProjectDir;

	/**
	 * The directory the test project resides in
	 */
	private File testProjectDir;

	private String solutionGUID;

	private String mainProjectGUID;

	private String testProjectGUID;

	private String binding;

	private Set includes;

	private Set libraryPaths;

	private Set defines;

	private Set libraryFiles;

	private Set headerFiles;

	private Set sourceFiles;

	private List testDependencies;

	private Set testHeaderFiles;

	private Set testSourceFiles;

	private Set testLibraryPaths;

	private Set testLibraryFiles;

	public NarVisualStudioSetupMojo()
	{
		generateGUIDS();
	}

	public final void narExecute() throws MojoFailureException,
			MojoExecutionException
	{
		checkPermissions(getBasedir());
		createSolutionFiolders();

		createSolution();

		createMainProjectFile();
		createMainFiltersFile();
		createMainUserFile();

		createTestProjectFile();
		createTestFiltersFile();
		createTestUserFile();
	}

	private void createMainUserFile() throws MojoExecutionException, MojoFailureException
	{
		File user = new File(getProjectDirectory(), getProjectName() + ".vcxproj.user");
		VisualStudioTemplateModifier modifier =
			new VisualStudioUsersTemplateModifier("VS2012UserTemplate.txt", user,
					getLibraryPaths());
		modifier.createPopulatedOutput();
	}

	private void createTestUserFile() throws MojoExecutionException, MojoFailureException
	{
		File user = new File(getTestProjectDirectory(), getTestProjectName() + ".vcxproj.user");
		VisualStudioTemplateModifier modifier =
			new VisualStudioUsersTemplateModifier("VS2012UserTemplate.txt", user,
					getTestLibraryPaths());
		modifier.createPopulatedOutput();
	}

	private void createMainFiltersFile() throws MojoExecutionException, MojoFailureException
	{
		File filters = new File(getProjectDirectory(), getProjectName() + ".vcxproj.filters");
		VisualStudioTemplateModifier modifier =
			new VisualStudioFiltersTemplateModifier("VS2012FiltersTemplate.txt", filters,
					getHeaderFiles(), getSourceFiles());
		modifier.createPopulatedOutput();
	}

	private void createTestFiltersFile() throws MojoExecutionException, MojoFailureException
	{
		File filters = new File(getTestProjectDirectory(), getTestProjectName() + ".vcxproj.filters");
		VisualStudioTemplateModifier modifier =
			new VisualStudioFiltersTemplateModifier("VS2012FiltersTemplate.txt", filters,
					getTestHeaderFiles(), getTestSourceFiles());
		modifier.createPopulatedOutput();
	}

	private List getTestDependencies() throws MojoExecutionException, MojoFailureException
	{
		if(testDependencies == null)
		{
			//Use the test scope as this will include both test and compile dependencies
			testDependencies = getNarManager().getNarDependencies("test");

			getLog().debug("Found test dependencies:");
			for(Iterator i = testDependencies.iterator(); i.hasNext();)
			{
				getLog().debug(((NarArtifact) i.next()).getArtifactId());
			}
		}
		return testDependencies;
	}

	private String getBinding() throws MojoExecutionException, MojoFailureException
	{
		if(binding == null)
		{
			binding = getNarInfo().getBinding(getAOL(), Library.STATIC);
			getLog().debug("Artifact binding: " + binding);
		}
		return binding;
	}

	private Set getIncludes() throws MojoExecutionException, MojoFailureException
	{
		if(includes == null)
		{
			includes = new HashSet();
			//we only care about the c++ includes
			//Do we want test includes here as well?
			includes.addAll(getCpp().getIncludePaths("main"));
			includes.addAll(getDependencyIncludes());
			includes = getRelativePaths(getProjectDirectory(), includes);
			reportOnStringSet("Found include locations:", includes);
		}
		return includes;
	}

	private Set getDependencyIncludes() throws MojoFailureException,
			MojoExecutionException
	{
		Set dependencyIncludes = new HashSet();
		for(Iterator i = getTestDependencies().iterator(); i.hasNext();)
        {
            NarArtifact narDependency = (NarArtifact) i.next();
            String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
            if (!binding.equals(Library.JNI))
            {
                File include =
                    getLayout().getIncludeDirectory(getTestUnpackDirectory(), narDependency.getArtifactId(),
                                                     narDependency.getVersion());
                dependencyIncludes.add(include.getPath());
            }
        }
		return dependencyIncludes;
	}

	private Set getDefines()
	{
		if(defines == null)
		{
			defines = new HashSet();
			defines.addAll(getCpp().getDefines());
			reportOnStringSet("Found defines:", defines);
		}
		return defines;
	}

	private Set getLibraryPaths() throws MojoExecutionException, MojoFailureException
	{
		if(libraryPaths == null)
		{
			libraryPaths = new HashSet();
			//Use the test scope as this will include both test and compile dependencies
			for(Iterator i = getTestDependencies().iterator(); i.hasNext();)
			{
				NarArtifact narDependency = (NarArtifact) i.next();
				String binding = narDependency.getNarInfo().getBinding(getAOL(), Library.STATIC);
				if(!binding.equals(Library.JNI ))
				{
					File libraryPath =
						getLayout().getLibDirectory(getTestUnpackDirectory(), narDependency.getArtifactId(),
                                                     narDependency.getVersion(), getAOL().toString(), binding);
					libraryPaths.add(libraryPath.getPath());
				}
			}
			libraryPaths = getRelativePaths(getProjectDirectory(), libraryPaths);
			reportOnStringSet("Found library locations:", libraryPaths);
		}
		return libraryPaths;
	}

	private Set getTestLibraryPaths() throws MojoExecutionException, MojoFailureException
	{
		if(testLibraryPaths == null)
		{
			testLibraryPaths = new HashSet();
			//Do we want to exclude any other bindings?
			if(!getBinding().equals(Library.EXECUTABLE))
			{
				testLibraryPaths.addAll(getLibraryPaths());
				File libDir = getLayout().getLibDirectory(getTargetDirectory(), getMavenProject().getArtifactId(),
						getMavenProject().getVersion(), getAOL().toString(), getBinding());
				testLibraryPaths.add(getRelativePath(getTestProjectDirectory(), libDir.getPath()));
			}
			reportOnStringSet("Found test library locations:", testLibraryPaths);
		}
		return testLibraryPaths;
	}

	private Set getLibraryFiles() throws MojoExecutionException, MojoFailureException
	{
		if(libraryFiles == null)
		{
			libraryFiles = new HashSet();
			libraryFiles.addAll(getLibraries(getLibraryPaths(), getProjectDirectory()));
			reportOnStringSet("Found libraries:", libraryFiles);
		}
		return libraryFiles;
	}

	private Set getTestLibraryFiles() throws MojoExecutionException, MojoFailureException
	{
		if(testLibraryFiles == null)
		{
			testLibraryFiles = new HashSet();
			testLibraryFiles.addAll(getLibraries(getTestLibraryPaths(), getTestProjectDirectory()));
			reportOnStringSet("Found test libraries:", testLibraryFiles);
		}
		return testLibraryFiles;
	}

	private void reportOnStringSet(String text, Collection data) {
		getLog().debug(text);
		for ( Iterator i = data.iterator(); i.hasNext(); )
		{
            getLog().debug((String) i.next());
		}
	}

	private Set getHeaderFiles() throws MojoExecutionException, MojoFailureException
	{
		if(headerFiles == null)
		{
			headerFiles = getHeaderFilesFromLocations(getProjectDirectory(), getCpp().getIncludePaths("main"));
			reportOnStringSet("Found header files:", headerFiles);
		}
		return headerFiles;
	}

	private Set getHeaderFilesFromLocations(File baseDir, List includePaths) throws MojoExecutionException
	{
		Set absoluteHeaderFiles = new HashSet();
		for(Iterator it = includePaths.iterator(); it.hasNext();)
		{
			File[] includeFiles = new File((String)it.next()).listFiles();
			for(int i = 0; i < includeFiles.length; i++)
				absoluteHeaderFiles.add(includeFiles[i].getPath());
		}
		return getRelativePaths(baseDir, absoluteHeaderFiles);
	}

	private Set getSourceFiles() throws MojoExecutionException, MojoFailureException
	{
		if(sourceFiles == null)
		{
			Set absoluteSourceFiles = new HashSet(getSourcesFor(getCpp()));
			sourceFiles = getRelativePathsFromFiles(getProjectDirectory(), absoluteSourceFiles);
			reportOnStringSet("Found source files:", sourceFiles);
		}
		return sourceFiles;
	}

	private Set getTestHeaderFiles() throws MojoExecutionException, MojoFailureException
	{
		if(testHeaderFiles == null)
		{
			testHeaderFiles = getHeaderFilesFromLocations(getTestProjectDirectory(), getCpp().getIncludePaths("test"));
			reportOnStringSet("Found test header files:", testHeaderFiles);
		}
		return testHeaderFiles;
	}

	private Set getTestSourceFiles() throws MojoExecutionException, MojoFailureException
	{
		if(testSourceFiles == null)
		{
			Set absoluteTestSourceFiles = new HashSet(getTestSourcesFor(getCpp()));
			testSourceFiles = getRelativePathsFromFiles(getTestProjectDirectory(), absoluteTestSourceFiles);
			reportOnStringSet("Found test source files:", testSourceFiles);
		}
		return testSourceFiles;
	}

	private void generateGUIDS()
	{
		solutionGUID = UUID.randomUUID().toString();
		mainProjectGUID = UUID.randomUUID().toString();
		testProjectGUID = UUID.randomUUID().toString();
	}

	private void createMainProjectFile() throws MojoExecutionException, MojoFailureException
	{
		File mainProject = new File(getProjectDirectory(), getProjectName() + ".vcxproj");
		VisualStudioTemplateModifier modifier =
			new VisualStudioProjectTemplateModifier("VS2012ProjectTemplate.txt", mainProject,
					mainProjectGUID, getProjectName(), getBinding(), getIncludes(), getLibraryPaths(), getDefines(),
					getLibraryFiles(), getHeaderFiles(), getSourceFiles());
		modifier.createPopulatedOutput();
	}

	private void createTestProjectFile() throws MojoExecutionException, MojoFailureException
	{
		File testProject = new File(getTestProjectDirectory(), getTestProjectName() + ".vcxproj");
		VisualStudioTemplateModifier modifier =
			new VisualStudioProjectTemplateModifier("VS2012TestProjectTemplate.txt", testProject,
					testProjectGUID, getTestProjectName(), Library.EXECUTABLE, getIncludes(), getTestLibraryPaths(), new HashSet(),
					getTestLibraryFiles(), getTestHeaderFiles(), getTestSourceFiles());
		modifier.createPopulatedOutput();
	}

	private void createSolution() throws MojoExecutionException
	{
		File solution = new File(getSolutionDirectory(), getMavenProject().getName() + "Solution.sln");
		VisualStudioTemplateModifier modifier =
			new VisualStudioSolutionTemplateModifier("VS2012SolutionTemplate.txt", solution,
					solutionGUID, getProjectName(), getProjectDirectory().getName(), mainProjectGUID,
					getTestProjectName(), getTestProjectDirectory().getName(), testProjectGUID);
		modifier.createPopulatedOutput();
	}

	private String getProjectName()
	{
		return getMavenProject().getName() + "Project";
	}

	private String getTestProjectName()
	{
		return getMavenProject().getName() + "TestProject";
	}

	private void createSolutionFiolders()
	{
		getSolutionDirectory().mkdir();
		getProjectDirectory().mkdir();
		getTestProjectDirectory().mkdir();
	}

	private File getSolutionDirectory()
	{
		if(solutionDir == null)
			solutionDir = new File(getBasedir(), getMavenProject().getName() + "Solution");
		return solutionDir;
	}

	private File getProjectDirectory()
	{
		if(mainProjectDir == null)
			mainProjectDir = new File(getSolutionDirectory(), getProjectName());
		return mainProjectDir;
	}

	private File getTestProjectDirectory()
	{
		if(testProjectDir == null)
			testProjectDir = new File(getSolutionDirectory(), getTestProjectName());
		return testProjectDir;
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

	private Collection getLibraries(Set libraryPaths, File baseDir)
	{
		Set libraries = new HashSet();
		for(Iterator i = libraryPaths.iterator(); i.hasNext();)
		{
			File libDir = new File(baseDir, (String) i.next());
			File[] libFiles =  libDir.listFiles(new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(".lib");
				}
			});
			for(int index = 0; index < libFiles.length; index++)
			{
				libraries.add(libFiles[index].getName());
			}
		}
		return libraries;
	}

	//Takes a set of String file paths
	private Set getRelativePaths(File sourceDir, Set targetPaths) throws MojoExecutionException
	{
		Set relativePaths  = new HashSet();
		for( Iterator it = targetPaths.iterator(); it.hasNext(); )
		{
			relativePaths.add( getRelativePath( sourceDir, ((String) it.next()) ) );
		}
		return relativePaths;
	}

	//Takes a set of Files
	private Set getRelativePathsFromFiles(File sourceDir, Set targetFiles) throws MojoExecutionException
	{
		Set relativePaths  = new HashSet();
		for( Iterator it = targetFiles.iterator(); it.hasNext(); )
		{
			relativePaths.add( getRelativePath( sourceDir, ((File) it.next()).getPath() ) );
		}
		return relativePaths;
	}

	private String getRelativePath(File sourceDir, String targetPath) throws MojoExecutionException
	{
        String pathSeparator = "\\";

		String[] base = sourceDir.getPath().split(Pattern.quote(pathSeparator));
        String[] target = targetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuffer common = new StringBuffer();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex]))
        {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0)
        {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativised.
            throw new MojoExecutionException("No common path element found for '" + targetPath + "' and '" + sourceDir
                    + "'");
        }

        StringBuffer relative = new StringBuffer();

        if (base.length != commonIndex)
        {
            int numDirsUp = base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++)
            {
                relative.append(".." + pathSeparator);
            }
        }
        relative.append(targetPath.substring(common.length()));
        return relative.toString();
	}
}
