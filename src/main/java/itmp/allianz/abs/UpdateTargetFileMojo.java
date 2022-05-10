package itmp.allianz.abs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import itmp.allianz.abs.files.FileUpdater;
import itmp.allianz.abs.repo.BinaryRepoUtil;

@Mojo(name = "updateTargetFile", defaultPhase = LifecyclePhase.COMPILE)
public class UpdateTargetFileMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	MavenProject project;

	@Parameter(defaultValue = "${localRepository}", readonly = true)
	private ArtifactRepository localRepository;

	@Parameter(property = "remoteRepositories")
	private String remoteRepositories;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
	private List<ArtifactRepository> artifactRemoteRepositories;

	@Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}", required = true)
	private File outputDirectory;

	@Parameter(property = "application")
	private String application;

	@Parameter(property = "nexus.host", defaultValue = "${nexus.host}")
	private String nexusHost;

	private List<String> applicationTargetFile;

	private Log log;

	public Log getLog() {

		log = super.getLog();

		return this.log;
	}

	private String baseDir;

	private String groupID;

	public String getGroupID() {
		return groupID;
	}

	public void setGroupID(String groupID) {
		this.groupID = groupID;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub

		getLog().info("Executing updateTargetFile" + application);
		List<String> applicationList = new ArrayList<String>();
		getLog().info("Using nexus host: " + nexusHost);
		setBaseDir(project.getBasedir().toString());
		setGroupID(project.getGroupId());

		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model = null;
		try {
			model = reader.read(new FileReader(project.getFile()));
		} catch (IOException | XmlPullParserException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Properties mavenProp = project.getProperties();
		Properties pomProp = model.getProperties();

		if (project.getArtifactId().equals(project.getGroupId() + ".core")) {

			if (application == null) {
				getLog().info("Getting the application from profile ID ");
				for (Profile profile : model.getProfiles()) {
					/*
					 * if (!(profile.getId().toString().contains("-tp") ||
					 * profile.getId().toString().equals("version-upgrade") ||
					 * profile.getId().toString().equals("pdm") ||
					 * profile.getId().toString().equals("soa") ||
					 * profile.getId().toString().equals("absvoc"))) {
					 */

					applicationList.add(profile.getId().toString());

					// }
				}

			} else {
				applicationList.add(application);

			}

			getLog().info("Target files will be changed for: " + applicationList.toString());
			setApplicationTargetFile(applicationList);

			for (String targetFilePath : getApplicationTargetFile()) {
				File targetFile = new File(targetFilePath);
				getLog().info("Processing " + targetFile.getAbsolutePath());
				HashMap<String, List<String>> repoUnitIdsMap = new HashMap<>();
				FileUpdater updater = new FileUpdater(getLog());
				getLog().info(
						"Changed Target Platform list" + updater.getTargetPlatforms(targetFile, mavenProp, pomProp));

				for (Map.Entry<String, String> entry : updater.getTargetPlatforms(targetFile, mavenProp, pomProp)
						.entrySet()) {

					getLog().info("Gathering Unit IDs for p2: " + entry.getKey() + " using: " + entry.getValue());
					String downloadEndPoint = entry.getValue();
					downloadEndPoint = StringUtils.removeEnd(downloadEndPoint, "/");
					BinaryRepoUtil repoUtil = new BinaryRepoUtil(getLog(), downloadEndPoint, localRepository,
							remoteRepositories, artifactRemoteRepositories);
					List<String> p2IuList = new ArrayList<>();

					p2IuList = repoUtil.getP2IuList(outputDirectory);

					repoUnitIdsMap.put(downloadEndPoint, p2IuList);

				}
				FileUpdater updateTargetFile = new FileUpdater(log);
				getLog().info("Updating the target files");
				updateTargetFile.updateTargetFile(targetFile, repoUnitIdsMap);
			}
		}

	}

	public List<String> getApplicationTargetFile() {
		return applicationTargetFile;
	}

	public void setApplicationTargetFile(List<String> applicationList) {

		String currentPath = getBaseDir();
		String groupID = getGroupID();

		List<String> applicationTargetFileList = new ArrayList<String>();

		for (String application : applicationList) {
			String applicationParentDirName = groupID.concat("." + application.replace("-", "."));
			String applicationDirectoryPath;
			if (application.equals("cisl-adapter")) {

				applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
						+ application.replace("-", File.separator) + File.separator;
			} else {
				applicationDirectoryPath = currentPath + File.separator + ".." + File.separator + ".." + File.separator
						+ application + File.separator;
			}
			applicationDirectoryPath = applicationDirectoryPath + applicationParentDirName + File.separator
					+ applicationParentDirName + ".target";
			if (new File(applicationDirectoryPath).exists()) {
				applicationTargetFileList.add(applicationDirectoryPath);
			}

		}

		applicationTargetFile = applicationTargetFileList;

	}

}
