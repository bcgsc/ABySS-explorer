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
public class FastaAndAdjFileFilter implements FileFilter {
    public boolean accept(File f) {
        String name = f.getName();
        return f.isFile() && (name.endsWith(".fa") || name.endsWith(".adj") || name.endsWith(".dot"));
    }

}