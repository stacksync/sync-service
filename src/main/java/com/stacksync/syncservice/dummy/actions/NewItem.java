/**
 * 
 */
package com.stacksync.syncservice.dummy.actions;

import static com.stacksync.syncservice.db.DAOUtil.prepareStatement;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.stacksync.commons.models.Chunk;
import com.stacksync.commons.models.Device;
import com.stacksync.commons.models.Item;
import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.commons.models.ItemVersion;
import com.stacksync.commons.models.User;
import com.stacksync.commons.models.Workspace;
import com.stacksync.syncservice.db.DAOError;
import com.stacksync.syncservice.db.ItemVersionDAO;
import com.stacksync.syncservice.db.postgresql.PostgresqlDAO;
import com.stacksync.syncservice.exceptions.dao.DAOException;
import com.stacksync.syncservice.handler.Handler;

/**
 * @author Sergi Toda <sergi.toda@estudiants.urv.cat>
 * 
 */
public class NewItem extends Action {

	private static final Logger logger = Logger.getLogger(PostgresqlDAO.class.getName());
	// timestamp,op,file_id,file_type,file_mime,file_size,file_version,sid,user_id
	// 0.003,new,1148728602,File,utext/x-python,,1,e7b48491-0a4d-4e39-9e81-f486fc404095,55a21c4e-68d7-3e27-a510-4314065ca088
	// 0.031,mod,1207353482,,,105654,2,47cae7aa-81ff-4a47-84d5-512e12c141a5,c4b359db-3bb2-31b9-ad39-231d2a94c6e2
	// 0.042,mod,3001803552,,,832,2,b1d91de2-aa05-402c-98a7-577f2eedb1c1,022df093-f6a5-3124-9dbf-efb41cc74105
	// 0.065,new,4207445328,File,uimage/jpeg,,1,e6b74dad-21a4-480a-9d3a-cc9850c4979c,76821f34-1a13-3f16-8795-5cefb8131bb8
	// 0.066,mod,688571918,,,1537,2,332e299c-513e-4ba2-a386-10c8387b1759,0654f5c9-7aca-3a41-8efd-4ada692191a1
	// 0.079,mod,1361784091,,,3672137,2,c938f5e4-8852-468d-9999-a60ac145de4b,93fbc684-a228-3a3a-901c-c1263c4def5c
	// 0.103,mod,2797693937,,,733,2,862a630d-2eae-495d-9a53-3630d2b14d2e,fa057485-0ca8-3b7a-af37-7023e237fcea

	private Connection connection;
	private ItemVersionDAO itemVersionDao;

	public NewItem(Handler handler, UUID userId, Long fileId, Long fileSize, String fileType, String fileMime, Long fileVersion) {
		super(handler, userId, fileId, fileSize, fileType, fileMime, fileVersion);
		status = "NEW";

		connection = handler.getConnection();
		itemVersionDao = handler.getItemVersionDao();

		Random ran = new Random(System.currentTimeMillis());
		if (fileMime == null) {
			String[] mimes = { "pdf", "php", "java", "docx", "html", "png", "jpeg", "xml" };
			super.fileMime = mimes[ran.nextInt(mimes.length)];
		} else if (fileMime.length() > 20) {
			super.fileMime = fileMime.substring(0, 20);
		}
		if (fileSize == null) {
			int max = 8;
			int min = 1;

			super.fileSize = (long) ((ran.nextInt((max - min) + 1) + min) * CHUNK_SIZE);
		}
		if (fileType == null) {
			super.fileType = "File";
		}

	}

	@Override
	public void doCommit() throws DAOException {
		// Create user info
		User user = new User(userId);
		Device device = new Device(userId);
		Workspace workspace = new Workspace(userId);

		Random ran = new Random(System.currentTimeMillis());

		ItemMetadata metadata = createItemMetadata(ran);
		saveNewObject(user, metadata, workspace, device);
	}

