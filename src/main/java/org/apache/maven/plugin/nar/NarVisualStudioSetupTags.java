package org.apache.maven.plugin.nar;

public interface NarVisualStudioSetupTags
{
	//Solution template tags
	public static final String TEST_PROJECT_PATH = "[#testProjectPath#]";
	public static final String TEST_PROJECT_NAME = "[#testProjectName#]";
	public static final String TEST_PROJECT_GUID = "[#testProjectGUID#]";
	public static final String DEPENDENCY_PROJECT_PATH = "[#dependencyProjectPath#]";
	public static final String DEPENDENCY_PROJECT_NAME = "[#dependencyProjectName#]";
	public static final String DEPENDENCY_PROJECT_GUID = "[#dependencyProjectGUID#]";
	public static final String MAIN_PROJECT_PATH = "[#mainProjectPath#]";
	public static final String MAIN_PROJECT_NAME = "[#mainProjectName#]";
	public static final String MAIN_PROJECT_GUID = "[#mainProjectGUID#]";
	public static final String SOLUTION_GUID = "[#solutionGUID#]";

	//project template tags
	public static final String PROJECT_GUID = "[#projectGUID#]";
	public static final String PROJECT_NAME = "[#projectName#]";
	public static final String LIBRARY_TYPE = "[#libraryType#]";
	public static final String INCLUDES = "[#includes#]";
	public static final String LIBRARY_PATHS = "[#libraryPaths#]";
	public static final String DEFINES = "[#defines#]";
	public static final String LIBRARIES = "[#libraries#]";
	public static final String LIBRARIES_DEBUG = "[#librariesDebug#]";
	public static final String HEADER_FILE_ELEMENTS = "[#headerFileElements#]";
	public static final String SOURCE_FILE_ELEMENTS = "[#sourceFileElements#]";
	public static final String NEW_GUID = "[#newGUID#]";
	public static final String USE_PRE_COMPILED_HEADERS = "[#usePreCompiledHeader#]";
	public static final String CLEAN_PRE_COMPILED_HEADERS = "[#cleanPreCompiledHeaders#]";
	public static final String PRE_COMPILED_HEADER_H = "[#preCompiledHeader.h#]";
	public static final String PRE_COMPILED_HEADER_PDB = "[#preCompiledHeader.pdb#]";
	public static final String FORCED_INCLUDES = "[#forcedIncludes#]";
	public static final String PRE_COMPILED_HEADER_H_DEBUG = "[#preCompiledHeader.hDebug#]";
	public static final String PRE_COMPILED_HEADER_PDB_DEBUG = "[#preCompiledHeader.pdbDebug#]";
	public static final String FORCED_INCLUDES_DEBUG = "[#forcedIncludesDebug#]";
}
