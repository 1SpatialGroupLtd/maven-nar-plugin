package org.apache.maven.plugin.nar;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import org.apache.maven.model.Dependency;
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
    private static final String NUGET_LIST_COMMAND = "NuGet.exe list -allversions -Source";
    private static final String CONTENT_PLACEHOLDER = "<contentPlaceholder>";
    private static final String EMPTY_ARRAY = "@()";
    private static final String SHARED = "shared";
    private static final String NUPKG_EXTENSION = ".nupkg";
    private static final String NUGET_PACK_COMMAND = "NuGet.exe pack <nuspecFile>";
    private static final String TOOLS_LOCATION = "tools";
    private static final String INSTALL_SCRIPT_NAME = "Install.ps1";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String ID_ATTRIBUTE = "id";
    private static final String DEPENDENCIES_TAG = "dependencies";
    private static final String REFERENCES_TAG = "references";
    private static final String GROUP_TAG = "group";
    private static final String TARGET_FRAMEWORK_ATTRIBUTE = "targetFramework";
    private static final String DOT_NET_TARGET_FRAMEWORK = "Net45";
    private static final String WINRT_TARGET_FRAMEWORK = "NetCore45";
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
    private static final String LIB_LOCATION = "lib";
    private static final String DLL_EXTENSION = ".dll";
    private static final String WINMD_EXTENSION = ".winmd";
    private static final String PDB_EXTENSION = ".pdb";
    private static final String REFERENCE_TAG = "reference";
    private static final String UNINSTALL_SCRIPT_NAME = "Uninstall.ps1";
    private static final String UTILITIES_NAME = "InstallUtilities";
    private static final String UTILITIES_EXTENSION = ".psm1";

    /**
     * @parameter expression=""
     * @required
     */
    private String centralNugetPackageSource;

    private File nugetDir;
    private File nuspecFile;
    private String packageName;
    private Document nuspecDocument;
    private String version;
    private File nupkgFile;
    private File contentDirectory;

    public void narExecute() throws MojoFailureException,
            MojoExecutionException
    {
        //Only create nuget packages for modules we are explicitly told to.
        if(!createNugetPackage)
        {
            getLog().info("Not creating nuget package as createNugetPackage is false.");
            return;
        }

        String artifactId = getMavenProject().getArtifactId();
        packageName = convertToPackageName(artifactId);

        try
        {
            version = getNugetVersion();
            cleanNugetDirectory();
            createTemplateNuspecFile();
            populateNuspecFile();
            moveLibs();
            addScripts();
            packNugetPackage();
            copyToCentralPackageSource();
        }
        catch (Exception e)
        {
            throw new MojoFailureException("Failed to create NuGet package", e);
        }
    }

    private void moveFiles(String destination, List artifacts, FilenameFilter filefilter) throws MojoExecutionException, MojoFailureException, IOException
    {
        File targetDirectory = new File(nugetDir, LIB_LOCATION + File.separator + destination);

        for (Iterator i = artifacts.iterator(); i.hasNext();)
        {
            NarArtifact artifact = (NarArtifact) i.next();
            File libDir = getLayout().getLibDirectory(getTargetDirectory(),
                    artifact.getArtifactId(), artifact.getVersion(),
                    getAOL().toString(), SHARED); //We only care about dlls.

            getLog().debug("Source directory: " + libDir);
            getLog().debug("Destination directory: " + targetDirectory);

            File[] filesToCopy = libDir.listFiles(filefilter);
            if(filesToCopy != null)
                for(int j = 0; j < filesToCopy.length; j++)
                    copyToDirectory(filesToCopy[j], targetDirectory);
        }
    }

    private void moveLibs() throws MojoExecutionException, MojoFailureException, IOException
    {
        getLog().info("Copying binaries to lib folder");

        // Get dependencies
        List dependencies = getNarManager().getNarDependencies(Artifact.SCOPE_COMPILE);

        // Separate out the win RT dependencies

        // This is a list of the maven artifacts that want to go int the net framework lib folder
        // Hardcoding these is hacky as hell and I don't like it, but we are binning the nar plugin soon
        List netDependencyIds = new ArrayList();
        netDependencyIds.add("amalgam-com");
        netDependencyIds.add("amalgam-devices-communication");
        netDependencyIds.add("amalgam-devices-teststub");
        netDependencyIds.add("amalgam-devices-sharedcomponents");
        netDependencyIds.add("amalgam-devices-leicatotalstation");
        netDependencyIds.add("amalgam-devices-leicagps");
        netDependencyIds.add("amalgam-devices-GeoComS2K");
        netDependencyIds.add("amalgam-devices-nmealib");

        List winRTDependencies = new ArrayList();
        List netDependencies = new ArrayList();
        for (Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact dependency = (NarArtifact) i.next();
            if (netDependencyIds.contains(dependency.getArtifactId()))
            {
                netDependencies.add(dependency);
            }
            else
            {
                winRTDependencies.add(dependency);
            }
        }

        FilenameFilter filter = new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                return name.endsWith(DLL_EXTENSION)
                || name.endsWith(WINMD_EXTENSION)
                || name.endsWith(PDB_EXTENSION);
            }
        };

        moveFiles(DOT_NET_TARGET_FRAMEWORK, netDependencies, filter);
        moveFiles(WINRT_TARGET_FRAMEWORK, winRTDependencies, filter);
    }

    private String convertToPackageName(String artifactId) throws MojoExecutionException, MojoFailureException
    {
		String libs = getNarInfo().getLibs(getAOL());
		if (!libs.contains(","))
		{
			return libs;
		}
		return libs.split(",", 0)[0];
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

        nupkgFile = new File(nugetDir, packageName + "." + version + NUPKG_EXTENSION);
        if(!nupkgFile.exists())
            throw new MojoExecutionException("Failed to package " + nupkgFile.getName());
    }

    private void addScripts() throws MojoExecutionException, IOException, MojoFailureException
    {
        getLog().info("Adding scripts");
        File toolsDir = new File(nugetDir, TOOLS_LOCATION);
        createDirectory(toolsDir);

        File utilitiesDir = new File(toolsDir, UTILITIES_NAME);
        createDirectory(utilitiesDir);
        File utilitiesScript = new File(utilitiesDir, UTILITIES_NAME + UTILITIES_EXTENSION);
        addContentToScript(utilitiesScript, NarUtil.class.getResourceAsStream(UTILITIES_NAME + UTILITIES_EXTENSION));
        if(!utilitiesScript.exists())
            throw new MojoExecutionException("Problem copying utilities script");

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
        String[] contentNames = contentDirectory.list();
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
        removeElement(DEPENDENCIES_TAG);
        setElementContents(DESCRIPTION_TAG, getMavenProject().getDescription());
        removeElement(RELEASE_NOTES_TAG);
        setElementContents(TAGS_TAG, PRODUCT_NAME);
        addReference();

        saveNuspec();
    }

    private Element createReferenceGroup( List dependencies
                                        , String targetFramework
                                        , String extension
                                        ) throws MojoExecutionException, MojoFailureException
    {
        Element group = nuspecDocument.createElement(GROUP_TAG);
        group.setAttribute(TARGET_FRAMEWORK_ATTRIBUTE, targetFramework);

        for (Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact dependency = (NarArtifact) i.next();

            // Get the list of lib names of the current dependency.
            String libs = dependency.getNarInfo().getLibs(getAOL());
            String libName = "";
            if (libs != null)
            {
              String[] libsList = libs.split(", ");

              // Adopt the first as the name of the reference.
              libName = libsList[0].trim();
            }

            String referenceName = libName + extension;
            Element reference = nuspecDocument.createElement(REFERENCE_TAG);
            reference.setAttribute(FILE_ATTRIBUTE, referenceName);
            group.appendChild(reference);
        }

        return group;
    }

    private void addReference() throws MojoExecutionException, MojoFailureException
    {
        // Get dependencies
        List dependencies = getNarManager().getDirectNarDependencies(Artifact.SCOPE_COMPILE);

        // Separate out the win RT dependencies
        List winRTDependencies = new ArrayList();
        for (Iterator i = dependencies.iterator(); i.hasNext();)
        {
            NarArtifact dependency = (NarArtifact) i.next();
            if (isWinRT(dependency.getNarInfo()))
            {
                winRTDependencies.add(dependency);
                i.remove();
            }
        }
        // Create the reference groups
        List groupElements = new ArrayList();
        if (dependencies.size() != 0)
        {
            groupElements.add(createReferenceGroup(dependencies, DOT_NET_TARGET_FRAMEWORK, DLL_EXTENSION));
        }
        if (winRTDependencies.size() != 0)
        {
            groupElements.add(createReferenceGroup(winRTDependencies, WINRT_TARGET_FRAMEWORK, WINMD_EXTENSION));
        }

        // Add the reference groups to the nuspec file
        if (groupElements.size() != 0)
        {
            Element references = nuspecDocument.createElement(REFERENCES_TAG);

            for (Iterator i = groupElements.iterator(); i.hasNext();)
            {
                Element group = (Element) i.next();
                references.appendChild(group);
            }
            getNamedNode(METADATA_TAG).appendChild(references);
        }
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
        String majorMinorBuild = getNugetMajorMinorBuildVersion(version);
        String revision = getRevisionNumber(majorMinorBuild);
        return majorMinorBuild + "." + revision;
    }

    private String getRevisionNumber(String majorMinorBuild) throws IOException, InterruptedException
    {
        String revision = "0"; //Default value for first snapshot package
		int latestRevision = 0; // Holder for the highest revision number we have found so far
        CommandResult result = runCommand(NUGET_LIST_COMMAND + " " + centralNugetPackageSource);

        for(Iterator it = result.output.iterator(); it.hasNext();)
        {
            String line  = (String)it.next();
            getLog().debug(line);
            if(!line.startsWith(packageName))
                continue;
            String latestVersion = line.substring(packageName.length() + 1);
            if(!latestVersion.startsWith(majorMinorBuild))
                continue;
            String buildRevision = latestVersion.substring(majorMinorBuild.length() + 1);
            int foundRevision = Integer.parseInt(buildRevision);
			if (foundRevision >= latestRevision)
			{
				latestRevision = foundRevision;
				revision = Integer.toString(foundRevision + 1);
			}
        }
        return revision;
    }

    private String getNugetMajorMinorBuildVersion(String version)
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

    private void copyToDirectory(File file, File destinationDir) throws IOException
    {
        getLog().debug("Copying " + file.getName());
        FileUtils.copyFileToDirectory(file, destinationDir);
    }

    private boolean isWinRT(NarInfo info) throws MojoExecutionException, MojoFailureException
    {
        return info.isTargetWinRT(getAOL());
    }

    private void createTemplateNuspecFile() throws IOException, InterruptedException, MojoExecutionException, SAXException, ParserConfigurationException
    {
        String command = NUGET_SPEC_COMMAND + " " + packageName;
        runCommandLogOutput(command);

        nuspecFile = new File(nugetDir, packageName + NUSPEC_EXTENSION);
        if (!nuspecFile.exists())
            throw new MojoExecutionException("Failed to create " + nuspecFile.getName());

        createNuspecDocument();
    }

    private void runCommandLogOutput(String command) throws IOException,
            InterruptedException, MojoExecutionException
    {
        CommandResult result = runCommand(command);

        for(Iterator it = result.output.iterator(); it.hasNext();)
            getLog().debug((String)it.next());

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
        StreamEater eater = new StreamEater(specProcess.getInputStream(), result.output);
        eater.start();

        result.exitCode = specProcess.waitFor();

        getLog().debug("Command " + command + " returned: " + result.exitCode);

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
        List output;

        CommandResult()
        {
            output = new ArrayList();
        }
    }

    private class StreamEater extends Thread
    {
        InputStream is;
        List outputStrings;

        StreamEater(InputStream is, List outputStrings)
        {
            this.is = is;
            this.outputStrings = outputStrings;
        }

        public void run()
        {
            try
            {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                boolean output = true;
                while((line = br.readLine()) != null || output) //the line assignment needs to be first else it is short circuited
                {
                    if(line == null)
                    {
                        output = false;
                        sleep(1000);
                    }
                    else
                    {
                        output = true;
                        outputStrings.add(line);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
