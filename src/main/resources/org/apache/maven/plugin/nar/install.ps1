param($installPath, $toolsPath, $package, $project)

$contentArray = <contentPlaceholder>

foreach ($content in $contentArray)
{
	$item = $project.ProjectItems.Item($content)

	# set 'Copy To Output Directory' to 'Copy if newer'
	$copyToOutput = $item.Properties.Item("CopyToOutputDirectory")
	$copyToOutput.Value = 2
}
