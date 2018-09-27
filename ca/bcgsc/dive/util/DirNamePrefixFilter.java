/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author kmnip
 */
public class DirNamePrefixFilter implements FilenameFilter{
    String prefix = null;
    
    public DirNamePrefixFilter(String prefix)
    {
        super();
        this.prefix = prefix;        
    }

    public boolean accept(File file, String string) {
        File f = new File(file.getAbsoluteFile() + File.separator + string);
        return f.isDirectory() && string.startsWith(prefix);
    }
}
