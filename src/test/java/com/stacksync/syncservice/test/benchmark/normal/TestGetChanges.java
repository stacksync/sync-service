package com.stacksync.syncservice.test.benchmark.normal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import omq.common.util.Serializers.GsonImp;
import omq.common.util.Serializers.ISerializer;
import omq.common.util.Serializers.JavaImp;
import omq.common.util.Serializers.KryoImp;
import omq.exception.SerializerException;

import com.stacksync.syncservice.test.benchmark.Constants;
import com.stacksync.syncservice.db.ConnectionPool;
import com.stacksync.syncservice.db.ConnectionPoolFactory;
import com.stacksync.syncservice.handler.Handler;
import com.stacksync.syncservice.handler.SQLHandler;
import com.stacksync.syncservice.models.ItemMetadata;
import com.stacksync.syncservice.util.Config;

public class TestGetChanges {

	public static byte[] zip(byte[] b) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		GZIPOutputStream zos = new GZIPOutputStream(baos);
		zos.write(b);
		zos.close();

		return baos.toByteArray();
	}

	public static byte[] unzip(byte[] b) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayInputStream bais = new ByteArrayInputStream(b);

		GZIPInputStream zis = new GZIPInputStream(bais);
		byte[] tmpBuffer = new byte[256];
		int n;
		while ((n = zis.read(tmpBuffer)) >= 0) {
			baos.write(tmpBuffer, 0, n);
		}
		zis.close();

		return baos.toByteArray();
	}

	public static Boolean compareByteArray(byte[] array1, byte[] array2) {

		if (array1.length != array2.length) {
			return false;
		}

		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i]) {
				return false;
			}
		}

		return true;
	}

	private static void printSize(ISerializer serializer, List<ItemMetadata> list) throws IOException {
		try {
			long start = System.currentTimeMillis();
			byte[] bytes = serializer.serialize(list);
			long total = System.currentTimeMillis() - start;

			byte[] compressed = zip(bytes);
			System.out.println(serializer.getClass().getName() + " -- Tiempo: " + total + " ms || Tama�o: " + bytes.length + " Bytes");

			byte[] uncompressed = unzip(compressed);
			System.out.println("Compressed: " + compressed.length + " Bytes || UnCompressed: " + uncompressed.length + " Bytes" + " || Compare: "
					+ compareByteArray(uncompressed, bytes));

		} catch (SerializerException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception {
		Config.loadProperties();

		String datasource = Config.getDatasource();
		ConnectionPool pool = ConnectionPoolFactory.getConnectionPool(datasource);
		Handler handler = new SQLHandler(pool);

		long startTotal = System.currentTimeMillis();
		// [User:AUTH_e26e8353dbd043ae857ad6962e02f5cc,
		// Request:gguerrero201305161637-1368778961517, Workspace:
		// RemoteWorkspace[id=benchmark-93539494/, latestRevision=1, path=/]]
		// GetChangesMessage getChangesRequest = new
		// GetChangesMessage("AUTH_e26e8353dbd043ae857ad6962e02f5cc",
		// Message.GET_CHANGES, Constants.REQUESTID, "benchmark-93539494/", "");
		List<ItemMetadata> listFiles = handler.doGetChanges(Constants.WORKSPACEID, Constants.USER);

		System.out.println("Objects -> " + listFiles.size());
		int countChk = 0;
		for (ItemMetadata obj : listFiles) {
			countChk += obj.getChunks().size();
		}
		System.out.println("Chunks -> " + countChk);

		printSize(new JavaImp(), listFiles);
		printSize(new GsonImp(), listFiles);
		// printSize(new JsonImp(), listFiles);
		printSize(new KryoImp(), listFiles);
		// printSize(new XmlImp(), listFiles);
		// printSize(new YamlImp(), listFiles);

		long totalTime = System.currentTimeMillis() - startTotal;
		// System.out.println("Objects -> " + ((GetChangesResponseMessage)
		// response).getMetadata().size());
		System.out.println("Total level time --> " + totalTime + " ms");
	}

}
