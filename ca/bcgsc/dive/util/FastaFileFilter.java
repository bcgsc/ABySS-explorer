/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.util;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * @author kmnip
 */
public class FastaFileFilter implements FileFilter {
    public boolean accept(File f) {
        String name = f.getName();
        if(name.endsWith(".fa") )
            return true;

        return false;
    }
}
