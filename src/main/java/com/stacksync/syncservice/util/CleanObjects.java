/*
 * HOW TO USE:
 * 	- cleanUserObjects(userID): Delete ALL metadata from ALL workspaces
 * 	- cleanUserObjects(userID, workspaceID): Delete ALL metadata from
 * 											 workspaceID
 */

package com.stacksync.syncservice.util;

import java.util.ArrayList;
import java.util.List;

public class CleanObjects {

	//private RiakConnector rc;
	
	public CleanObjects(String ip, int port) throws Exception{
		//this.rc = new RiakConnector(ip, port);
	}
	
	public void cleanUserObjects(String userID){
		/*
		try {
			List<String> workspaces = this.rc.getUserWorkspace(userID);
			for (String workspace : workspaces){
				cleanUserObjects(userID, workspace);
			}
		} catch (Exception e){
			System.out.println(e.toString());
		}
		*/
	}
	
	public void cleanUserObjects(String userID, String workspace){
		/*
		try {
			JSONObject objects = this.rc.getWorkspaceObjects(workspace);
			Iterator<String> iterObjects = objects.keys();
			
			while( iterObjects.hasNext() ){
				String objectID = iterObjects.next();
				JSONObject fileMetadata = objects.getJSONObject(objectID);
				int latestVersion = fileMetadata.getInt(Constants.KEY_VERSION);
				
				for (int i=1; i<=latestVersion; i++){
					String key = workspace+":"+objectID+":"+i;
					this.rc.deleteObjectVersionResource(key);
				}
			}
		
			this.rc.putWorkspaceObjectResource(workspace, new JSONObject());
			
		} catch (Exception e){
			System.out.println(e.toString());
		}
		*/
	}
	
	public static void showUsage(){
		System.out.println("Usage:");
		System.out.println("\t-u user_id (required)");
		System.out.println("\t-i ip (required)");
		System.out.println("\t-p port (required)");
		System.out.println("\t-w workspace_id (optional)");
		System.out.println("Example: CleanObjects -u 123 -i 127.0.0.1 -p 8087 -w a -w b");
	}
	
	public static void main(String[] argv) throws Exception{

		
		if ( argv.length < 2 || argv.length%2 != 0){
			showUsage();
			System.exit(1);
		}
		
		String userID = null;
		String ip = null;
		int port = -1;
		List<String> workspaces = new ArrayList<String>();
		
		for ( int i=0; i<argv.length; i++ ) {
			String arg = argv[i++];
			if ( arg.equals("-u") ) {
				userID = argv[i];
			} else if ( arg.equals("-w") ) {
				workspaces.add(argv[i]);
			} else if ( arg.equals("-i") ) {
				ip = argv[i];
			} else if ( arg.equals("-p") ) {
				port = Integer.parseInt(argv[i]);
			} else {
				showUsage();
				System.exit(1);
			}
		}
		
		if ( userID == null || ip == null || port == -1) {
			showUsage();
			System.exit(1);
		}
		
		CleanObjects cleaner = new CleanObjects(ip, port);
		
		if ( workspaces.size() == 0 ) {
			System.out.println("Removing objects from ALL workspaces from user "+userID);
			cleaner.cleanUserObjects(userID);
		} else {
			System.out.println("Removing objects from workspaces "+workspaces.toString()+" from user "+userID);
			for ( String w : workspaces ){
				cleaner.cleanUserObjects(userID, w);
			}
		}
		
		System.exit(0);
	}
}
