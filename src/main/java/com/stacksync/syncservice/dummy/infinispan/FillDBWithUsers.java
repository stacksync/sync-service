/**
 *
 */
package com.stacksync.syncservice.dummy.infinispan;

import com.stacksync.syncservice.db.Connection;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.db.DAOFactory;
import com.stacksync.syncservice.db.infinispan.DeviceDAO;
import com.stacksync.syncservice.db.infinispan.UserDAO;
import com.stacksync.syncservice.db.infinispan.WorkspaceDAO;
import com.stacksync.syncservice.db.infinispan.models.DeviceRMI;
import com.stacksync.syncservice.db.infinispan.models.UserRMI;
import com.stacksync.syncservice.db.infinispan.models.WorkspaceRMI;
import com.stacksync.syncservice.util.Config;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 *
 */
public class FillDBWithUsers {
    // 584316987,558432e6-c0d9-3843-b2aa-11b08ff90a65

    protected final Logger logger = Logger.getLogger(FillDBWithUsers.class.getName());
    private ConnectionPool pool;
    private UserDAO userDao;
    private WorkspaceDAO workspaceDao;
    private DeviceDAO deviceDao;

    private FillDBWithUsers() throws Exception {
        String configPath = "config.properties";
        Config.loadProperties(configPath);
        String datasource = Config.getDatasource();
        pool = ConnectionPoolFactory.getConnectionPool(datasource);
        Connection conn = pool.getConnection();

        DAOFactory factory = new DAOFactory(datasource);
        userDao = factory.getUserDao(conn);
        deviceDao = factory.getDeviceDAO(conn);
        workspaceDao = factory.getWorkspaceDao(conn);
    }

    private void createUser(UUID userId) throws Exception {
        String idStr = userId.toString();

        UserRMI user = new UserRMI();
        user.setName(idStr);
        user.setId(userId);
        user.setEmail(idStr);
        user.setSwiftUser(idStr);
        user.setSwiftAccount(idStr);
        user.setQuotaLimit(2048);
        user.setQuotaUsed(1403);

        DeviceRMI device = new DeviceRMI(UUID.randomUUID(),"1+1",user);
        device.setAppVersion("1");
        device.setLastIp("192.168.1.1");
        device.setOs("Android");
        
        user.addDevice(device);

        WorkspaceRMI workspace = new WorkspaceRMI();
        workspace.setId(userId);
        workspace.setLatestRevision(0);
        workspace.setOwner(userId);

        userDao.add(user);
        workspaceDao.add(workspace);
        deviceDao.add(device);
    }

    public static void main(String[] args) throws Exception {

        args = new String[] {  "/home/cotes/Desktop/FUCKINGTEST/1day_users.csv"};
        if (args.length != 1) {
            System.err.println("Usage: file_path");
            System.exit(0);
        }

        FillDBWithUsers filler = new FillDBWithUsers();

        String line;
        BufferedReader buff = new BufferedReader(new FileReader(new File(args[0])));
        int counter = 0;
        while ((line = buff.readLine()) != null) {
            try {
                String id = line.split(",")[1];
                filler.createUser(UUID.fromString(id));
                counter++;
                if (counter%100 == 0){
                    System.out.println(counter+" users added.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        buff.close();
    }
}
