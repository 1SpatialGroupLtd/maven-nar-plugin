package org.apache.maven.plugin.nar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.booter.shade.org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Creates a nuget package for a module
 *
 * @goal nar-create-nuget
 * @phase package
 * @requiresProject
 * @requiresDependencyCollection compile
 *
 * @author Mike Boyd
 */
public class NarNugetMojo extends AbstractCompileMojo
{
    private static final String NUGET_LIST_COMMAND = "NuGet.exe list -Source";
	private static final String CONTENT_PLACEHOLDER = "<contentPlaceholder>";
	private static final String EMPTY_ARRAY = "@()";
	private static final String SHARED = "shared";
	private static final String NUPKG_EXTENSION = ".nupkg";
	private static final String NUGET_PACK_COMMAND = "NuGet.exe pack <nuspecFile> -NoPackageAnalysis";
	private static final String TOOLS_LOCATION = "tools";
	private static final String INSTALL_SCRIPT_NAME = "install.ps1";
	private static final String VERSION_ATTRIBUTE = "version";
	private static final String ID_ATTRIBUTE = "id";
	private static final String DEPENDENCY_TAG = "dependency";
	private static final String DEPENDENCIES_TAG = "dependencies";
	private static final String REFERENCES_TAG = "references";
	private static final String METADATA_TAG = "metadata";
	private static final String FILE_ATTRIBUTE = "file";
	private static final String PRODUCT_NAME = "FeatureEditor";
	private static final String OWNER = "1Spatial";
	private static final String TAGS_TAG = "tags";
	private static final String RELEASE_NOTES_TAG = "releaseNotes";
	private static final String DESCRIPTION_TAG = "description";
	private static final String ICON_URL_TAG = "iconUrl";
	private static final String PROJECT_URL_TAG = "projectUrl";
	private static final String LICENSE_URL_TAG = "licenseUrl";
	private static final String AUTHORS_TAG = "authors";
	private static final String OWNERS_TAG = "owners";
	private static final String VERSION_TAG = VERSION_ATTRIBUTE;
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	private static final String NUGET_SPEC_COMMAND = "NuGet.exe spec";
	private static final String NUGET_LOCATION = "NuGet";
	private static final String NUSPEC_EXTENSION = ".nuspec";
	private static final String CONTENT_LOCATION = "content";
	private static final String LIB_LOCATION = "lib";
	private static final String WINRT_FRAMEWORK = "WinRT45";
	private static final String DLL_EXTENSION = ".dll";
	private static final String WINMD_EXTENSION = ".winmd";
	private static final String REFERENCE_TAG = "reference";
	private static final String UNINSTALL_SCRIPT_NAME = "uninstall.ps1";

	/**
	 * @parameter expression=""
	 * @required
	 */
	private String centralNugetPackageSource;

	/**
	 * @parameter expression="" default="false"
	 */
	private boolean createNugetPackage;

	private File nugetDir;
	private File nuspecFile;
	private String moduleName;
	private File dllDirectory;
	private Document nuspecDocument;
	private String version;
	private File nupkgFile;

	public void narExecute() throws MojoFailureException,
			MojoExecutionException
	{
		//Only create nuget packages for modules we are explicitly told to.
		if(!createNugetPackage)
		{
			getLog().info("Not creating nuget package as createNugetPackage is false.");
			return;
		}

		moduleName = getMavenProject().getArtifactId();
		try
		{
			version = getNugetVersion();
			cleanNugetDirectory();
			createTemplateNuspecFile();
			populateNuspecFile();
			createDllDirectory();
			moveDlls();
			addScripts();
			packNugetPackage();
			copyToCentralPackageSource();
		}
		catch (Exception e)
		{
			throw new MojoFailureException("Failed to create NuGet package", e);
		}
	}

	private void copyToCentralPackageSource() throws MojoExecutionException, IOException
	{
		getLog().info("Copying package to local package source");
		File packageSource = new File(centralNugetPackageSource);
		if(!packageSource.exists())
			throw new MojoExecutionException("Package source " + centralNugetPackageSource + " does not exist");
		getLog().debug("Package source: " +centralNugetPackageSource);
		copyToDirectory(nupkgFile, packageSource);
	}