	private void saveNewObject(User user, ItemMetadata metadata, Workspace workspace, Device device) throws DAOException {
		// Insert object to DB
		Item item = new Item();
		item.setId(metadata.getId());
		item.setFilename(metadata.getFilename());

		String mimetype = metadata.getMimetype();
		if (mimetype.length() > 20) {
			mimetype = mimetype.substring(0, 20);
		}

		item.setMimetype(mimetype);
		item.setIsFolder(metadata.isFolder());
		item.setClientParentFileVersion(metadata.getParentVersion());

		item.setLatestVersion(metadata.getVersion());
		item.setWorkspace(workspace);
		item.setParent(null);

		// Add user item
		add(user, item);

		// Insert objectVersion
		ItemVersion objectVersion = new ItemVersion();
		objectVersion.setVersion(metadata.getVersion());
		objectVersion.setModifiedAt(metadata.getModifiedAt());
		objectVersion.setChecksum(metadata.getChecksum());
		objectVersion.setStatus(metadata.getStatus());
		objectVersion.setSize(metadata.getSize());

		objectVersion.setItem(item);
		objectVersion.setDevice(device);
		itemVersionDao.add(user, objectVersion);

		// If no folder, create new chunks
		if (!metadata.isFolder()) {
			List<String> chunks = metadata.getChunks();
			this.createChunks(user, chunks, objectVersion);
		}
	}

	protected ItemMetadata createItemMetadata(Random ran) {
		Long id = fileId;
		String filename = id.toString();
		Long version = 1L;

		Long parentId = null;
		Long parentVersion = null;

		Date modifiedAt = new Date();
		Long checksum = (long) ran.nextInt(Integer.MAX_VALUE);
		List<String> chunks = new ArrayList<String>();
		Boolean isFolder = fileType.equals("File") ? false : true;
		String mimetype = fileMime;

		// Fill chunks
		int numChunks = (int) Math.ceil((double) (fileSize / CHUNK_SIZE));
		long size = fileSize;

		for (int i = 0; i < numChunks; i++) {
			String str = java.util.UUID.randomUUID().toString();
			try {
				chunks.add(doHash(str));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}

		ItemMetadata itemMetadata = new ItemMetadata(id, version, userId, parentId, parentVersion, status, modifiedAt, checksum, size,
				isFolder, filename, mimetype, chunks);
		itemMetadata.setChunks(chunks);
		itemMetadata.setTempId((long) ran.nextInt(10));

		return itemMetadata;
	}

	private void add(User user, Item item) throws DAOException {

		if (!item.isValid()) {
			throw new IllegalArgumentException("Item attributes not set");
		}

		Object[] values = { user.getId(), item.getId(), item.getWorkspace().getId(), item.getLatestVersion(), item.getParentId(),
				item.getFilename(), item.getMimetype(), item.isFolder(), item.getClientParentFileVersion() };

		String query = "SELECT add_item(?::uuid, ?, ?::uuid, ?, ?, ?, ?, ?, ? )";

		ResultSet resultSet = executeQuery(query, values);

		try {
			if (resultSet.next()) {
				Long id = (Long) resultSet.getObject(1);
				if (!id.equals(item.getId())) {
					throw new DAOException("Creating object failed, different key obtained.");
				}
			} else {
				throw new DAOException("Creating object failed, no generated key obtained.");
			}
		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(DAOError.INTERNAL_SERVER_ERROR);
		}

	}

	private ResultSet executeQuery(String query, Object[] values) throws DAOException {

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			preparedStatement = prepareStatement(connection, query, false, values);
			resultSet = preparedStatement.executeQuery();

		} catch (SQLException e) {
			logger.error(e);
			throw new DAOException(e, DAOError.INTERNAL_SERVER_ERROR);
		}

		return resultSet;
	}

	private void createChunks(User user, List<String> chunksString, ItemVersion objectVersion) throws IllegalArgumentException,
			DAOException {
		if (chunksString != null) {
			if (chunksString.size() > 0) {
				List<Chunk> chunks = new ArrayList<Chunk>();
				int i = 0;

				for (String chunkName : chunksString) {
					chunks.add(new Chunk(chunkName, i));
					i++;
				}

				itemVersionDao.insertChunks(user, chunks, objectVersion.getId());
			}
		}
	}

}
