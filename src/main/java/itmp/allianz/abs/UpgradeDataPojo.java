package itmp.allianz.abs;

import java.util.List;

public class UpgradeDataPojo {

	String repoID;
	String newVersion;

	String repoVerPlaceholder;

	String repoUrl;

	List<String> UnitIDList;

	public String getRepoID() {
		return repoID;
	}

	public void setRepoID(String repoID) {
		this.repoID = repoID;
	}

	public String getNewVersion() {
		return newVersion;
	}

	public void setNewVersion(String newVersion) {
		this.newVersion = newVersion;
	}

	public String getRepoVerPlaceholder() {
		return repoVerPlaceholder;
	}

	public void setRepoVerPlaceholder(String repoVerPlaceholder) {
		this.repoVerPlaceholder = repoVerPlaceholder;
	}

	public String getRepoUrl() {
		return repoUrl;
	}

	public void setRepoUrl(String repoUrl) {
		this.repoUrl = repoUrl + "/artifacts.jar";
	}

	public List<String> getUnitIDList() {
		return UnitIDList;
	}

	public void setUnitIDList(List<String> unitIDList) {
		UnitIDList = unitIDList;
	}

}
