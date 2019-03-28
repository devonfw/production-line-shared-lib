package com.capgemini.productionline.configuration

/**
 * Contains the configuration methods of the gitlab component
 * <p>
 *     The main purpose collecting configuration methods.
 *
 * Created by tlohmann on 19.10.2018.
 */
class GitLab implements Serializable {

  def String accesstoken = ""
  def String gitlabHostUrl = ""
  def context

  //In order to access the GitLab API, we need a private token!
  //context is the JENKINS object "this"
  GitLab (context, token, gitlabHostUrl) {
    this.context = context
    this.accesstoken = token
    this.gitlabHostUrl = gitlabHostUrl
  }

  public String getGroupId (String groupname) {
    def searchresult = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'GET', url: "${gitlabHostUrl}/api/v4/groups?search="+groupname
    def jsonObject = this.context.readJSON text: searchresult.getContent()
    return String.valueOf(jsonObject.id).replace("[","").replace("]","")
  } 

  //Returns the project ID of a GIT project
  public String getProjectId (String groupname, String projectname) {
    def groupid = getGroupId(groupname)
    def searchresult = this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'GET', url: "${gitlabHostUrl}/api/v4/groups/"+groupid+'/projects?search='+projectname
    def jsonObject = this.context.readJSON text: searchresult.getContent()
    return String.valueOf(jsonObject.id).replace("[","").replace("]","")
  } 

  //In order to create a group in Gitlab the user needs to have the "Can create groups permissions". This needs to be sex explicitely!
  public createGroup(String groupname, String grouppath, String groupdesc, String grouptype) {
    this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'POST', url: "${gitlabHostUrl}/api/v4/groups?name="+groupname+'&path='+grouppath+'&description='+java.net.URLEncoder.encode(groupdesc, "UTF-8")+'&visibility='+grouptype
  }

  //Creates a new GIT project
  // Ref: https://docs.gitlab.com/ce/api/projects.html#create-project
  //
  // visibility = private/internal/public
  // import_url = set to any public git repo and it will clone it!
  public createProject(String groupname, String projectname, String projectpath, String projectdescription, String branchname, String importurl, String visibility) {
    def groupid = getGroupId(groupname)
    // create project in target group
    this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'POST', url: "${gitlabHostUrl}/api/v4/projects?name="+projectname+'&path='+projectpath+'&namespace_id='+groupid+'&default_branch='+branchname+'&description='+java.net.URLEncoder.encode(projectdescription, "UTF-8")+'&import_url='+importurl+'&visibility='+visibility
  }

  //Creates a new branch in a gitlab project
  public createBranch(String group, String project, String from, String branchname) {
    def projectid = getProjectId(groupname)
    this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'POST', url: "${gitlabHostUrl}/api/v4/projects/"+projectid+'/repository/branches?branch='+branchname+'&ref='+from
  }

  public protectBranch(String group, String project, String branchname) {
    def projectid = getProjectId(groupname)
    this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'PUT', url: "${gitlabHostUrl}/api/v4/projects/"+projectid+'/repository/branches/'+branchname+'/protect'
  }

  public unprotectBranch(String group, String project, String branchname) {
    def projectid = getProjectId(groupname)
    this.context.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: accesstoken]], httpMode: 'PUT', url: "${gitlabHostUrl}/api/v4/projects/"+projectid+'/repository/branches/'+branchname+'/unprotect'
  }
}