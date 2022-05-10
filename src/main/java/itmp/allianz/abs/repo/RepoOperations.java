package itmp.allianz.abs.repo;

import java.io.IOException;
//import java.io.File;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class RepoOperations {

	String nexusHost;
	String nexusVersion;
	String existingVerDeliveryUrl;
	String newVerDeliveryUrl;
	Document docExistingVerDeliveryUrl;
	Document docNewVerDeliveryUrl;

	String repoName = "abs-core-deliveries";

	String existingVersion;
	String newVersion;

	public HashMap<String, String> changedTargetVersionPair;
	public HashMap<String, String> newTargetVersionPair;

	
	public RepoOperations(String nexusHost, String nexusVersion, String existingVersion, String newVersion)
			throws MojoExecutionException, MojoFailureException {
		this.nexusHost = nexusHost;
		this.existingVersion = existingVersion.replace(".alpha-", "-alpha.");
		this.newVersion = newVersion.replace(".alpha-", "-alpha.");

		String repoContext;
		if (Integer.parseInt(nexusVersion) >= 3) {
			repoContext = "/repository/";
		} else {
			repoContext = "/repositories/";
		}

		String existingVerDeliveryUrlTemp = nexusHost + repoContext + this.repoName
				+ "/at/allianz/abs/delivery/delivery_report/" + this.existingVersion + "/delivery_report.txt";

		String newVerDeliveryUrlTemp = nexusHost + repoContext + this.repoName
				+ "/at/allianz/abs/delivery/delivery_report/" + this.newVersion + "/delivery_report.txt";

		try {

			URL existingVerDeliveryUrlCheck = new URL(existingVerDeliveryUrlTemp);
			HttpURLConnection existingVerDeliveryUrlConnection = (HttpURLConnection) existingVerDeliveryUrlCheck
					.openConnection();
			existingVerDeliveryUrlConnection.setRequestMethod("GET");
			existingVerDeliveryUrlConnection.connect();
			int existingVerDeliveryUrlCode = existingVerDeliveryUrlConnection.getResponseCode();
			System.out.println("Response Code:" + existingVerDeliveryUrlCode);

			if (existingVerDeliveryUrlCode == 200) {

				this.existingVerDeliveryUrl = existingVerDeliveryUrlTemp;
			} else {
				this.existingVerDeliveryUrl = nexusHost + repoContext + this.repoName
						+ "/at/allianz/abs/delivery/delivery_report/" + this.existingVersion + "/delivery_report-"
						+ this.existingVersion + ".txt";
			}
			System.out.println("Current Version Delivery Url:" + this.existingVerDeliveryUrl);

			URL newVerDeliveryUrlCheck = new URL(newVerDeliveryUrlTemp);
			HttpURLConnection newVerDeliveryUrlConnection = (HttpURLConnection) newVerDeliveryUrlCheck.openConnection();
			newVerDeliveryUrlConnection.setRequestMethod("GET");
			newVerDeliveryUrlConnection.connect();
			int newVerDeliveryUrlCode = newVerDeliveryUrlConnection.getResponseCode();
			System.out.println("Response Code:" + newVerDeliveryUrlCode);

			if (newVerDeliveryUrlCode == 200) {
				this.newVerDeliveryUrl = newVerDeliveryUrlTemp;
			} else {
				this.newVerDeliveryUrl = nexusHost + repoContext + this.repoName
						+ "/at/allianz/abs/delivery/delivery_report/" + this.newVersion + "/delivery_report-" + this.newVersion
						+ ".txt";
			}
			System.out.println("New Version Delivery Url:" + this.newVerDeliveryUrl);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// this.existingVerDeliveryUrl = nexusHost + repoContext + this.repoName
		// + "/at/allianz/abs/delivery/delivery_report/" + existingVersion +
		// "/delivery_report.txt";

//		this.newVerDeliveryUrl = nexusHost + repoContext + this.repoName + "/at/allianz/abs/delivery/delivery_report/"
//				+ newVersion + "/delivery_report.txt";

		try {
			// System.out.println("*****Contains test inside *****" +
			// Jsoup.connect(this.existingVerDeliveryUrl).timeout(400 * 1000).);
			this.docExistingVerDeliveryUrl = Jsoup.connect(this.existingVerDeliveryUrl).timeout(400 * 1000).get();

			this.docNewVerDeliveryUrl = Jsoup.connect(this.newVerDeliveryUrl).timeout(400 * 1000).get();
			/* For test purposes to load from file
			//***************
			
			File input = new File("C:\\DEV\\new_file.txt");
			this.docExistingVerDeliveryUrl = Jsoup.parse(input, "UTF-8");
             */
			HashMap<String, String> existingTargetVersionPair = new HashMap<>();
			existingTargetVersionPair = getAlkulthuTargetP2List(docExistingVerDeliveryUrl, existingVersion);
			System.out.println("Delivery Report of Existing verison: " + existingTargetVersionPair); 

			HashMap<String, String> newTargetVersionPair = new HashMap<>();
			newTargetVersionPair = getAlkulthuTargetP2List(docNewVerDeliveryUrl, newVersion);
			System.out.println("Delivery Report of new verison: " + newTargetVersionPair);

			this.newTargetVersionPair = newTargetVersionPair;

			this.changedTargetVersionPair = getChangedAlkulthuTargetP2List(existingTargetVersionPair,
					newTargetVersionPair);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HashMap<String, String> getAlkulthuTargetP2List(Document doc, String version) {
		String text = doc.body().text().replace("Ok", "\n");
		text = text.replaceAll("(?m)^ : *", "");
		text = text.substring(text.indexOf('\n') + 1);
		String deliveryOldLines[] = text.split("\\r?\\n");
		HashMap<String, String> targetVersionPairList = new HashMap<>();
		for (int i = 0; i < deliveryOldLines.length; i++) {
			if (!deliveryOldLines[i].contains(version)) {
				String targetVersionPair[] = deliveryOldLines[i].split(":");
				targetVersionPairList.put(targetVersionPair[1].trim(), targetVersionPair[2].trim());
			    // System.out.println(deliveryOldLines[i].trim());
			}
		}
		// System.out.println(targetVersionPairList);
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

				changedTargetVersionPair.put((String) pair.getKey(), newReport.get(oldKey));
			}
			it.remove();
		}

		// HashMap<String, String> changedTargetVersionPairMap;
		/*
		 * for (String key : changedTargetVersionPair.keySet()) { if
		 * (!targetP2List.contains(key)) { changedTargetVersionPair.remove(key); } }
		 */
		// changedTargetVersionPair.keySet().retainAll(targetP2List);
		System.out.println(changedTargetVersionPair);
		return changedTargetVersionPair;
	}

}