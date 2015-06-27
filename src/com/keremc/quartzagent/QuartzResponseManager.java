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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class QuartzResponseManager {


    private String status = "OK";
    private int code = 0;
    private Set<Job> jobs = new HashSet<Job>();

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

        ArrayList<String> header = new ArrayList<String>();
        String message = "";

        for (Job j : jobs) {

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String data = dateFormat.format(date);

            if (option == Option.ERROR_EXISTENCE) {
                File errorFolder = new File(j.getPath() + File.separator + File.separator + "Errors" + File.separator + data);
                System.out.println(errorFolder.getAbsolutePath());

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

            }
        }

        if (header.isEmpty()) {
            header.add("All jobs running properly");
        }

        String data = header.toString().substring(1, header.toString().length() - 1);


        return code + "|" + data + "|" + message;
    }

}