	private void packNugetPackage() throws MojoExecutionException, IOException, InterruptedException
	{
		String command = NUGET_PACK_COMMAND.replace("<nuspecFile>", nuspecFile.getName());
		runCommandLogOutput(command);

		nupkgFile = new File(nugetDir, moduleName + "." + version + NUPKG_EXTENSION);
		if(!nupkgFile.exists())
			throw new MojoExecutionException("Failed to package " + nupkgFile.getName());
	}

	private void addScripts() throws MojoExecutionException, IOException, MojoFailureException
	{
		getLog().info("Adding install script");
		File toolsDir = new File(nugetDir, TOOLS_LOCATION);
		createDirectory(toolsDir);

		File installScript = new File(toolsDir, INSTALL_SCRIPT_NAME);
		addContentToScript(installScript, NarUtil.class.getResourceAsStream(INSTALL_SCRIPT_NAME));
		if(!installScript.exists())
			throw new MojoExecutionException("Problem copying install script");

		File uninstallScript = new File(toolsDir, UNINSTALL_SCRIPT_NAME);
		addContentToScript(uninstallScript, NarUtil.class.getResourceAsStream(UNINSTALL_SCRIPT_NAME));
		if(!uninstallScript.exists())
			throw new MojoExecutionException("Problem copying uninstall script");
	}

	private void addContentToScript(File installScriptOutput, InputStream installScriptResourceStream)
			throws IOException, MojoExecutionException, MojoFailureException {
		BufferedReader scriptInput = null;
		BufferedWriter scriptOutput = null;
		try
		{
			scriptInput = new BufferedReader(new InputStreamReader(installScriptResourceStream));
			scriptOutput = new BufferedWriter(new FileWriter(installScriptOutput));

			String line = null;
			while (( line = scriptInput.readLine()) != null)
			{
				scriptOutput.write(replacePlaceholders(line));
				scriptOutput.write(System.getProperty("line.separator"));
			}
			scriptOutput.flush();
		}
		finally
		{
			if(scriptInput != null)
				scriptInput.close();
			if(scriptOutput != null)
				scriptOutput.close();
		}
	}

	private String replacePlaceholders(String line) throws MojoExecutionException, MojoFailureException
	{
		if(!line.contains(CONTENT_PLACEHOLDER))
			return line;
		String content;
		if(isWinRT())
			content = EMPTY_ARRAY;
		else
		{
			String[] contentNames = dllDirectory.list();
			if(contentNames.length == 0)
				content = EMPTY_ARRAY;
			else
			{
				StringBuilder builder = new StringBuilder();
				for(int i = 0; i < contentNames.length; i++)
				{
					builder.append("\"" + contentNames[i] + "\"");
					if(i < contentNames.length - 1)
						builder.append(",");
				}
				content = builder.toString();
			}
		}
		getLog().debug("Setting content to " + content);
		return line.replaceAll(CONTENT_PLACEHOLDER, content);
	}

	private void populateNuspecFile() throws MojoExecutionException, TransformerException, MojoFailureException
	{
		getLog().info("Populating " + nuspecFile);
		setElementContents(VERSION_TAG, version);
		setElementContents(OWNERS_TAG, OWNER);
		setElementContents(AUTHORS_TAG, OWNER);
		removeElement(LICENSE_URL_TAG);
		removeElement(PROJECT_URL_TAG);
		removeElement(ICON_URL_TAG);
		setElementContents(DESCRIPTION_TAG, getMavenProject().getDescription());
		removeElement(RELEASE_NOTES_TAG);
		setElementContents(TAGS_TAG, PRODUCT_NAME);
		setDependencies();
		if(isWinRT())
			addReference();

		saveNuspec();
	}

