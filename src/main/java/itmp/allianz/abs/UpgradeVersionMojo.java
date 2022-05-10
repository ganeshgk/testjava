package itmp.allianz.abs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import itmp.allianz.abs.files.ABSFileUtils;
import itmp.allianz.abs.repo.BinaryRepoUtil;


@Mojo(name = "upgrade-version", defaultPhase = LifecyclePhase.COMPILE)
public class UpgradeVersionMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.outputDirectory}", required = true)
	private File outputDirectory;

	@Parameter(property = "nexus.host")
	private String nexusHost;

	@Parameter(property = "new.version", required = true)
	private String newVersion;

	@Parameter(property = "check.delivery", defaultValue = "true")
	private Boolean checkDelivery;

	private String existingVersion;

	public String getExistingVersion() {
		return existingVersion;
	}

	public void setExistingVersion(String existingVersion) {
		this.existingVersion = existingVersion;
	}

	@Parameter(property = "nexus.version", defaultValue = "3")
	private String nexusVersion;

	private List<String> profiles;

	private Map<String, String> appUrlPair;

	private Log mavenLog;

	private HashMap<String, String> applicationTargetFileMapping;

	private String client;

	public String getClient() {
		return client;
	}

	public void setClient(String client) {
		this.client = client;
	}

	public HashMap<String, String> getApplicationTargetFileMapping() {
		return applicationTargetFileMapping;
	}

	public void setApplicationTargetFileMapping(HashMap<String, String> applicationTargetFileMapping) {
		this.applicationTargetFileMapping = applicationTargetFileMapping;
	}

	/**
	 * @return Returns the log.
	 */
	public Log getLog() {

		mavenLog = super.getLog();

		return this.mavenLog;
	}

	public List<String> removeDuplicates(List<String> listOfValues) {
		Set<String> hs = new HashSet<>();
		// System.out.println(listOfValues);
		hs.addAll(listOfValues);
		listOfValues.clear();
		listOfValues.addAll(hs);
		return listOfValues;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {

		Log mavenLog = this.getLog();

//		System.out.println(mavenProperties);
//		mavenProperties.setProperty("itmp.adapter.version", "100");
//		System.out.println(project.getProperties());
		// List<String> activeProfiles = Arrays.asList("rap", "cisl", "configsuite",
		// "rcp");
//		project.setActiveProfiles(activeProfiles);
		// System.out.println(project.getActiveProfiles());
		File corePom = project.getFile();
//		System.out.println(corePom.getAbsolutePath());
		profiles = null;

		try {

			MavenPomReader corePomReader = new MavenPomReader(corePom);
			mavenLog.info("ArtifactID:" + corePomReader.model.getArtifactId());
			String artifactId = corePomReader.model.getArtifactId();
			List<String> targetP2List = new ArrayList<>();

			/*
			 * File f = outputDirectory; if (!f.exists()) { f.mkdirs(); } File touch = new
			 * File(f, "mavenProp.properties"); OutputStream output = new
			 * FileOutputStream(touch); mavenProperties.store(output, null);
			 */

			HashMap<String, String> appTargetFileMapping = new HashMap<String, String>();
			if (artifactId.equals("itmp.allianz.abs.core")) {
				setClient(artifactId.split("\\.")[0]);
				Properties mavenProperties = project.getProperties();
				setExistingVersion(mavenProperties.getProperty("abs.core.version"));

				if (nexusHost == null || nexusHost.isEmpty()) {
					mavenProperties.setProperty("nexus.host", nexusHost);
					setNexusHost(mavenProperties.getProperty("nexus.host"));
				}

				for (Profile profile : corePomReader.model.getProfiles()) {

					if (!(profile.getId().toString().contains("-tp")
							|| profile.getId().toString().equals("version-upgrade"))) {
						mavenLog.info(profile.getId());

						if (!profile.getRepositories().isEmpty()) {
							ABSFileUtils file = new ABSFileUtils(profile.getId(), artifactId, mavenLog);
							targetP2List.addAll(file.getTargetP2FromTargetFile());

							appTargetFileMapping.put(profile.getId().toString(),
									file.applicationTargetFile.getAbsolutePath() + "::"
											+ file.applicationProductFile.getAbsolutePath());

							for (Repository repository : profile.getRepositories()) {

								String targetP2Name = repository.getId().toString();

								targetP2Name = StringUtils.substringBetween(targetP2Name, "${", "}");

								// getLog().info(mavenProperties.get(repoVersionPropName).toString() + ":"
								// + mavenProperties.get(repoUrlPropName).toString());
								targetP2List.add(mavenProperties.get(targetP2Name).toString());

//						appUrlMap.put(profile.getId().toString(), mavenProperties.get(repoUrlPropName).toString());
//						setAppUrlPair(appUrlMap);

							}
							// profiles.add(profile.getId().toString());
						}
					} else {
						getLog().info("Execution skipped!!!" + profile.getId().toString() + "!!");

					}
				}
				setApplicationTargetFileMapping(appTargetFileMapping);
			}
			// System.out.println(removeDuplicates(targetP2List));
			System.out.println(getApplicationTargetFileMapping());
			if (getExistingVersion() != null && checkDelivery) {
				/*RepoOperations nexusOperation = new RepoOperations(nexusHost, nexusVersion, getExistingVersion(),
						newVersion, removeDuplicates(targetP2List));
				System.out.println("Changed targetP2: " + nexusOperation.changedTargetVersionPair);*/
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*
			 * catch (IOException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); } catch (XmlPullParserException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */

		// System.out.println(getAppUrlPair());
		/*
		 * List<Dependency> dependencies = project.getDependencies(); long
		 * numDependencies = dependencies.stream().count();
		 * getLog().info("Number of dependencies: " + numDependencies);
		 */
//		System.out.println(getAppUrlPair());
	}

	public String getNexusHost() {
		return nexusHost;
	}

	public void setNexusHost(String nexusHost) {
		this.nexusHost = nexusHost;
	}

	public Map<String, String> getAppUrlPair() {
		return appUrlPair;
	}

	public void setAppUrlPair(Map<String, String> appUrlPair) {
		this.appUrlPair = appUrlPair;
	}
}
