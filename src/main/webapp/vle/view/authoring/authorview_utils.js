/**
 * Util functions for the authoring view
 * 
 * @author patrick lawler
 * @author jonathan lim-breitbart
 */
/**
 * Returns the content base from the given full url to a file.
 */
View.prototype.utils.getContentBaseFromFullUrl = function(url){
	if(url.indexOf('\\')!=-1){
		return url.substring(0, url.lastIndexOf('\\'));
	} else {
		return url.substring(0, url.lastIndexOf('/'));
	};
};

/**
 * Returns the path separator used by the given url.
 */
View.prototype.utils.getSeparator = function(url){
	if(url.indexOf('\\')!=-1){
		return '\\';
	} else {
		return '/';
	};
};

/**
 * Given a nodeId, removes associated node content file from server.
 */
View.prototype.utils.removeNodeFileFromServer = function(view, nodeId){
	var filename = view.getProject().getNodeFilename(nodeId);
	
	var callback = function(text, xml, o){
		if(text!='success'){
			o.notificationManager.notify('failed request to remove file: ' + filename + '  from the server', 3);
		};
	};

	if(filename){
		view.connectionManager.request('POST', 1, view.requestUrl, {forward:'filemanager', projectId:view.portalProjectId, command: 'removeFile', projectPath: this.getContentPath(view.authoringBaseUrl,view.getProject().getContentBase()), fileName: filename}, callback, view);
	};
};

/**
 * Returns the corresponding todo filename for the currently opened project.
 */
View.prototype.utils.getTODOFilename = function(projectFilename){
	if(projectFilename.indexOf('.project.json')!=-1){
		/* this is a raw project file */
		return projectFilename.replace('.project.json','.todo.text');
	} else {
		/* this is a versioned project file */
		return projectFilename.replace(/\.project\.(.*)\.json/,'.todo.$1.text');
	};
};

/**
 * Returns the corresponding project meta filename for the currently opened project.
 */
View.prototype.utils.getProjectMetaFilename = function(projectFilename){
	if(projectFilename.indexOf('.project.json')!=-1){
		/* this is raw project file */
		return projectFilename.replace('.project.json','.project-meta.json');
	} else {
		/* this is a versioned project file */
		return projectFilename.replace(/\.project\.(.*)\.json/, '.project-meta.$1.json');
	};
};

/**
 * Hides all nodes in the project
 */
View.prototype.utils.hideNodes = function(){
	//$('.projectNode.node').parent().parent().addClass('hidden');
	$('#hideNodesBtn').addClass('hidden');
	$('#showNodesBtn').removeClass('hidden');
	$('#showNodesBtn').removeAttr('disabled');
	$('.projectNode.node').parent().parent().fadeOut();
};

/**
 * Shows all nodes in the project
 */
View.prototype.utils.unhideNodes = function(){
	//$('.projectNode.node').parent().parent().removeClass('hidden');
	$('#showNodesBtn').addClass('hidden');
	$('#hideNodesBtn').removeClass('hidden');
	$('#hideNodesBtn').removeAttr('disabled');
	$('.projectNode.node').parent().parent().fadeIn();
};

/**
 * Adjustments to be made when browser window is resized
 */
View.prototype.utils.resize = function(){
	// set max width of project title display
	var titleMaxW = $('#infoSummary').width() - $('#infoSummary a.bookmark').outerWidth() - $('#projectId').outerWidth() - $('#sharedIcon').outerWidth() - $('#libraryIcon').outerWidth()- $('#topProjectTools').outerWidth() - 50;
	$('#projectTitle').css('max-width',titleMaxW + 'px');
	$('#projectTitle').width('auto').width($('#projectTitle').width() + 2);
	
	// set width of activity editing controls
	if($('#seqControls').hasClass('sticky')){
		view.positionActivityControls();
	}
	
	// set projectStructure to fit the remaining height of the projectContent
	var psHeight = $('#projectContent').height() - $('#projectInfo').outerHeight(true) - parseInt($('#projectContent > .panelContent').css('padding-top'),10) - parseInt($('#projectContent > .panelContent').css('padding-bottom'),10);
	$('#projectStructure').height(psHeight);
};

/*
 * Returns the current view mode (student, grading, authoring, etc.)
 */
View.prototype.getMode = function() {
	var mode = this.config.getConfigParam('mode');
	return mode;
};


// add indexOf functionality to js in ie6 and ie7
if (!Array.prototype.indexOf)  
{  
  Array.prototype.indexOf = function(elt /*, from*/)  
  {  
    var len = this.length >>> 0;  
  
    var from = Number(arguments[1]) || 0;  
    from = (from < 0)  
         ? Math.ceil(from)  
         : Math.floor(from);  
    if (from < 0)  
      from += len;  
  
    for (; from < len; from++)  
    {  
      if (from in this &&  
          this[from] === elt)  
        return from;  
    }  
    return -1;  
  };  
}

/**
 * Retrieve path to project folder for current node
 * e.g.
 * "http://wise.berkeley.edu/curriculum/135/"
 * @return full path to the project folder
 */
View.prototype.getProjectFolderPath = function() {
	/*
	 * get the content base url which should be the url to the curriculum folder
	 * e.g.
	 * http://wise.berkeley.edu/curriculum
	 */
	var curriculumBaseUrl = this.getConfig().getConfigParam('curriculumBaseUrl');

	var fullProjectFolderPath = null;
	
	if(this.getProjectMetadata().projectFolder != null) {
		/*
		 * the project folder is in the project meta data
		 * e.g.
		 * /135
		 * 
		 * so the full project folder path will look like
		 * http://wise.berkeley.edu/curriculum/135
		 */
		fullProjectFolderPath = curriculumBaseUrl + this.getProjectMetadata().projectFolder;
	}
	
	//make sure the projectFolder ends with '/'
	if(fullProjectFolderPath.charAt(fullProjectFolderPath.length - 1) != '/') {
		fullProjectFolderPath += '/';
	}
	
	return fullProjectFolderPath;
};

//used to notify scriptloader that this script has finished loading
if(typeof eventManager != 'undefined'){
	eventManager.fire('scriptLoaded', 'vle/view/authoring/authorview_utils.js');
};