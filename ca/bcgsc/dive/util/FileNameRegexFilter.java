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
import java.util.regex.Pattern;

public class FileNameRegexFilter implements FilenameFilter{
    private Pattern regex = Pattern.compile(".*");;

    public FileNameRegexFilter(String regex) {
        super();
        this.regex = Pattern.compile(regex);
    }
    
    public FileNameRegexFilter(Pattern regex) {
        super();
        this.regex = regex;
    }

    public boolean accept(File file, String string) {
        return regex.matcher(string).matches();
    }
}

