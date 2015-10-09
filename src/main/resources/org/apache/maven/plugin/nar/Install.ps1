param
(
	$installPath,
	$toolsPath, 
	$package, 
	$project
)

# Import the utilities
$oldPSModulePath = $env:PSModulePath
$env:PSModulePath = $env:PSModulePath + ';' + $toolsPath
Import-Module InstallUtilities

try
{
	$frameworkFolder = Get-FrameworkFolder $project
	$content = Get-NativeFiles $installPath $frameworkFolder
	Add-AsContent $content $project
}
catch
{
	Write-Warning $_
	Undo-Install $package $project
}

# And unimport them (if this has not already happened
# (e.g. in exception circumstances)
if ((Get-Module |? {$_.Name -eq "InstallUtilities"}))
{
	Remove-Module InstallUtilities
}
$env:PSModulePath = $oldPSModulePath
