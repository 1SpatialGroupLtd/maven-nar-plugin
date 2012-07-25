package org.apache.maven.plugin.nar;

import java.io.File;

public class VisualStudioSolutionTemplateModifier extends
		VisualStudioTemplateModifier
{
	private String mainProjectDir;
	private String mainProjectName;
	private String testProjectDir;
	private String testProjectName;
	private String solutionGUID;
	private String mainProjectGUID;
	private String testProjectGUID;

	public VisualStudioSolutionTemplateModifier(
			String templateFileName, File destinationFile, String solutionGUID,
			String mainProjectName, String mainProjectDir, String mainProjectGUID,
			String testProjectName, String testProjectDir, String testProjectGUID)
	{
		super(templateFileName, destinationFile);
		this.solutionGUID = solutionGUID;
		this.mainProjectDir = mainProjectDir;
		this.mainProjectName = mainProjectName;
		this.mainProjectGUID = mainProjectGUID;
		this.testProjectDir = testProjectDir;
		this.testProjectName = testProjectName;
		this.testProjectGUID = testProjectGUID;
	}

	protected String replacePlaceholders(String contents)
	{
		String modifiedContents = replace(contents, SOLUTION_GUID, solutionGUID);
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_GUID, mainProjectGUID);
		modifiedContents = replace(modifiedContents, TEST_PROJECT_GUID, testProjectGUID);
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_NAME, mainProjectName);
		modifiedContents = replace(modifiedContents, MAIN_PROJECT_PATH, mainProjectDir + "\\" + mainProjectName + ".vcxproj");
		modifiedContents = replace(modifiedContents, TEST_PROJECT_NAME, testProjectName);
		modifiedContents = replace(modifiedContents, TEST_PROJECT_PATH, testProjectDir + "\\" + testProjectName + ".vcxproj");

		return modifiedContents;
	}
}
