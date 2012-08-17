package org.apache.maven.plugin.nar;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;

public class RelativePathUtils
{
    //Takes a set of String file paths
    static Set getRelativePaths(File sourceDir, Set targetPaths) throws MojoExecutionException
    {
        Set relativePaths  = new HashSet();
        for( Iterator it = targetPaths.iterator(); it.hasNext(); )
        {
            relativePaths.add( getRelativePath( sourceDir, ((String) it.next()) ) );
        }
        return relativePaths;
    }

    static String getRelativePath(File sourceDir, String targetPath) throws MojoExecutionException
    {
        String pathSeparator = "\\";

        String[] base = sourceDir.getPath().split(Pattern.quote(pathSeparator));
        String[] target = targetPath.split(Pattern.quote(pathSeparator));

        // First get all the common elements. Store them as a string,
        // and also count how many of them there are.
        StringBuffer common = new StringBuffer();

        int commonIndex = 0;
        while (commonIndex < target.length && commonIndex < base.length
                && target[commonIndex].equals(base[commonIndex]))
        {
            common.append(target[commonIndex] + pathSeparator);
            commonIndex++;
        }

        if (commonIndex == 0)
        {
            // No single common path element. This most
            // likely indicates differing drive letters, like C: and D:.
            // These paths cannot be relativised.
            throw new MojoExecutionException("No common path element found for '" + targetPath + "' and '" + sourceDir
                    + "'");
        }

        StringBuffer relative = new StringBuffer();

        if (base.length != commonIndex)
        {
            int numDirsUp = base.length - commonIndex;

            for (int i = 0; i < numDirsUp; i++)
            {
                relative.append(".." + pathSeparator);
            }
        }
        relative.append(targetPath.substring(common.length()));
        return relative.toString();
    }
}
