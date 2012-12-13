package com.hts;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.json.JSONObject;

import com.wowza.wms.logging.WMSLoggerFactory;

/**
 * GET reads a resource PUT creates a new resource DELETE removes the resources. POST updates an existing resource or
 * creates a new resource.
 */

public class WowzaRESTService {

	private static Scope scope = new Scope();

	/**
	 * 
	 * @return JSON FLV string
	 */
	public String getFLVs() {

		String result = "[Wowza] Getting all flvs";
		WMSLoggerFactory.getLogger(null).info(result);

		Map<String, Map<String, Object>> m = getListOfAvailableFLVs();

		JSONObject json = new JSONObject();
		json.accumulateAll(m);

		result = json.toString();

		return result;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Resource[] resources = new Scope().getResources("mp4");
		resources = scope.getResources("flv");

		Map<String, Map<String, Object>> map = new WowzaRESTService().getListOfAvailableFLVs();

		String json = new WowzaRESTService().getFLVs();
	}

	/**
	 * Getter for property 'listOfAvailableFLVs'.
	 * 
	 * @return Value for property 'listOfAvailableFLVs'.
	 */
	public Map<String, Map<String, Object>> getListOfAvailableFLVs() {

		Map<String, Map<String, Object>> filesMap = new HashMap<String, Map<String, Object>>();
		try {
			WMSLoggerFactory.getLogger(null).info("Getting the media files");
			addToMap(filesMap, scope.getResources(".flv"));
			addToMap(filesMap, scope.getResources(".f4v"));
			addToMap(filesMap, scope.getResources(".mp3"));
			addToMap(filesMap, scope.getResources(".mp4"));
			addToMap(filesMap, scope.getResources(".m4a"));
			addToMap(filesMap, scope.getResources(".3g2"));
			addToMap(filesMap, scope.getResources(".3gp"));
		}
		catch (IOException e) {
			WMSLoggerFactory.getLogger(null).error("", e);
		}
		return filesMap;
	}

	private void addToMap(Map<String, Map<String, Object>> filesMap, Resource[] files) throws IOException {
		if (files != null) {
			for (Resource flv : files) {
				File file = flv.getFile();
				Date lastModifiedDate = new Date(file.lastModified());
				String lastModified = formatDate(lastModifiedDate);
				String flvName = flv.getFile().getName();
				
				//some Wowza crazy url notation for streams URLs like mp4:sample.mp4
				//iyuvchen: not adding the rest as we need to play with other file types and test them.
				if(flvName.endsWith("flv"))
					flvName = "flv:"+flvName;
				if(flvName.endsWith("mp4"))
					flvName = "mp4:"+flvName;
				//TODO add similar Wowza specific URL handles for the media types mp3, f4v, 3gp. 
				
				
				String flvBytes = Long.toString(file.length());
				if (WMSLoggerFactory.getLogger(null).isDebugEnabled()) {
					WMSLoggerFactory.getLogger(null).debug("flvName: {}" + flvName);
					WMSLoggerFactory.getLogger(null).debug("lastModified date: {}" + lastModified);
					WMSLoggerFactory.getLogger(null).debug("flvBytes: {}" + flvBytes);
					WMSLoggerFactory.getLogger(null).debug("-------");
				}
				Map<String, Object> fileInfo = new HashMap<String, Object>();
				fileInfo.put("name", flvName);
				fileInfo.put("lastModified", lastModified);
				fileInfo.put("size", flvBytes);
				filesMap.put(flvName, fileInfo);
			}
		}
	}

	private String formatDate(Date date) {
		SimpleDateFormat formatter;
		String pattern = "dd/MM/yy H:mm:ss";
		Locale locale = new Locale("en", "US");
		formatter = new SimpleDateFormat(pattern, locale);
		return formatter.format(date);
	}

}

class Resource {
	File f;

	public Resource(File f) {
		this.f = f;
	}

	public File getFile() {
		return f;

	}
}

class Scope {
	public static final String WOWZAHOMEDIR = "WMSAPP_HOME";

	public Resource[] getResources(String pattern) {
		String wowzaHomeDir = System.getenv(WOWZAHOMEDIR);
		String storageDir = "content";

		if (wowzaHomeDir == null) {
			// TODO make oflaDemo as jar and import exception in order to throw hts exception
			throw new RuntimeException("Environmnet variable WMSAPP_HOME is empty");
		}
		else if (!wowzaHomeDir.endsWith("/") && !wowzaHomeDir.endsWith("\\")) {
			storageDir = wowzaHomeDir + File.separatorChar + storageDir;
		}
		else {
			storageDir = wowzaHomeDir + storageDir;
		}
		File fStorageDir = new File(storageDir);

		ArrayList<Resource> res = new ArrayList<Resource>();

		for (File file : fStorageDir.listFiles()) {
			if (file.isFile() & file.getPath().endsWith(pattern)) {
				res.add(new Resource(file));
			}
		}
		Resource[] resources = res.toArray(new Resource[res.size()]);

		return resources;

	}
}
