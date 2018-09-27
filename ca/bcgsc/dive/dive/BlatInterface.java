package ca.bcgsc.dive.dive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTE: This class relies on the (structure of) HTML code of the UCSC Blat web interface
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */
public class BlatInterface {

    private HashMap<String, HashMap<String,String>> orgToDbsMap = null;
    private HashMap<String,String> assemblyToDbsMap = null;

    private static final String BASE = "http://genome.ucsc.edu/cgi-bin/hgBlat?"; // base address of UCSC Blat
    private static final Pattern PATTERN = Pattern.compile("<OPTION.*VALUE=\"(.*)\">(.*)</OPTION>");

    /**
     * Go to UCSC Blat and get the list of organisms by looking through the HTML code.
     */
    public Set<String> listAllOrgs(){
        if(orgToDbsMap == null){
            orgToDbsMap = new HashMap<String, HashMap<String,String>>(); // KEY: name of the organism; VALUE: a map between the names of assembly of the organism and the corresponding database

            try {
                URL address = new URL(BASE);
                BufferedReader in = new BufferedReader(new InputStreamReader(address.openStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    if(line.startsWith("<SELECT NAME=\"org\"")){
                        while ((line = in.readLine()) != null && !line.equals("</SELECT>")) {
                            Matcher m = PATTERN.matcher(line);
                            if(m.matches()){
                                orgToDbsMap.put(m.group(1), null); // no value for now
                            }
                        }
                    }
                }
                in.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return orgToDbsMap.keySet();
    }

    /**
     * Select an organism and get the list of assemblies by looking through the HTML code.
     */
    public Set<String> selectOrg(String org){
        assemblyToDbsMap = orgToDbsMap.get(org); // KEY: name of assembly; VALUE: name of corresponding database

        if(assemblyToDbsMap == null || assemblyToDbsMap.size() == 0){
            assemblyToDbsMap = new HashMap<String, String>();
            
            try {
                URL address = new URL(BASE + "org=" + java.net.URLEncoder.encode(org, "UTF-8"));
                BufferedReader in = new BufferedReader(new InputStreamReader(address.openStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    if(line.startsWith("<SELECT NAME=\"db\">")){
                        while ((line = in.readLine()) != null && !line.equals("</SELECT>")) {
                            Matcher m = PATTERN.matcher(line);
                            if(m.matches()){
                                assemblyToDbsMap.put(m.group(2), m.group(1));
                            }
                        }
                    }
                }
                in.close();
                orgToDbsMap.put(org, assemblyToDbsMap);
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return assemblyToDbsMap.keySet();
    }

    public String getDatabaseName(String assembly){
        return assemblyToDbsMap.get(assembly);
    }
}
