package org.wise.vle.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.TreeMap;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.run.Run;
import org.wise.portal.domain.user.User;
import org.wise.portal.domain.workgroup.Workgroup;
import org.wise.portal.presentation.web.controllers.ControllerUtil;
import org.wise.portal.service.offering.RunService;
import org.wise.portal.service.project.ProjectService;
import org.wise.portal.service.workgroup.WorkgroupService;

/**
 * Servlet implementation class AssetManager
 * 
 * @author Patrick Lawler
 * @author Geoffrey Kwan
 */
public class AssetManager extends AbstractController {
	private static final long serialVersionUID = 1L;

	private final static String COMMAND = "command";

	private final static String PATH = "path";

	private final static String ASSET = "asset";

	private final static String FAILED = "failed";

	private static Properties wiseProperties = null;
	
	private RunService runService;
	
	private WorkgroupService workgroupService;
	
	private ProjectService projectService;

	private boolean standAlone = true;

	private boolean modeRetrieved = false;

	static {
		try {
			// Read properties file.
			wiseProperties = new Properties();
			wiseProperties.load(FileManager.class.getClassLoader().getResourceAsStream("wise.properties"));
		} catch (Exception e) {
			System.err.println("FileManager could not read in wiseProperties file");
			e.printStackTrace();
		}
	}
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AssetManager() {
		super();
	}


	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (request.getMethod() == AbstractController.METHOD_GET) {
			return doGet(request, response);
		} else if (request.getMethod() == AbstractController.METHOD_POST) {
			return doPost(request, response);
		}
		return null;
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected ModelAndView doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//get the command such as assetList or getSize 
		String command = request.getParameter("command");
		String type = request.getParameter("type");
		
		if(type.equals("studentAssetManager")) {
			//the user is a student
			
			if(command.equals("assetList")) {
				//we are going to list the 
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				//get the directory name for the workgroup for this run
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced"; // looks like /studentuploads/[runId]/[workgroupId]/unreferenced
				
				//get the student uploads base directory path
				String path = wiseProperties.getProperty("studentuploads_base_dir");
				
				//get a list of file names in this workgroup's upload directory
				String assetList = getAssetList(path, dirName);
				response.getWriter().write(assetList);
			} else if(command.equals("getSize")) {
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				//get the directory name for the workgroup for this run
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced"; // looks like /studentuploads/[runId]/[workgroupId]/unreferenced
				
				//get the student uploads base directory path
				String path = wiseProperties.getProperty("studentuploads_base_dir");
				
				//get the disk space usage of the workgroup's upload directory
				String result = getSize(path, dirName);
				response.getWriter().write(result);
			}
		}
		
		return null;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected ModelAndView doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String command = request.getParameter("command");
		String type = request.getParameter("type");
		
