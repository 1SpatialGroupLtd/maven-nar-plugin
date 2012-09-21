param($installPath, $toolsPath, $package, $project)

$contentArray = <contentPlaceholder>

foreach ($content in $contentArray)
{
	$Item = $project.ProjectItems.Item($content)

	if($Item)
	{
		$Item.Remove()
	}
}
