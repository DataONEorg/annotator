package org.dataone.annotator.store;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

public class FileBasedCache {
	
	private static Map<String, File> cache = new HashMap<String, File>();	

	public static void cache(String id, InputStream is) throws IOException {
		if (!cache.containsKey(id)) {
			File file = File.createTempFile("annotator_cache", ".dat");
			file.deleteOnExit();
			IOUtils.copy(is, new FileOutputStream(file));
			cache.put(id, file);
		}
	}
	
	public static InputStream get(String id) throws FileNotFoundException {
		
		if (cache.containsKey(id)) {
			File file = cache.get(id);
			return new FileInputStream(file);
		}
		
		return null;
	}
	
	public static boolean remove(String id) {
		if (cache.containsKey(id)) {
			File file = cache.get(id);
			file.delete();
			cache.remove(id);
			return true;
		}
		return false;
	}
	
	public static void clear() {
		for (File file: cache.values()) {
			file.delete();
		}
		cache.clear();
	}
}