	private void setDependencies() throws MojoExecutionException, MojoFailureException
	{
		List dependencies = getNarManager().getDirectNarDependencies(Artifact.SCOPE_COMPILE);
		int numDependencies = dependencies.size();
		getLog().debug("Adding " + numDependencies + " dependencies");
		if(numDependencies == 0)
		{
			removeElement(DEPENDENCIES_TAG);
			return;
		}
		removeElement(DEPENDENCY_TAG);
		for(Iterator i = dependencies.iterator(); i.hasNext();)
		{
			NarArtifact dependency = (NarArtifact) i.next();
			String dependencyName = dependency.getArtifactId();

			if(!dependency.getNarInfo().getBinding(getAOL(), "").equals(SHARED))
			{
				getLog().debug("Not adding dependency " + dependencyName + " as it has no dlls");
				continue;
			}
			getLog().debug("Adding dependency " + dependencyName);
			Element dependencyElement = nuspecDocument.createElement(DEPENDENCY_TAG);
			dependencyElement.setAttribute(ID_ATTRIBUTE, dependencyName);
			dependencyElement.setAttribute(VERSION_ATTRIBUTE, getNugetMajorMinorVersion(dependency.getVersion()));
			getNamedNode(DEPENDENCIES_TAG).appendChild(dependencyElement);
		}
	}

	private void addReference() throws MojoExecutionException, MojoFailureException
	{
		String referenceName = getOutput(getAOL()) + WINMD_EXTENSION;
		getLog().debug("Adding " + referenceName + " as reference");
		Element referenceElement = nuspecDocument.createElement(REFERENCE_TAG);
		referenceElement.setAttribute(FILE_ATTRIBUTE, referenceName);
		Element parent = nuspecDocument.createElement(REFERENCES_TAG);
		parent.appendChild(referenceElement);
		getNamedNode(METADATA_TAG).appendChild(parent);
	}

	private void removeElement(String tagname) throws MojoExecutionException
	{
		getLog().debug("Removing element " + tagname);
		Node node = getNamedNode(tagname);
		node.getParentNode().removeChild(node);
	}

	private void saveNuspec() throws TransformerException
	{
        Source source = new DOMSource(nuspecDocument);
        Result result = new StreamResult(nuspecFile);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
	}

	private void setElementContents(String tagname, String contents) throws MojoExecutionException
	{
		getLog().debug("Setting contents of " + tagname + " to " + contents);
		Node node = getNamedNode(tagname);
		node.getFirstChild().setNodeValue(contents);
	}

	private Node getNamedNode(String tagname) throws MojoExecutionException {
		NodeList nodes = nuspecDocument.getElementsByTagName(tagname);
		if(nodes.getLength() == 0)
			throw new MojoExecutionException("No element named " + tagname + " present in " + nuspecFile);
		if(nodes.getLength() > 1)
			throw new MojoExecutionException("Multiple elements named " + tagname + " present in " + nuspecFile);
		return nodes.item(0);
	}

	private String getNugetVersion() throws IOException, InterruptedException
	{
		getLog().info("Calculating NuGet version number");
		String version = getMavenProject().getVersion();
		String majorMinor = getNugetMajorMinorVersion(version);
		String build;
		String revision;
		if(getSnapshotIndex(version) == -1)
		{
			build = "2"; //indicates release version (must be higher than snapshot version)
			revision = "0";
		}
		else
		{
			build = "1"; //indicates snapshot
			revision = getBuildNumber(majorMinor, build);
		}
		return majorMinor + "." + build + "." + revision;
	}

	private String getBuildNumber(String majorMinor, String build) throws IOException, InterruptedException
	{
		String revision = "0"; //Default value for first snapshot package
		CommandResult result = runCommand(NUGET_LIST_COMMAND + " " + centralNugetPackageSource);

		String line = null;
		while((line = result.output.readLine()) != null)
		{
			getLog().debug(line);
			if(!line.startsWith(moduleName))
				continue;
			String latestVersion = line.substring(moduleName.length() + 1);
			if(!latestVersion.startsWith(majorMinor))
				break;
			String buildRevision = latestVersion.substring(majorMinor.length() + 1);
			if(!buildRevision.startsWith(build))
				break;
			int latestRevision = Integer.parseInt(buildRevision.substring(build.length() + 1));
			revision = Integer.toString(latestRevision + 1);
		}
		return revision;
	}

	private String getNugetMajorMinorVersion(String version)
	{
		int snapshotIndex = getSnapshotIndex(version);
		if(snapshotIndex != -1)
			version = version.substring(0, snapshotIndex);

		return version;
	}

	private int getSnapshotIndex(String version)
	{
		return version.indexOf(SNAPSHOT_SUFFIX);
	}

