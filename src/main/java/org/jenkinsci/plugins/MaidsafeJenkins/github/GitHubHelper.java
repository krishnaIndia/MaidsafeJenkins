/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.MaidsafeJenkins.github;

import hudson.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import org.jenkinsci.plugins.MaidsafeJenkins.actions.GithubCheckoutAction;
import org.jenkinsci.plugins.MaidsafeJenkins.util.ShellScript;

/**
 *
 * @author krishnakumarp
 */
public class GitHubHelper {

	private PrintStream consoleLogger;
	private FilePath superProject; // may be it can be needed in future - not used as of now
	private ShellScript script;
	private HashMap<String, String> modulePathMapping;
	private String defaultBaseBranch = "master";
	private GithubCheckoutAction checkoutAction;
	private final String SUM_MODULE_INIT_CMD = "git submodule init";
	private final String SUB_MODULE_UPDATE_CMD = "git submodule foreach 'git checkout %s && git pull'";
	private final String SUBMOD_GREP_CMD = "git config --list | sed -rn 's/submodule\\.([^.]*).*\\/(.*)/\\1,\\2/p'";
	private final String SUPER_PROJ_UPDATE_CMD = "git checkout %s && git pull";
	private final String HARD_RESET_CMD = "git reset --hard HEAD && git submodule foreach 'git reset --hard HEAD'";
	private String accessToken;	

	public GitHubHelper(String superProjectName, FilePath superProject, PrintStream consoleLogger, ShellScript script,
			String defaultBaseBranch, GithubCheckoutAction checkoutAction) {
		this.superProject = superProject;
		this.consoleLogger = consoleLogger;
		this.script = script;
		this.checkoutAction = checkoutAction;
		if (defaultBaseBranch != null && !defaultBaseBranch.isEmpty()) {
			this.defaultBaseBranch = defaultBaseBranch;
		}
		updateSubModuleConfig();
		modulePathMapping.put(superProjectName, ".");
	}

	private void updateSubModuleConfig() {
		String temp;
		String[] splittedArray;
		Scanner scanner;
		try {
			final StringBuilder submodulesOutput = new StringBuilder();
			List<String> commands = new ArrayList<String>();
			commands.add(SUM_MODULE_INIT_CMD);
			commands.add(SUBMOD_GREP_CMD);
			// Creating a temporary output stream to get the execution data
			// Would be cheaper that to pipe it to a file and read it later
			OutputStream outStream = new OutputStream() {

				@Override
				public void write(int b) throws IOException {					
					submodulesOutput.append((char) b);
				}
				
			};
			script.execute(commands, outStream); 
			scanner = new Scanner(submodulesOutput.toString());
			modulePathMapping = new HashMap<String, String>();			
			while (scanner.hasNextLine()) {				
				temp = scanner.nextLine();		
				splittedArray = temp.split(",");
				if (splittedArray.length != 2 || splittedArray[0].contains("git config --list")) {
					continue;
				}				
				modulePathMapping.put(splittedArray[1].trim().toLowerCase(), splittedArray[0].trim());
			}
			consoleLogger.println(modulePathMapping.size() + " sub modules were found");
		} catch (Exception ex) {
			consoleLogger.println(ex);
		}
	}
	
	public void setAccessToken(String token) {
		accessToken = token;
	}
	
	private void doHardReset() {
		try {
			List<String> commands = new ArrayList<String>();
			commands.add(HARD_RESET_CMD);
			script.execute(commands);
		} catch(Exception e) {
			consoleLogger.println(e);
		}
	}


	@SuppressWarnings("unchecked")
	public GithubCheckoutAction checkoutModules(Map<String, Map<String, Object>> prList) throws Exception {
		int scriptExecutionStatus;
		String temp = null;		
		Map<String, Object> pullRequest;
		List<String> command = new ArrayList<String>();		
		command.add(String.format(SUPER_PROJ_UPDATE_CMD, defaultBaseBranch));
		command.add(String.format(SUB_MODULE_UPDATE_CMD, defaultBaseBranch));
		scriptExecutionStatus = script.execute(command);
		consoleLogger.println("Execution status  ::: " + scriptExecutionStatus);
		if (scriptExecutionStatus != 0) {
			doHardReset();
			throw new Exception("Checking out modules to the latest " + defaultBaseBranch + " failed. Check the logs");
		}
		consoleLogger.println("Super project and Sub modules were checked out to the " +
				defaultBaseBranch + " branch with the status " + scriptExecutionStatus);
		if (prList == null || prList.isEmpty()) {			
			return checkoutAction;
		}
		Iterator<String> prModules = prList.keySet().iterator();
		while (prModules.hasNext()) {
			command = new ArrayList<String>();
			temp = prModules.next();
			if (!modulePathMapping.containsKey(temp)) {
				consoleLogger.println("ERROR :: " + temp + " could not be found. ");
			}
			pullRequest = prList.get(temp);
			command.add("cd " + modulePathMapping.get(temp));
			command.addAll(buildPRMergeCommands(pullRequest));			
			scriptExecutionStatus = script.execute(command);
			if (scriptExecutionStatus != 0) {
				doHardReset();
				checkoutAction.setBuildPassed(false);
				checkoutAction.setReasonForFailure("Merge from remote branch " + getBaseBranchNameFromPR(pullRequest) + " with local branch " +
						getRemoteBranchNameToMerge(pullRequest) + " has encountered conflicts in module - " + temp);				
			}
			checkoutAction.addBranchUsedByModule(temp, getBaseBranchNameFromPR(pullRequest));
		}					
		checkoutAction.setBranchTarget(((Map<String, Object>) prList.get(temp).get("head")).get("ref").toString());	
		return checkoutAction;
	}
	
	private String getRemoteBranchNameToMerge(Map<String, Object> pullRequest) {
		return ((Map) pullRequest.get("base")).get("ref").toString();
	}
	
	private String getBaseBranchNameFromPR(Map<String, Object> pullRequest) {		
		return ((Map<String, Object>) pullRequest.get("head")).get("ref").toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> buildPRMergeCommands(Map<String, Object> pullRequest) {
		List<String> mergeCommand = new ArrayList<String>();		
		String localBranch = getBaseBranchNameFromPR(pullRequest);
		String baseBranch = getRemoteBranchNameToMerge(pullRequest);
		String pullRemoteSSHUrl = ( (Map) ((Map<String, Object>) pullRequest.get("head")).get("repo"))
				.get("ssh_url").toString();
		mergeCommand.add("git checkout -b " + localBranch + " " + baseBranch);
		mergeCommand.add("git pull " + pullRemoteSSHUrl + " " + localBranch);
		return mergeCommand;
	}

	public List<String> getModuleNames() {
		List<String> moduleNames = new ArrayList<String>();
		Iterator<String> keysIterator = modulePathMapping.keySet().iterator();
		while (keysIterator.hasNext()) {
			moduleNames.add(keysIterator.next());
		}
		return moduleNames;
	}

	public String getSubModulePath(String submoduleName) {
		if (submoduleName == null || submoduleName.isEmpty()) {
			return null;
		}
		return modulePathMapping.get(submoduleName.toLowerCase());
	}

}
