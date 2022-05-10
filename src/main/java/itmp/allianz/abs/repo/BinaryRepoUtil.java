package itmp.allianz.abs.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import itmp.allianz.abs.mvn.ArtifactDownload;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BinaryRepoUtil {

	String nexusHost;
	String repoUrl;
	String repoName;
	String version;

	String mvnGroupID;
	String mvnArtifactID;
	String mvnVersion;
	String mvnPackaging;
	String mvnClassifier;

	ArtifactRepository localRepository;
	String remoteRepositories;
	List<ArtifactRepository> artifactRemoteRepositories;

	URL p2Url = null;
	URLConnection repoUrlConnection;
	org.jsoup.nodes.Document doc;

	Log log;

	public BinaryRepoUtil(String nexusHost, String version) throws MojoExecutionException {

		this.version = version;

		this.repoName = "abs-core-deliveries";
		this.repoUrl = nexusHost + "/repository/" + this.repoName + "/at/allianz/abs/delivery/delivery_report/"
				+ version.replace("-alpha.", ".alpha-") + "/delivery_report.txt";
		try {
			this.doc = Jsoup.connect(this.repoUrl).timeout(400 * 1000).get();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new MojoExecutionException("Host URL not avilable  " + repoUrl + " !!! \n" + e);
		}

	}

	public BinaryRepoUtil(Log log, String downloadEndPoint, ArtifactRepository localRepository,
			String remoteRepositories, List<ArtifactRepository> artifactRemoteRepositories)
			throws MojoExecutionException {
		this.log = log;

		try {
			if (downloadEndPoint.startsWith("http")) {
				this.p2Url = new URL(downloadEndPoint + "/artifacts.jar");
				this.log.info("Using URL format: " + this.p2Url);
			} else if (downloadEndPoint.startsWith("mvn")) {
				String mvnCoordinates[] = downloadEndPoint.split(":");
				this.mvnGroupID = mvnCoordinates[1];
				this.mvnArtifactID = mvnCoordinates[2];
				this.mvnVersion = mvnCoordinates[3];
				this.mvnClassifier = "";
				this.mvnPackaging = mvnCoordinates[4];
				this.localRepository = localRepository;
				this.remoteRepositories = remoteRepositories;
				this.artifactRemoteRepositories = artifactRemoteRepositories;
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			throw new MojoExecutionException("Malformed P2 url  " + this.p2Url + " !!! \n" + e);
		}

		// System.out.println(this.repoUrlConnection.getContentLength());

	}

	public HashMap<String, String> getAlkulthuTargetP2List() {
		String text = doc.body().text().replace("Ok", "\n");
		text = text.replaceAll("(?m)^ : *", "");
		text = text.substring(text.indexOf('\n') + 1);
		String deliveryOldLines[] = text.split("\\r?\\n");
		HashMap<String, String> targetVersionPairList = new HashMap<>();
		for (int i = 0; i < deliveryOldLines.length; i++) {
			if (!deliveryOldLines[i].contains(this.version)) {
				String targetVersionPair[] = deliveryOldLines[i].split(":");
				targetVersionPairList.put(targetVersionPair[1].trim(), targetVersionPair[2].trim());
				// System.out.println(deliveryOldLines[i].trim());
			}
		}
		System.out.println(targetVersionPairList);
		return targetVersionPairList;

	}

	public HashMap<String, String> getChangedAlkulthuTargetP2List(HashMap<String, String> oldReport,
			HashMap<String, String> newReport) {
		HashMap<String, String> changedTargetVersionPair = new HashMap<>();
		Iterator it = oldReport.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			String oldKey = (String) pair.getKey();
			if (!pair.getValue().equals(newReport.get(oldKey))) {
				// System.out.println(pair.getKey() + "::" +
				// newReport.get(oldKey));
				changedTargetVersionPair.put((String) pair.getKey(), newReport.get(oldKey));
			}
			it.remove();
		}

		return changedTargetVersionPair;
	}

	public List<String> getP2IuList(File downloadDirectory) throws MojoExecutionException {

		/*
		 * try { FileUtils.copyURLToFile(this.p2Url, new File(downloadDirectory +
		 * "/artifacts.jar"), 400000, 400000); } catch (IOException e1) { // TODO
		 * Auto-generated catch block throw new
		 * MojoExecutionException("Copying file from URL  " + p2Url +
		 * " failed inside folder: " + downloadDirectory.getAbsolutePath() + " !!! \n" +
		 * e1); }
		 */
		ZipFile artifactJar=null;
		try {
			if (this.p2Url != null) {
				artifactJar = new ZipFile(this.tryConnection(downloadDirectory.getAbsolutePath()));
			} else if (this.mvnGroupID != null) {
				artifactJar = new ZipFile(this.tryMavenDownload(downloadDirectory.getAbsolutePath()));

			}
		} catch (IOException e2) {
			throw new MojoExecutionException("File: " + downloadDirectory + "/artifacts.jar not found!!! \n" + e2);

		}
		List<String> p2IuList = null;

		for (Enumeration e = artifactJar.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();

			if (!entry.isDirectory()) {
//				System.out.println(entry.getName());
				if (entry.getName().equals("artifacts.xml")) {
//					System.out.println(artifactJar.getInputStream(entry));
					List<String> features = new ArrayList<>();
					List<String> plugins = new ArrayList<>();
					BufferedReader reader;
					String line;
					try {
						reader = new BufferedReader(new InputStreamReader(artifactJar.getInputStream(entry)));
						while ((line = reader.readLine()) != null) {
							if (line.contains("<artifact")) {
								line = line.replace(">", "/>");
								line = line.replace("'", "\"");
//								System.out.println(line);
								DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
								DocumentBuilder dBuilder;
								Document doc;
								try {
									dBuilder = dbFactory.newDocumentBuilder();
									doc = dBuilder.parse(new InputSource(new StringReader(line)));

								} catch (Exception e1) {
									// TODO Auto-generated catch block
									throw new MojoExecutionException("Issue processing the line: " + line + " \n" + e1);
								}

								NodeList nodes = doc.getElementsByTagName("artifact");
								for (int temp = 0; temp < nodes.getLength(); temp++) {

									Node node = nodes.item(temp);
									if (node.getNodeType() == Node.ELEMENT_NODE) {
										Element eElement = (Element) node;
//										System.out.println(eElement.getAttribute("classifier").toString().trim());
										String classifier = eElement.getAttribute("classifier").toString().trim();

										if (classifier.equals("org.eclipse.update.feature")) {
											String featureUID = line.trim().replace(
													"<artifact classifier=\"org.eclipse.update.feature\" ", "<unit ");
											featureUID = featureUID.replace("\" version", ".feature.group\" version");
											if (!featureUID.contains("at.allianz.abs.core.rootfiles.feature")) {
												features.add(featureUID);
											}
										} else if (classifier.equals("osgi.bundle")) {
											plugins.add(line.trim().replace("<artifact classifier=\"osgi.bundle\"",
													"<unit "));
										}

										// out.append(line + "\n");
									}
								}
							}
						}

					} catch (IOException ioe) {
						// do something, probably not a text file
						throw new MojoExecutionException("Reading of artifact.xml failed inside folder: "
								+ downloadDirectory.getAbsolutePath() + " !!! \n" + ioe);
					} finally {

						try {
							artifactJar.close();
							FileUtils.forceDelete(new File(downloadDirectory + "/artifacts.jar"));
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							throw new MojoExecutionException(
									"Failed to delete " + downloadDirectory + "/artifacts.jar" + " !!! \n" + e1);
						}
					}
					if (features.isEmpty()) {
						p2IuList = plugins;

					} else {
						p2IuList = features;

					}
					// System.out.println(out);
				} else {
					p2IuList = null;
				}
			}

		}

//		System.out.println(p2IuList);
		return p2IuList;

	}

	File tryConnection(String downloadDirectory) throws MojoExecutionException {
		int retryCounter = 0;
		int maxRetries = 6;

		File artifactsJar = null;
		log.info("Processing p2 url :" + this.p2Url);

		while (retryCounter < maxRetries) {
			log.info("Download Retry Count: " + retryCounter + 1 + " ---> " + this.p2Url);
			try {
				artifactsJar = new File(downloadDirectory + "/artifacts.jar");
				FileUtils.copyURLToFile(this.p2Url, artifactsJar, 400000, 400000);
				return artifactsJar;
			} catch (Exception ex) {
				retryCounter++;
				if (retryCounter >= maxRetries) {
					log.info("Reached maximum retires!!!");
					break;
				}
			}

		}
		throw new MojoExecutionException("Failed to delete " + downloadDirectory + "/artifacts.jar" + " !!! \n");

	}

	File tryMavenDownload(String downloadDirectory) throws MojoExecutionException {
		File artifactsJar = null;
		try {

			log.info("Processing maven p2 artifact- " + this.mvnGroupID + ":" + this.mvnArtifactID + ":"
					+ this.mvnVersion + ":" + this.mvnClassifier + ":" + this.mvnPackaging);

			File artifactZip = ArtifactDownload.getArtifactByAether(this.mvnGroupID, this.mvnArtifactID,
					this.mvnVersion, this.mvnClassifier, this.mvnPackaging, new File(this.localRepository.getBasedir()),
					this.artifactRemoteRepositories);

			ZipFile zipArtifact = new ZipFile(artifactZip);
			InputStream in = zipArtifact.getInputStream(zipArtifact.getEntry("artifacts.jar"));
			artifactsJar = new File(downloadDirectory + "/artifacts.jar");

			FileUtils.copyInputStreamToFile(in, artifactsJar);
			zipArtifact.close();

		} catch (Exception e) {
			throw new MojoExecutionException(
					"Error processing maven p2 artifact- " + this.mvnGroupID + ":" + this.mvnArtifactID + ":"
							+ this.mvnVersion + ":" + this.mvnClassifier + ":" + this.mvnPackaging + e);

		}
		return artifactsJar;

	}

}
