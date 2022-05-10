package itmp.allianz.abs.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.unix4j.Unix4j;
import org.unix4j.line.Line;
import org.unix4j.unix.Grep;
import org.unix4j.unix.Sed;

import itmp.allianz.abs.data.DataCollector;

public class FileUpdater {

	Log log;

	public FileUpdater(Log log) {
		this.log = log;
	}

	public void updateCorePomFile(File corePomFile, HashMap<String, String> p2VersionMap)
			throws MojoExecutionException {

		Path path = Paths.get(corePomFile.getAbsolutePath());

		Charset charset = StandardCharsets.UTF_8;

		String content;
		try {
			content = new String(Files.readAllBytes(path), charset);
		} catch (IOException e) {
			log.error("File: " + corePomFile + " not found!!! \n" + e);
			throw new MojoExecutionException("File: " + corePomFile + " not found!!! \n" + e);
		}
		boolean isNull = false;
		Iterator it = p2VersionMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry) it.next();
			if (pair.getValue() == null) {
				isNull = true;
			}

			log.debug(pair.getKey() + " = " + pair.getValue());
			content = content.replaceAll("(<" + pair.getKey() + ">)" + "[^&]*" + "(</" + pair.getKey() + ">)",
					"<" + pair.getKey() + ">" + pair.getValue() + "</" + pair.getKey() + ">");
			it.remove(); // avoids a ConcurrentModificationException
		}

		try {
			if (!isNull) {
				Files.write(path, content.getBytes(charset));
			} else {
				throw new MojoExecutionException("Check whether the target definition is available with new delivery");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("File write on  " + corePomFile + " failed!!! \n" + e);
			throw new MojoExecutionException("File write on  " + corePomFile + " failed!!! \n" + e);
		}
		// System.out.println(content);

	}

	public void updateTargetFile(File targetFile, HashMap<String, List<String>> repoUnitIdsMap)
			throws MojoExecutionException {

		String finalTargetFile = new String();

		Path path = Paths.get(targetFile.getAbsolutePath());

		Charset charset = StandardCharsets.UTF_8;

		String content;
		try {
			content = new String(Files.readAllBytes(path), charset);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new MojoExecutionException("File: " + targetFile + " not found!!!" + e);

		}

		String locationStartTag = "<location includeAllPlatforms=\"true\" includeConfigurePhase=\"false\" includeMode=\"slicer\" includeSource=\"true\" type=\"InstallableUnit\">";
		String locationEndTag = "</location>";
		// finalTargetFile = finalTargetFile + locationStartTag;

		String locations = new String();
		log.debug("***********************************************");
		for (Map.Entry<String, List<String>> entry : repoUnitIdsMap.entrySet()) {

			String unitIDTag = new String();

			for (String unitID : entry.getValue()) {
				unitIDTag = unitIDTag + "\n\t" + unitID;
			}

			String repositoryTag = "\n\t<repository location=\"" + entry.getKey() + "\"/>\r\n";

			locations = locations + "\n" + locationStartTag + "\n" + unitIDTag + "\n" + repositoryTag + "\n"
					+ locationEndTag;
			// System.out.println(locations);

		}
		finalTargetFile = locations;

		content = content.replaceAll("(<locations>)" + "[^&]*" + "(</locations>)",
				"<locations>" + "\n\t" + finalTargetFile + "\n" + "</locations>");

		log.debug("***********************************************");
		log.debug(finalTargetFile);
		try {
			Files.write(path, content.getBytes(charset));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("File write on  " + targetFile + " failed!!! \n" + e);
			throw new MojoExecutionException("File write on  " + targetFile + " failed!!! \n" + e);
		}
		log.debug("***********************************************");

	}

	public HashMap<String, String> getTargetPlatforms(File targetFile, Properties mavenProp, Properties pomProp)
			throws MojoExecutionException {

		FileReader fRead = null;
		try {
			fRead = new FileReader(targetFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			log.error("Unable to Read  " + targetFile + "\n" + e);
			throw new MojoExecutionException("Unable to Read  " + targetFile + "\n" + e);
		}
		BufferedReader bRead = new BufferedReader(fRead);

		ArrayList<String> targetPlatformList = new ArrayList<>();

		String currentLine;
		try {
			while ((currentLine = bRead.readLine()) != null) {
				if (currentLine.contains("repository location=")) {

					String target = StringUtils.substringBetween(currentLine, "\"", "\"");
					String targetPlatform[];
					String artifactIdExtracted = null;

					if (target.startsWith("http")) {
						target = target.endsWith("/") ? target.substring(0, target.length() - 1) : target;
						target = target.substring(target.lastIndexOf("/") + 1).trim();
						targetPlatform = target.split("-");
						artifactIdExtracted = targetPlatform[0];
					} else if (target.startsWith("mvn:")) {
						targetPlatform = target.split(":");
						artifactIdExtracted = targetPlatform[2];
					}

					// System.out.println(targetPlatform[0]);
					targetPlatformList.add(artifactIdExtracted);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("Failed to readline inside  " + targetFile + "\n" + e);
			throw new MojoExecutionException("Failed to readline inside  " + targetFile + "\n" + e);
		}
		DataCollector collector = new DataCollector();

		return collector.getP2Mapping(targetPlatformList, mavenProp, pomProp);

	}

	public void updateDockerFile(ArrayList<File> dockerFiles, HashMap<String, String> changedTargetVersionPair)
			throws MojoExecutionException {
		String ABS_MATH_LIBSTDCPP_VERSION_NUMBER = "at.allianz.abs.cpp.math.libstdcpp";
		String ABSSDK_VERSION_NUMBER = "abs-sdk";

		HashMap<String, String> propertyNameMap = new HashMap<>();
		propertyNameMap.put("ABS_MATH_LIBSTDCPP_VERSION_NUMBER", "at.allianz.abs.cpp.math.libstdcpp");
		propertyNameMap.put("ABSSDK_VERSION_NUMBER", "abs-sdk");
		System.out.println(propertyNameMap);

		for (File dockerFile : dockerFiles) {

			List<Line> lines = Unix4j.grep("ENV", dockerFile).toLineList();
			System.out.println(dockerFile);
			System.out.println(lines);
			for (Line line : lines) {
				try {
					String content = IOUtils.toString(new FileInputStream(dockerFile), "UTF-8");
					for (Map.Entry<String, String> entry : propertyNameMap.entrySet()) {
						if (line.toString().contains(entry.getKey())) {
							if (changedTargetVersionPair.get(entry.getValue()) != null) {
								content = content.replace(line, "ENV " + entry.getKey() + "=" + "\""
										+ changedTargetVersionPair.get(entry.getValue()) + "\"\n");
								System.out.println(
										"Updating version of" + entry.getKey() + " in Docker file " + dockerFile);
								IOUtils.write(content, new FileOutputStream(dockerFile), "UTF-8");
							}
						}

					}
				} catch (Exception e) {
					log.error("Failed to update docker File \n" + e);
					throw new MojoExecutionException("Failed to update docker File \n" + e);
				}
			}
		}

	}

	public void updateoeDockerFile(ArrayList<File> dockerFiles, String newVersion, String application)
			throws MojoExecutionException {

		String dockerPrefix = "FROM";
		for (File dockerFile : dockerFiles) {

			// List<Line> lines = Unix4j.grep("^FROM", dockerFile).toLineList();
			String lines = Unix4j.grep("^FROM", dockerFile).toStringResult();
			String linesReplaced = Unix4j.grep("^FROM", dockerFile).sed(Sed.Options.s, "[^:]+$", newVersion)
					.toStringResult();
			StringBuilder linesReplacedN = new StringBuilder(linesReplaced);
			String linesReplacedEnd = linesReplacedN.append(System.lineSeparator()).toString();
			System.out.println(dockerFile);
			System.out.println(lines);
			System.out.println(linesReplaced);
			System.out.println("With Line Ending " + linesReplacedEnd);

			// for (Line line : lines)
			try {
				String content = IOUtils.toString(new FileInputStream(dockerFile), "UTF-8");

				if (lines.startsWith(dockerPrefix) && lines.contains("itmp")) {
					// if (lines.startsWith(dockerPrefix) && lines.contains("itmp")) {
					content = content.replace(lines, linesReplaced);
					System.out.println("Updating version of " + application + " in Docker file " + dockerFile);
					IOUtils.write(content, new FileOutputStream(dockerFile), "UTF-8");
					// }
				}

				// }
			} catch (Exception e) {
				log.error("Failed to update docker File \n" + e);
				throw new MojoExecutionException("Failed to update docker File \n" + e);
			}
		}
	}

}
//}