		if(type.equals("studentAssetManager")) {
			//the user is a student
			
			if(command.equals("remove")) {
				//the student is removing an asset
				
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				//get the directory name for the workgroup for this run
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced"; // looks like /studentuploads/[runId]/[workgroupId]/unreferenced
				
				//get the student uploads base directory path
				String path = wiseProperties.getProperty("studentuploads_base_dir");
				
				//get the file name the student wants to remove
				String assetFileName = request.getParameter("asset");
				
				//remove the file from the student asset folder
				String result = removeAsset(path, dirName, assetFileName);
				
				response.getWriter().write(result);
			} else if(command.equals("studentAssetCopyForReference")) {
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				// looks like /studentuploads/[runId]/[workgroupId]/unreferenced
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced";
				
				String referencedDirName = "";
				String commandParameter = request.getParameter("command");
				if (commandParameter != null && "studentAssetCopyForReference".equals(commandParameter)) {
					// if we're copying student asset for reference, also pass along the referenced dir. looks like /studentuploads/[runId]/[workgroupId]/referenced
					referencedDirName = run.getId()+"/"+workgroupId+"/referenced";
				}
				
				//get the file name to copy
				String fileName = request.getParameter("assetFilename");
				
				String result = copyAssetForReference(dirName, referencedDirName, fileName);
				
				response.getWriter().write(result);
			} else if(command.equals("uploadAsset")) {
				//the student is uploading an asset
				
				ServletFileUpload uploader = new ServletFileUpload(new DiskFileItemFactory());
				List<?> fileList = null;
				try {
					//get a list of the files that are being uploaded
					fileList = uploader.parseRequest(request);
				} catch (FileUploadException e) {
					e.printStackTrace();
				}
				
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				//get the directory name for the workgroup for this run
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced";
				
				//get the student uploads base directory path
				String path = wiseProperties.getProperty("studentuploads_base_dir");
				Long studentMaxTotalAssetsSize = new Long(wiseProperties.getProperty("student_max_total_assets_size", "2097152"));
				String pathToCheckSize = path + "/" + dirName;
				
				DefaultMultipartHttpServletRequest multiRequest = (DefaultMultipartHttpServletRequest) request;
				List<String> fileNames = new ArrayList<String>();
				Map<String,byte[]> fileMap = new TreeMap<String,byte[]>();
				
				//get all the file names and files to be uploaded
				Iterator iter = multiRequest.getFileNames();
				while(iter.hasNext()){
					String filename = (String)iter.next();
					fileNames.add(filename);
					fileMap.put(filename, multiRequest.getFile(filename).getBytes());
				}
				
				//upload the files
				String result = uploadAsset(fileList, fileNames, fileMap, path, dirName, pathToCheckSize, studentMaxTotalAssetsSize);
				
				response.getWriter().write(result);
			} else if(command.equals("download")){
				// the student is downloading assets
				
				User user = ControllerUtil.getSignedInUser();
				
				//get the run
				String runId = request.getParameter("runId");
				Run run = null;
				try {
					run = getRunService().retrieveById(new Long(runId));
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (ObjectNotFoundException e) {
					e.printStackTrace();
				}
				
				//get the workgroup id
				List<Workgroup> workgroupListByOfferingAndUser = getWorkgroupService().getWorkgroupListByOfferingAndUser(run, user);
				Workgroup workgroup = workgroupListByOfferingAndUser.get(0);
				Long workgroupId = workgroup.getId();
				
				//get the directory name for the workgroup for this run
				String dirName = run.getId()+"/"+workgroupId+"/unreferenced"; // looks like /studentuploads/[runId]/[workgroupId]/unreferenced
				
				//get the student uploads base directory path
				String path = wiseProperties.getProperty("studentuploads_base_dir");
				
				// get the projectId
				String projectId = (String) run.getProject().getId();
				//String projectId = request.getParameter(PROJECTID);
				
				// get the asset(s) to download from the request
				String asset = request.getParameter(ASSET);
				AssetManager.downloadAsset(path, dirName, projectId, asset, response);
			}
		}
		
		return null;
	}
	

