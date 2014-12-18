package org.jenkinsci.plugins.MaidsafeJenkins.github;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import jenkins.model.JenkinsLocationConfiguration;


public class CommitStatus {
	private String orgName;
	private String accessToken;
	private final String context = "jenkins-ci";
	private PrintStream logger;	
	private final String END_POINT = "https://api.github.com/repos/%s/%s/statuses/%s";
	private JenkinsLocationConfiguration locationConfig = new JenkinsLocationConfiguration();
	
	private final String SUCCESS_DESCRIPTION = "Build succeeded. Good to merge.";
	private final String FAILURE_DESCRIPTION = "Build failed. Check the CI build for more information";
	private final String PENDING_DESCRIPTION = "Build triggered in CI machine";
	
	private final String SUCCESS_STATE_STRING = "success";
	private final String FAILURE_STATE_STRING = "failure";
	private final String PENDING_STATE_STRING = "pending";
	
	private boolean testingMode;
	
	
	public enum State {
		PENDING, SUCCESS, FAILURE
	}
	
	public CommitStatus(String orgName, PrintStream logger, boolean testingMode) {
		this.orgName = orgName;
		this.logger = logger;
		this.testingMode = testingMode;
	}
	
	public void setAccessToken(String token) {
		accessToken = token;
	}
	
	private String getDefaultDescription(State state) {
		String description;
		switch (state) {
			case SUCCESS:
				description = SUCCESS_DESCRIPTION;
				break;
				
			case FAILURE:
				description = FAILURE_DESCRIPTION;
				break;
				
			default:
				description = PENDING_DESCRIPTION;				
		}
		return description;
	}
	
	private String getStateText(State state) {
		String stateText;
		switch (state) {
			case SUCCESS:
				stateText = SUCCESS_STATE_STRING;
				break;
				
			case FAILURE:
				stateText = FAILURE_STATE_STRING;
				break;	
				
			default:			
				stateText = PENDING_STATE_STRING;				
		}
		return stateText;
	}
	
	private CommitStatusPayload getPayload( State status, String buildRefUrl, String description , String context) {		
		CommitStatusPayload payload = new CommitStatusPayload();
		payload.setContext(context);
		payload.setDescription(description);
		payload.setTarget_url(locationConfig.getUrl() + buildRefUrl);
		payload.setState(getStateText(status));
		return payload;
	}
	
	public void updateAll(Map<String, Map<String, Object>> pullRequests, State state, String buildRefUrl) {			
		updateAll(pullRequests, state, buildRefUrl , getDefaultDescription(state));
	}
	
	public void updateAll(Map<String, Map<String, Object>> pullRequests, State state, String buildRefUrl, String description) {
		if (pullRequests == null) {
			return;
		}
		Map<String, Object> pr;
		String module;
		String sha;
		Iterator<String> modules = pullRequests.keySet().iterator();
		while (modules.hasNext()) {
			module = modules.next();
			pr = pullRequests.get(module);
			sha = (String) ((Map<String, Object>) pr.get("head")).get("sha");
			update(module, sha, state, buildRefUrl, description);
		}
		
	}

	public void update(String repo, String sha, State status, String buildRefUrl, String description) {
		update(repo, sha, status, buildRefUrl, description, context);
	}
	
	public void update(String repo, String sha, State status, String buildRefUrl, String description , String context) {
		HttpClient client;
		HttpPost postMethod;
		HttpEntity entity;
		int responseStatusCode;
		CommitStatusPayload payload = getPayload(status, buildRefUrl, description, context);
		if (testingMode) {
			logger.println("URL :: " +  String.format(END_POINT, orgName, repo, sha));
			logger.println("Post Data :: " +  payload);
			return;
		}
		try {
			client = new HttpClient();
			postMethod = new HttpPost(String.format(END_POINT, orgName, repo, sha));
			postMethod.setHeader("Content-Type", "application/json");
			if (accessToken != null && !accessToken.isEmpty()) {
				postMethod.setHeader("Authorization", "token " +  accessToken);
			}
			entity = (HttpEntity) new StringRequestEntity(payload.toString(), "application/json", "UTF-8");
			postMethod.setEntity(entity);
			responseStatusCode = client.executeMethod((HttpMethod) postMethod);
			if (responseStatusCode != HttpStatus.SC_OK){
				logger.println("Commit status API update failed with STATUS CODE :: " + responseStatusCode);
			}
		} catch(Exception ex) {
			logger.println("Commit status API update failed  :: " + ex.getMessage());
			ex.printStackTrace();
		}
			
	}
	
}
