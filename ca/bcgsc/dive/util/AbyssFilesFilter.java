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

public class AbyssFilesFilter implements FileFilter{
    public boolean accept(File f) {

        String name = f.getName();
        if(name.endsWith(".fa") || name.endsWith(".adj") || name.equals("coverage.hist")){
            return true;
        }

        return false;
    }
}
