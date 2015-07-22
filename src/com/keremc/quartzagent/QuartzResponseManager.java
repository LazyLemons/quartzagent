package com.keremc.quartzagent;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class QuartzResponseManager {

    private Set<Job> jobs = new HashSet<Job>();

    private static HashMap<String, String> lastBatchProcessedFolderTopNames = new HashMap<String, String>();
    private static HashMap<String, Long> lastFolderReads = new HashMap<String, Long>();

    public void query() {

        try {

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            Document doc = builder.parse(QuartzAgent.activationServletURI);

            NodeList nodes = doc.getElementsByTagName("row");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);

                NodeList images = element.getElementsByTagName("cell");
                String name = images.item(0).getTextContent();

                Element imageElement = (Element) images.item(4);
                String cdataimage = getCharacterDataFromElement(imageElement);

                boolean running = cdataimage.contains("Pause") && cdataimage.contains("Stop");

                Element dirElement = (Element) images.item(6);
                String dirdata = getCharacterDataFromElement(dirElement);

                Document soap = builder.parse(new InputSource(new StringReader(dirdata)));

                Element e = (Element) soap.getElementsByTagName("ns1:inputDirectory").item(0);

                if (e == null) {
                    continue;
                }

                Job j = new Job(running, name, e.getTextContent());
                jobs.add(j);

                System.out.println(j);

            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    public String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    public Set<Job> getJobs() {
        return jobs;
    }

    public String process(Option option) {
        // <id>|<message>|<graphData>
        int code = 0;

        ArrayList<String> header = new ArrayList<String>();
        String message = "";

        for (Job j : jobs) {

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String data = dateFormat.format(date);

            if (option == Option.ERROR_EXISTENCE) {
                File errorFolder = new File(j.getPath() + File.separator + "Errors" + File.separator + data);

                if (!errorFolder.exists()) {
                    continue;
                }

                System.out.println("Error folder for " + j.getName() + ": " + errorFolder.getAbsolutePath());

                int amt = errorFolder.listFiles().length;

                if (amt > 0) {
                    code = 2;
                    header.add(j.getName() + " has " + amt + " error " + (amt == 1 ? "" : "s"));
                }
            } else if (option == Option.RUNNING) {
                if (!j.isRunning()) {
                    code = 2;
                    header.add(j.getName() + " is NOT RUNNING!");
                }
            } else if (option == Option.PROCESS_TIME) {
                File processing = new File(j.getPath());

                for (File f : processing.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".txt");
                    }
                })) {
                    long ageInMinutes = (System.currentTimeMillis() - f.lastModified()) / 1000L / 60L;

                    if (ageInMinutes >= 60) {
                        code = 2;
                    } else if (ageInMinutes >= 15) {
                        if (code != 2) {
                            code = 1;
                        }
                    }
                    header.add(j.getName() + " has queued " + f.getName() + " for " + ageInMinutes + "m");

                }

            } else if (option == Option.CAPTURED_EMAILS) {
                String path = j.getPath();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                String finalFolderName = path.split("/")[path.split("/").length - 1];

                File cap_data = new File(new File(j.getPath()).getParentFile().getParentFile() + File.separator + "cap_data" + File.separator + "commit" + File.separator + finalFolderName);
                System.out.println("Captured email data folder: " + cap_data.toString());
                int files = 0;
                if (cap_data.exists() && cap_data.isDirectory() && cap_data.listFiles().length > 0) {

                    File newest = newestFileInDir(cap_data);

                    if (!lastBatchProcessedFolderTopNames.containsKey(j.getName())) {

                        lastBatchProcessedFolderTopNames.put(j.getName(), newest.getName());
                        lastFolderReads.put(j.getName(), newest.lastModified() + 1);
                    }

                    if (!lastBatchProcessedFolderTopNames.get(j.getName()).equals(newest.getName())) {
                        lastFolderReads.put(j.getName(), new File(cap_data + File.separator + lastBatchProcessedFolderTopNames.get(j.getName())).lastModified());
                        lastBatchProcessedFolderTopNames.put(j.getName(), newest.getName());
                    }

                    for (File f : cap_data.listFiles()) {
                        if (f.lastModified() > lastFolderReads.get(j.getName()) && f.isDirectory()) {
                            files += f.listFiles().length;
                        }
                    }
                    header.add("" + files + " emails have been captured for " + j.getName() + " since " + new SimpleDateFormat().format(new Date(lastFolderReads.get(j.getName()))));

                }

                message += j.getName() + "-emails-captured=" + files+"; ";
            }
        }

        if (header.isEmpty()) {
            header.add("All jobs running properly");
        }

        String data = header.toString().substring(1, header.toString().length() - 1);


        return code + "|" + data + "|" + message;
    }

    public static File newestFileInDir(File file) {
        long la = 0L;
        File a = file.listFiles()[0];
        for (File f : file.listFiles()) {
            if (f.lastModified() > la) {
                la = f.lastModified();
                a = f;
            }
        }
        return a;
    }
}
