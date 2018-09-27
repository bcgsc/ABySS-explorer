/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.util;

/**
 *
 * @author kmnip
 */
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class KDirectoryNameFilter implements FilenameFilter{
    public boolean accept(File file, String string) {
        File f = new File(file.getAbsolutePath()+File.separator+string);
        if(f.isDirectory())
        {
            try {
                String path = f.getCanonicalPath();
                int k = Utilities.getKFromPath(path);
                if(k <= 0)
                    return false;
            } catch (IOException e) {
                return false;
            }
            return true;
        }
        return false;
    }
}