	//WinRT: copy over dll and winmd
	//other: copy over dll
	private void moveDlls() throws MojoExecutionException, MojoFailureException, IOException
	{
		getLog().info("Copying dlls");
		MavenProject mavenProject = getMavenProject();
		File libDir = getLayout().getLibDirectory(getTargetDirectory(),
				mavenProject.getArtifactId(), mavenProject.getVersion(),
				getAOL().toString(), SHARED); //We only care about dlls.
		getLog().debug("Source directory: " + libDir);
		getLog().debug("Destination directory: " + dllDirectory);

		FilenameFilter filter;
		if(isWinRT())
			filter = new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(DLL_EXTENSION) || name.endsWith(WINMD_EXTENSION);
				}
			};
		else
			filter = new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.endsWith(DLL_EXTENSION);
				}
			};
		File[] filesToCopy = libDir.listFiles(filter);
		if(filesToCopy != null)
			for(int i = 0; i < filesToCopy.length; i++)
				copyToDirectory(filesToCopy[i], dllDirectory);
	}

	private void copyToDirectory(File file, File destinationDir) throws IOException
	{
		getLog().debug("Copying " + file.getName());
		FileUtils.copyFileToDirectory(file, destinationDir);
	}

	//WinRT dll (and winmd) files want to go under /lib/WinRT<version>
	//other dlls want to go under /content
	private void createDllDirectory() throws MojoExecutionException, MojoFailureException
	{
		String path;
		if(isWinRT())
			path = LIB_LOCATION + File.separator + WINRT_FRAMEWORK;
		else
			path = CONTENT_LOCATION;
		dllDirectory = new File(nugetDir, path);
		getLog().info("Creating " + dllDirectory);
		createDirectory(dllDirectory);
	}

	private boolean isWinRT() throws MojoExecutionException, MojoFailureException
	{
		return getNarInfo().isTargetWinRT(getAOL());
	}

	private void createTemplateNuspecFile() throws IOException, InterruptedException, MojoExecutionException, SAXException, ParserConfigurationException
	{
		String command = NUGET_SPEC_COMMAND + " " + moduleName;
		runCommandLogOutput(command);

		nuspecFile = new File(nugetDir, moduleName + NUSPEC_EXTENSION);
		if (!nuspecFile.exists())
			throw new MojoExecutionException("Failed to create " + nuspecFile.getName());

		createNuspecDocument();
	}

	private void runCommandLogOutput(String command) throws IOException,
			InterruptedException, MojoExecutionException
	{
		CommandResult result = runCommand(command);

		String line = null;
		while((line = result.output.readLine()) != null)
			getLog().debug(line);

		getLog().debug("Command " + command + " returned: " + result.exitCode);
		if(result.exitCode != 0)
			throw new MojoExecutionException("Problem running command " + command + ". Exit code: " + result.exitCode);
	}

	private CommandResult runCommand(String command) throws IOException, InterruptedException
	{
		getLog().info("Running command: " + command);

		ProcessBuilder builder = new ProcessBuilder(command.split(" "));
		builder.directory(nugetDir);
		builder.redirectErrorStream(true);

		Process specProcess = builder.start();
		CommandResult result = new CommandResult();
		result.exitCode = specProcess.waitFor();
		result.output = new BufferedReader(new InputStreamReader(specProcess.getInputStream()));

		return result;
	}

	private void createNuspecDocument() throws SAXException, IOException, ParserConfigurationException
	{
		nuspecDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(nuspecFile);
	}

	private void cleanNugetDirectory() throws MojoExecutionException, IOException
	{
		nugetDir = new File(getOutputDirectory(), NUGET_LOCATION);
		getLog().info("Cleaning " + nugetDir);
		if(nugetDir.exists())
			if(!deleteDirectory(nugetDir))
				throw new MojoExecutionException("Could not delete " + nugetDir);
		createDirectory(nugetDir);
	}

	private void createDirectory(File directory) throws MojoExecutionException
	{
		directory.mkdirs();
		if(!directory.exists())
			throw new MojoExecutionException("Could not create directory " + directory);
	}

	private boolean deleteDirectory(File directory) throws IOException
	{
		FileUtils.deleteDirectory(directory);
		return !directory.exists();
	}

	private class CommandResult
	{
		int exitCode;
		BufferedReader output;
	}
}
