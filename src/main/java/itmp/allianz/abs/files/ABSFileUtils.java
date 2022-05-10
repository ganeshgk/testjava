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

public class ABSFileUtils {

	public String application;
	public String applicationDirectoryPath;
	public String applicationParentDirName;
	public File applicationTargetFile;
	public File applicationProductFile;
	Log mavenLog;

	public ABSFileUtils(String application, String artifactId, Log mavenLog) {
		// TODO Auto-generated constructor stub
		this.application = application;
		this.mavenLog = mavenLog;

		Path currentRelativePath = Paths.get("");
		String currentPath = currentRelativePath.toAbsolutePath().toString();

		this.applicationParentDirName = artifactId.replace("core", application.replace("-", "."));

		if (application.equals("cisl-adapter")) {

			String[] cislFolderName = application.split("-");

			this.applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
					+ application.replace("-", File.separator) + File.separator;
		} else {
			this.applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
					+ application + File.separator;
		}

		this.applicationTargetFile = new File(applicationDirectoryPath + applicationParentDirName + File.separator
				+ applicationParentDirName + ".target");
		this.applicationProductFile = new File(applicationDirectoryPath + applicationParentDirName + ".product"
				+ File.separator + applicationParentDirName + ".product");

		mavenLog.info("Current relative path is: " + currentPath);
		mavenLog.info("Application Directory: " + this.applicationDirectoryPath);
		mavenLog.info("Application Target File Location: " + applicationTargetFile.getAbsolutePath());
		mavenLog.info("Application Product File Location: " + applicationProductFile.getAbsolutePath());

	}

	public List<String> getTargetP2FromTargetFile() throws SAXException, IOException, ParserConfigurationException {
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
					String targetP2Url = eElement.getAttribute("location").toString();
					if (targetP2Url.endsWith("/")) {
						targetP2Url = targetP2Url.substring(0, targetP2Url.length() - 1);
					}
					targetP2Url = targetP2Url.substring(targetP2Url.lastIndexOf("/") + 1).trim();
					targetP2s.add(targetP2Url.split("-")[0]);
				} else {
					mavenLog.error("URL returned emty values");
				}
			}
		}

		mavenLog.info(targetP2s.toString());
		return targetP2s;

	}

}
