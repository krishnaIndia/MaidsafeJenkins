/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.MaidsafeJenkins;

import hudson.model.InvisibleAction;

/**
 *
 * @author krishnakumarp
 */
public class GithubCheckoutAction extends InvisibleAction {
    
    private String branchTarget;        
    private ShellScript script;
    private String baseBranch;

    public String getBaseBranch() {
        return baseBranch;
    }

    public void setBaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }
        
    public String getBranchTarget() {
        return branchTarget;
    }

    public void setBranchTarget(String branchTarget) {
        this.branchTarget = branchTarget;
    }

    public ShellScript getScript() {
        return script;
    }

    public void setScript(ShellScript script) {
        this.script = script;
    }
        
        
}
