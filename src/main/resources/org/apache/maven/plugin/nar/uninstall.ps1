param($installPath, $toolsPath, $package, $project)

$contentArray = <contentPlaceholder>

foreach ($content in $contentArray)
{
	$project.ProjectItems.Item($content).Remove()
}
