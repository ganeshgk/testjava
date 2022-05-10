package itmp.allianz.abs.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class FileUtils {

	String application;
	String applicationDirectoryPath;
	String applicationParentDirName;
	File applicationTargetFile;
	File applicationProductFile;
	File applicationDockerFile;
	Log mavenLog;

	public HashMap<String, File> FileUtils(String application, String artifactId, Log mavenLog) {
		// TODO Auto-generated constructor stub
		this.application = application;
		this.mavenLog = mavenLog;

		HashMap<String, File> applicationFileMapping = new HashMap<>();

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		this.applicationParentDirName = artifactId.replace("core", application.replace("-", "."));

		if (application.equals("cisl-adapter")) {

			String[] cislFolderName = application.split("-");

			this.applicationDirectoryPath = currentPath + File.separator +  application.replace("-", File.separator) + File.separator;
			//this.applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
			//		+ application.replace("-", File.separator) + File.separator;
		} else {
			this.applicationDirectoryPath = currentPath + File.separator + application + File.separator;
		}

		this.applicationTargetFile = new File(applicationDirectoryPath + applicationParentDirName + File.separator
				+ applicationParentDirName + ".target");
		this.applicationProductFile = new File(applicationDirectoryPath + applicationParentDirName + ".product"
				+ File.separator + applicationParentDirName + ".product");
		this.applicationDockerFile = new File(
				applicationDirectoryPath + applicationParentDirName + ".docker" + File.separator + "Dockerfile");
		applicationFileMapping.put("target", this.applicationTargetFile);
		applicationFileMapping.put("product", this.applicationProductFile);
		applicationFileMapping.put("docker", this.applicationDockerFile);

		mavenLog.info("Current relative path is: " + currentPath);
		mavenLog.info("Application Directory: " + this.applicationDirectoryPath);
		mavenLog.info("Application Target File Location: " + applicationTargetFile.getAbsolutePath());
		mavenLog.info("Application Product File Location: " + applicationProductFile.getAbsolutePath());
		mavenLog.info("Application Docker File Location: " + applicationDockerFile.getAbsolutePath());

		return applicationFileMapping;

	}

	public List<String> getTargetP2FromTargetFile() throws ParserConfigurationException, SAXException, IOException {
		File targetFile = applicationTargetFile;

		List<String> targetP2s = new ArrayList<String>();

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(targetFile);

		doc.getDocumentElement().normalize();

		Element root = doc.getDocumentElement();

		NodeList nList = doc.getElementsByTagName("repository");

		mavenLog.info("Repos configured in " + targetFile.getAbsolutePath());

		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node node = nList.item(temp);

			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) node;
				String targetRepoUrls = eElement.getAttribute("location").toString().trim();
				mavenLog.debug("Target URLS:: " + targetRepoUrls);
				if (targetRepoUrls != null) {
					targetP2s.add(eElement.getAttribute("location").toString());
				} else {
					mavenLog.error("URL returned emty values");
				}
			}
		}

		mavenLog.info(targetP2s.toString());
		return targetP2s;

	}

}
