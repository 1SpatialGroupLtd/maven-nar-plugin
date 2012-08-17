package org.apache.maven.plugin.nar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

public class VisualStudioSolutionTemplateModifier extends
		VisualStudioTemplateModifier
{
/*	private String mainProjectDir;
	private String mainProjectName;
	private String testProjectDir;
	private String testProjectName;
	private String mainProjectGUID;
	private String testProjectGUID;
*/
	private String solutionGUID;
	private VS2012Project mainProject;
	private VS2012Project testProject;
	private VS2012Project dependencyProject;

	public VisualStudioSolutionTemplateModifier(String templateFileName, File destinationFile,
			String solutionGUID, VS2012Project mainProject,
			VS2012Project testProject, VS2012Project dependencyProject)
	{
		super(templateFileName, destinationFile);
		this.solutionGUID = solutionGUID;
		this.mainProject = mainProject;
		this.testProject = testProject;
		this.dependencyProject = dependencyProject;
	}

	protected String replacePlaceholders(String contents) throws MojoExecutionException
	{
		String modifiedContents = replace(contents, SOLUTION_GUID, solutionGUID);
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_GUID, mainProject.getGUID());
		modifiedContents = replace(modifiedContents, TEST_PROJECT_GUID, testProject.getGUID());
		modifiedContents = replace(modifiedContents, DEPENDENCY_PROJECT_GUID, dependencyProject.getGUID());
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_NAME, mainProject.getName());
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_PATH, mainProject.getRelativeProjectPath());
		modifiedContents = replace(modifiedContents, TEST_PROJECT_NAME, testProject.getName());
		modifiedContents = replace(modifiedContents, TEST_PROJECT_PATH, testProject.getRelativeProjectPath());
		modifiedContents = replace(modifiedContents, DEPENDENCY_PROJECT_NAME, dependencyProject.getName());
		modifiedContents = replace(modifiedContents, DEPENDENCY_PROJECT_PATH, dependencyProject.getRelativeProjectPath());

		return modifiedContents;
	}
}
