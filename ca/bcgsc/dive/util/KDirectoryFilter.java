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
import java.io.FileFilter;
import java.io.IOException;

public class KDirectoryFilter implements FileFilter{
    public boolean accept(File pathname) {
        if(pathname.isDirectory()) {
            try {
                String path = pathname.getCanonicalPath();
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