	/**
	 * Uploads the specified file to the given path.
	 * @param fileList a list of files that are to be uploaded
	 * @param fileNames the names of the files that are to be uploaded
	 * @param fileMap the files that are to be uploaded
	 * @param path the path to the project folder or the student uploads base directory
	 * @param dirName the folder name to upload to which will be assets or the directory
	 * for a workgroup for a run
	 * @param pathToCheckSize the path to check the disk space usage for. if we are uploading
	 * to a project we will check the whole project folder size. if we are uploading to a 
	 * student folder we will check that student folder
	 * @param maxTotalAssetsSizethe the max disk space usage allowable
	 * @return the message of the status of the upload
	 */
	@SuppressWarnings("unchecked")
	public static String uploadAsset(List<?> fileList, List<String> fileNames, Map<String,byte[]> fileMap, String path, String dirName, String pathToCheckSize, Long maxTotalAssetsSize) {
		String fullPath = path + "/" + dirName;
		
		try{
			/* if request was forwarded from the portal, the fileList will be empty because
			 * Spring already retrieved the list (it can only be done once). But Spring wrapped
			 * the request so we can get the file another way now */
			if(fileList.size()>0){
				Iterator<?> fileIterator = fileList.iterator();
				while(fileIterator.hasNext()){
					FileItem item = (FileItem)fileIterator.next();
					if(item.isFormField()){ //get path and set var
						if(item.getFieldName().equals(PATH)){
							path = item.getString();
						} else if(item.getFieldName().equals("forward") || item.getFieldName().equals("projectId")){
							// do nothing
						} else {
							throw new ServletException("I do not know what to do with multipart form field of name: " + item.getFieldName() + ". Cannot upload asset.");
						}
					} else { //do upload
						if(path!=null){
							if(!ensureAssetPath(path, dirName)){
								throw new ServletException("Unable to find or setup path to upload file. Operation aborted.");
							} else {
								File projectDir = new File(path);
								File assetsDir = new File(projectDir, dirName);
								if(Long.parseLong(getFolderSize(pathToCheckSize)) + item.getSize() > maxTotalAssetsSize){
									return "Uploading " + item.getName() + " of size " + appropriateSize(item.getSize()) + " would exceed maximum storage capacity of " + appropriateSize(maxTotalAssetsSize) + ". Operation aborted.";
								}
								File asset = new File(assetsDir, item.getName());
								item.write(asset);
								return asset.getName() + " was successfully uploaded!";
							}
						} else {
							throw new ServletException("Path or file name for upload not specified.  Unable to upload file.");
						}
					}
				}
			} else {
				/* file upload is coming from the portal so we need to read the bytes
				 * that the portal set in the attribute
				 */
				File projectDir = new File(path);
				File assetsDir = new File(projectDir, dirName);
				if(!assetsDir.exists()){
					assetsDir.mkdirs();
				}

				if(SecurityUtils.isAllowedAccess(path, assetsDir)){
					String successMessage = "";

					if(fileNames != null && fileNames.size()>0 && fileMap != null && fileMap.size()>0 && fileNames.size()==fileMap.size()){
						Iterator<String> iter = fileNames.listIterator();
						while(iter.hasNext()){
							String filename = iter.next();
							File asset = new File(assetsDir, filename);
							byte[] content = fileMap.get(filename);

							if(Long.parseLong(getFolderSize(pathToCheckSize)) + content.length > maxTotalAssetsSize){
								successMessage += "Uploading " + filename + " of size " + appropriateSize(content.length) + " would exceed your maximum storage capacity of "  + appropriateSize(maxTotalAssetsSize) + ". Operation aborted.";
							} else {
								if(!asset.exists()){
									asset.createNewFile();
								}

								FileOutputStream fos = new FileOutputStream(asset);
								fos.write(content);

								successMessage += asset.getName() + " was successfully uploaded! ";
							}
						}
					}

					return successMessage;
				} else {
					return "Access to path is denied.";
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			return e.getMessage();
		}

		return FAILED;
	}
	
	/**
	 * Copies a student uploaded asset to the referenced directory with a 
	 * timestamp and returns a JSON string that includes the filename of that copied file.
	 * @param dirName the student workgroup folder for the run
	 * @param referencedDirName the path to the referenced files
	 * @param fileName the file name
	 * @return String filename of the new copy
	 */
	private String copyAssetForReference(String dirName, String referencedDirName, String fileName) {
		JSONObject response = new JSONObject();

		String unreferencedAssetsDirName = dirName; 
		String referencedAssetsDirName = referencedDirName;

		//String studentUploadsBaseDirStr = (String) request.getAttribute("studentuploads_base_dir");
		String studentUploadsBaseDirStr = wiseProperties.getProperty("studentuploads_base_dir");

		/* file upload is coming from the portal so we need to read the bytes
		 * that the portal set in the attribute
		 */
		File studentUploadsBaseDir = new File(studentUploadsBaseDirStr);
		File unreferencedAssetsFullDir = new File(studentUploadsBaseDir, unreferencedAssetsDirName);
		if(!unreferencedAssetsFullDir.exists()){
			System.err.println("Unreferenced Directory Does Not Exist.");  // the unreferenced directory must exist.
			return "ERROR";
		}

		// if the referenced directory does not exist, make it.
		File referencedAssetsFullDir = new File(studentUploadsBaseDir, referencedAssetsDirName);
		if(!referencedAssetsFullDir.exists()){
			referencedAssetsFullDir.mkdirs();
		}

		// append timestamp to the file to make it unique.
		Calendar cal = Calendar.getInstance();
		int lastIndexOfDot = fileName.lastIndexOf(".");
		String newFilename = fileName.substring(0, lastIndexOfDot) + "-" + cal.getTimeInMillis() +fileName.substring(lastIndexOfDot);  // e.g. sun-20121025102912.png
		File unreferencedAsset = new File(unreferencedAssetsFullDir, fileName);
		File referencedAsset = new File(referencedAssetsFullDir, newFilename);

		try {
			this.copy(unreferencedAsset, referencedAsset);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			response.put("result", "SUCCESS");
			response.put("newFilename", newFilename);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return response.toString();
	}

		/**
		 * Copies the given <code>File</code> src to the given <code>File</code> dest. If they
		 * are directories, recursively copies the contents of the directories.
		 * 
		 * @param File src
		 * @param File dest
		 * @throws FileNotFoundException
		 * @throws IOException
		 */
		public void copy(File src, File dest) throws FileNotFoundException, IOException{
			if(src.isDirectory()){
				if(!dest.exists()){
					dest.mkdir();
				}

				String[] files = src.list();
				for(int a=0;a<files.length;a++){
					copy(new File(src, files[a]), new File(dest, files[a]));
				}
			} else {
				InputStream in = new FileInputStream(src);
				FileOutputStream out = new FileOutputStream(dest);

				byte[] buffer = new byte[2048];
				int len;
				while((len = in.read(buffer)) > 0){
					out.write(buffer, 0, len);
				}

				in.close();
				out.close();
			}
		}

		/**
		 * Checks to make sure the provided project path exists. If not returns false,
		 * if it does, then checks to see if the dirName directory exists. If it does, returns
		 * true, if not, attempts to create it. If the creation is successful, returns true,
		 * if not returns false.
		 * 
		 * @param <code>String</code> path
		 * @param <code>String</code> dirName
		 * @return boolean
		 */
		public static boolean ensureAssetPath(String path, String dirName) {
			File projectDir = new File(path);
			if(projectDir.exists()){
				File assetsDir = new File(projectDir, dirName);
				if(assetsDir.exists() && assetsDir.isDirectory()){
					return true;
				} else {
					return assetsDir.mkdir();
				}
			} else {
				return false;
			}
		}

		/**
		 * Returns the size in bytes of all of the files in the specified path/dirname
		 * @param path the path to the parent directory
		 * @param dirName the directory name 
		 * @return the disk space usage of the folder
		 */
		public static String getSize(String path, String dirName){
			if(path==null){
				return "No project path specified";
			} else {
				File projectDir = new File(path);
				if(projectDir.exists()){
					File assetsDir = new File(projectDir, dirName);
					if(assetsDir.exists() && assetsDir.isDirectory()){
						long total = 0;
						//get all file sizes and add to total
						File[] files = assetsDir.listFiles();
						for(int q=0;q<files.length;q++){
							total += files[q].length();
						}
						return String.valueOf(total);
					} else {
						return "0";
					}
				} else {
					return "Given project path does not exist.";
				}
			}
		}
		
		/**
		 * Returns the size in bytes of all of the files in the specified path/dirname
		 * 
		 * @param <code>String</code> folderPath the path to the folder as a string
		 * @return <code>String</code> size of all files in assets folder in bytes
		 */
		public static String getFolderSize(String folderPath) {
			String folderSize = "";
			
			if(folderPath != null) {
				//get a handle on the folder
				File folder = new File(folderPath);
				
				//make sure the folder exists and is a folder
				if(folder.exists() && folder.isDirectory()) {
					//get the size of the folder
					long sizeOfDirectory = FileUtils.sizeOfDirectory(folder);
					
					//get the folder size as a string
					folderSize = String.valueOf(sizeOfDirectory);
				} else {
					//folder does not exist or is not a folder
					folderSize = "Given folder path does not exist or is not a folder.";
				}
			} else {
				//folder path is null
				folderSize = "Folder path not provided.";
			}
			
			return folderSize;
		}

		/**
		 * Removes an asset from the folder
		 * @param path the path to the parent folder
		 * @param dirName the folder name
		 * @param assetFileName the file name
		 * @return a string that specifies whether the removal was successful or not
		 * @throws IOException
		 */
		public static String removeAsset(String path, String dirName, String assetFileName) throws IOException{
			String result = "";
			
			File projectDir = new File(path);
			if(path==null || !(projectDir.exists()) || !(projectDir.isDirectory())){
				result = "Bad Request";
			} else {
				File assetDir = new File(projectDir, dirName);
				if(!assetDir.exists() || !assetDir.isDirectory()){
					result = "Bad Request";
				} else {
					if(assetFileName==null){
						result = "Bad Request";
					} else {
						File assetFile = new File(assetDir, assetFileName);
						if(assetFile.exists() && assetFile.isFile()){
							if(SecurityUtils.isAllowedAccess(path, assetFile.getCanonicalPath())) {
								if(assetFile.delete()){
									result = "Asset " + assetFileName + " successfully deleted from server.";
								} else {
									result = "Server Error";
								}
							} else {
								result = "Unauthorized";
							}
						} else {
							result = "Bad Request";
						}
					}
				}
			}
			
			return result;
		}

		/**
		 * Given a <code>HttpServletRequest</code> request, returns
		 * a <code>String</code> ':' denoted list of all filenames
		 * within the project path directory.
		 * In the request:
		 * - path
		 * - dirName
		 * path + dirName = full content folder path
		 * 
		 * 
		 * @param <code>HttpServletRequest</code> request
		 * 
		 * 
		 * @param dirName
		 * @param type
		 * 
		 * @param studentUploadsBaseDir
		 * @param projectFolderPath
		 * 
		 * @return <code>String</code>
		 */
		
		/**
		 * Get a list of the file names in the folder
		 * @param path the path to the parent folder
		 * @param dirName the name of the folder
		 * @return a JSONArray string containing the file names 
		 */
		public static String getAssetList(String path, String dirName) {
			// if dirname is : separated, get asset list for each dir and return concatenated result
			String[] dirNames = dirName.split(":");
			if (dirNames.length > 1) {
				JSONArray jsonArr = new JSONArray();
				try {
					for (int i=0; i<dirNames.length; i++) {
						String currDirName = dirNames[i];
						String currAssetList = getAssetListFromFolder(path,currDirName);
						if (!"".equals(currAssetList)) {
							JSONObject jsonObj = new JSONObject();
							jsonObj.put("workgroupId", currDirName);
							jsonObj.put("assets", currAssetList);
							jsonArr.put(jsonObj);
						}
					}
					return jsonArr.toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					return "";
				}				
			} else {
				return getAssetListFromFolder(path,dirName);
			}
		}

		/**
		 * Get the file names in the folder
		 * @param path the path to the parent folder
		 * @param dirName the folder name
		 * @return the disk usage of the folder
		 */
		public static String getAssetListFromFolder(String path, String dirName) {
			File projectDir = new File(path);
			if(projectDir.exists()){
				File assetsDir = new File(projectDir, dirName);
				if(assetsDir.exists() && assetsDir.isDirectory()){
					File[] files = assetsDir.listFiles();

					JSONArray fileNames = new JSONArray();

					if(files==null){//no files in this dir
						return "";
					} else {
						for(int v=0;v<files.length;v++){
							fileNames.put(files[v].getName());
						}
						return fileNames.toString();
					}
				} else {
					return "";
				}
			} else {
				return "Given project path does not exist";
			}
		}
		
		/**
		 * Given a <code>HttpServletRequest</code> with path and asset parameters
		 * finds the given asset(s) associated with the project in the given path and
		 * allows the user to download the file(s). If asset parameter contains multiple
		 * file names, bundles the requested files into a zip archive.
		 * 
		 * @param <code>HttpServletRequest</code> request
		 * @return 
		 * @return <code>String</code> message
		 * @throws IOException 
		 * @throws <code>ServletException</code>
		 */
		public static Object downloadAsset(String path, String dirName, String projectId, String asset, HttpServletResponse response) throws IOException {
			//String path = request.getParameter(PATH);
			//String dirName = (String) request.getAttribute("dirName");
			//String projectId = request.getParameter(PROJECTID);
			
			/*if (dirName == null) {
				dirName = DEFAULT_DIRNAME;
			}
			if (path == null) {
			 path = (String) request.getAttribute(PATH);
			}
			if(projectId == null){
				projectId = "";
			}

			String studentUploadsBaseDir = (String) request.getAttribute("studentuploads_base_dir");
			String projectFolderPath = (String) request.getAttribute("projectFolderPath");
			
			if (studentUploadsBaseDir != null) {
				// the user is a student
				path = studentUploadsBaseDir;
			} else if(projectFolderPath != null) {
				//the user is a teacher
				path = projectFolderPath;
			}
			
			String asset = request.getParameter(ASSET);*/
			JSONArray assets = new JSONArray();
			try {
				assets = new JSONArray(asset);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
			File projectDir = new File(path);
			if(path==null || !(projectDir.exists()) || !(projectDir.isDirectory())){
				//response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return null;
			} else {
				File assetDir = new File(projectDir, dirName);
				if(!assetDir.exists() || !assetDir.isDirectory()){
					//response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return null;
				} else {
					if(assets.length()==0){
						//response.sendError(HttpServletResponse.SC_BAD_REQUEST);
						return null;
					} else if (assets.length() == 1){
						String name;
						try {
							name = assets.getString(0);
						} catch (JSONException e) {
							//response.sendError(HttpServletResponse.SC_BAD_REQUEST);
							return null;
						}
						File assetFile = new File(assetDir, name);
						if(assetFile.exists() && assetFile.isFile()){
							// TODO: removed security check for downloading, as user is assumed to be logged in and able to author project; may want to implement one at some point?
							//if(this.standAlone || SecurityUtils.isAllowedAccess(request, assetFile)){
								//String filename = assetFile.getName();
								
								Pattern regex = Pattern.compile("\\..+$");
								Matcher regexMatcher = regex.matcher(name);
								if (regexMatcher.find()) {
								    name = regexMatcher.replaceAll(regexMatcher.group(0).toLowerCase());
								}
								
								response.setContentType(new MimetypesFileTypeMap().getContentType(name));
								response.setContentLength((int)assetFile.length());
								response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(name, "UTF-8"));

								InputStream is = new FileInputStream(assetFile);
								IOUtils.copy(is, response.getOutputStream());
								response.flushBuffer();

								return null;
							//} else {
								//response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
								//return null;
							//}
						} else {
							response.sendError(HttpServletResponse.SC_BAD_REQUEST);
							return null;
						}
					} else {
						response.setContentType("Content-type: text/zip");
						response.setHeader("Content-Disposition", "attachment; filename=project" + projectId + "_files.zip");
						ServletOutputStream out = response.getOutputStream();
						ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));
						
						for(int r=0;r<assets.length();r++){
							String name;
							try {
								name = assets.getString(r);
							} catch (JSONException e) {
								response.sendError(HttpServletResponse.SC_BAD_REQUEST);
								return null;
							}
							File assetFile = new File(assetDir, name);
							if(assetFile.exists() && assetFile.isFile()){
								// TODO: removed security check for downloading, as user is assumed to be logged in and able to author project; may want to implement one at some point?
								//if(this.standAlone || SecurityUtils.isAllowedAccess(request, assetFile)){
									
									zos.putNextEntry(new ZipEntry(assetFile.getName()));
									InputStream is = new FileInputStream(assetFile);
									BufferedInputStream fif = new BufferedInputStream(is);
									// Write the contents of the file
									int data = 0;
									while ((data = fif.read()) != -1) {
										zos.write(data);
									}
									fif.close();

									zos.closeEntry();
								//} else {
									//zos.write(("ERROR: You do not have permission to download file: " + file.getName()).getBytes());
									//zos.closeEntry();
									//System.out.println("Permission to download file: " + assetFile.getAbsolutePath() + " denied.");
									//continue;
								//}
							} else {
								response.sendError(HttpServletResponse.SC_BAD_REQUEST);
								return null;
							}
						}
						zos.close();
						return null;
					}
				}
			}
		}

		/**
		 * Given a <code>long</code> size of bytes, returns a <code>String</code>
		 * with the size either in: bytes, kilobytes or megabytes rounded
		 * to the nearest 10th.
		 * 
		 * @param <code>long</code> size
		 * @return <code>String</code>
		 */
		public static String appropriateSize(long size){
			if(size>1048576){
				return String.valueOf(Math.round(((size/1024)/1024)*10)/10) + " mb";
			} else if (size>1024){
				return String.valueOf(Math.round((size/1024)*10)/10) + " kb";
			} else {
				return String.valueOf(size) + " b";
			}
		}

		public RunService getRunService() {
			return runService;
		}

		public void setRunService(RunService runService) {
			this.runService = runService;
		}

		public WorkgroupService getWorkgroupService() {
			return workgroupService;
		}

		public void setWorkgroupService(WorkgroupService workgroupService) {
			this.workgroupService = workgroupService;
		}


		public Properties getWiseProperties() {
			return wiseProperties;
		}


		public void setWiseProperties(Properties wiseProperties) {
			this.wiseProperties = wiseProperties;
		}


		public ProjectService getProjectService() {
			return projectService;
		}


		public void setProjectService(ProjectService projectService) {
			this.projectService = projectService;
		}
	}
