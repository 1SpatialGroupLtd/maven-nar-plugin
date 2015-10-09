# Get the correct folder name to use for the target framework of the target $project
# This duplicates work done by nuget (which is unfortunately not available in powershell) and will need changing if nuget
# changes its package layout (specifically with respect to target frameworks)
# This only handles the NetCore (winRT and universal apps) and Net (.Net framework) target frameworks
# An exception will be thrown for other target frameworks
function Get-FrameworkFolder
{
	param
	(
		$project
	)

	$keys = @{}
	$keys[".NetCore"] = "NetCore"
	$keys[".NetFramework"] = "Net" # TargetFramework mappings are not always this simple
	
	# Target framework monikers are of the form <dte framework>,version=<version> where version is <major>.<minor>.<others>
	# We want <nuget framework><major><minor>
	$targetFrameworkMoniker = $project.Properties.Item("TargetFrameworkMoniker").Value
	$parts = $targetFrameworkMoniker -Split ',' -Match '\S' |% Trim # Split on comma, ignore empty entries and trim whitespace
	$targetFrameWorkName = $keys[$parts[0]]
	
	if(-not $targetFrameWorkName)
	{
		Throw "Unexpected target framework name " + $parts[0]
	}
	
	# Get the major and minor versions together
	$versions = ($parts[1] -Split "=")[1].TrimStart("v") -Split "\." 
	if($versions.count -lt 2)
	{
		Throw "Could not get major and minor version numbers from " + ($versions -join ".")
	}
	$version = $versions[0] + $versions[1]
	
	$targetFrameworkName + $version
}

# Get the files in the $frameworkFolder folder under $installPath\lib that do not have winmd files associated with them
# (i.e. share the same name (excluding extensions))
function Get-NativeFiles
{
	param
	(
		$installPath,
		$frameworkFolder
	)

	$files = Get-ChildItem ($installPath + '\lib\' + $frameworkFolder)
	$files |? `
	{
		$name = ($_.Name -Split '\.')[0]
		$keep = $true
		foreach ($file in $files)
		{
			if($file.Name -eq $name+'.winmd')
			{
				$keep = $false
			}
		}
		$keep
	}
}

# Rolls back the instalation of the $package on the $project, typically called in failure conditions
function Undo-Install
{
	param
	(
		$package,
		$project
	)

	$packageParts = -Split $package
	Uninstall-package $packageParts[0] -ProjectName $project.ProjectName
}

# Adds the files specified in $content as CopyAlways content to $project
function Add-AsContent
{
	param
	(
		$content,
		$project
	)

	$content |% `
	{
		$projectItem = $project.ProjectItems.AddFromFile($_.FullName)
		$projectItem.Properties.Item("CopyToOutputDirectory").Value = 1 # 1 is copy always
	}
	$project.Save()
}

# Removes the files specified in $content from $project if they are present
function Remove-Content
{
	param
	(
		$content,
		$project
	)

	$projectItemNames = $project.ProjectItems |% {$_.Name}
	$content |% `
	{
		if($projectItemNames -contains $_.Name)
		{
			$project.ProjectItems.Item($_.Name).Delete()
		}
	}
	$Project.Save
}
