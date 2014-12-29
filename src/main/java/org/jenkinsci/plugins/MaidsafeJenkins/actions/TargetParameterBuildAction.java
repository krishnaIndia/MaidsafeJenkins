package org.jenkinsci.plugins.MaidsafeJenkins.actions;

import java.util.List;

import hudson.Functions;
import hudson.model.Action;

public class TargetParameterBuildAction implements Action {	
	private final String DISPLAY_NAME = "Parameters";
	private final String URL = "branchesUsed";
	private List<BuildTargetParameter> parameters;
	
	public String getIconFileName() {	
		return Functions.getResourcePath() + "/plugin/MaidsafeJenkins/icons/octocat.jpg";
	}

	public String getDisplayName() { 
		return DISPLAY_NAME;
	}

	public String getUrlName() {
		return URL;
	}

	/**
	 * @return the parameters
	 */
	public List<BuildTargetParameter> getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(List<BuildTargetParameter> parameters) {
		this.parameters = parameters;
	}
	
}
