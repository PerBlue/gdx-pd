package net.mgsx.pd;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;

import net.mgsx.pd.utils.PdRuntimeException;

public class FileUtils {
	
	public static void copyPatchFolder(FileHandle patchFile, FileHandle destDir) {
		File codeFile = new File(Gdx.app.getApplicationListener().getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
		if(codeFile.getName().endsWith(".jar")) {
			String sourceDir = patchFile.parent().path();
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(codeFile);
				Enumeration<? extends ZipEntry> e = jarFile.entries();
				while(e.hasMoreElements()) {
					ZipEntry entry = e.nextElement();
					String name = entry.getName();
					if(name.startsWith(sourceDir)) {
						String filename = name.substring(sourceDir.length());
						File f = new File(destDir.file(), filename);
						if(entry.isDirectory()) {
							if(!ensureDirectoryExists(f)) {
								throw new IOException("Could not create directory: " + f.getAbsolutePath());
							}
						}
						else {
							InputStream entryInputStream = null;
							FileOutputStream fileOutputStream = null;
							try {
								entryInputStream = jarFile.getInputStream(entry);
								fileOutputStream = new FileOutputStream(f);
								StreamUtils.copyStream(entryInputStream, fileOutputStream);
							} finally {
								if(entryInputStream != null) {
									entryInputStream.close();
								}
								if(fileOutputStream != null) {
									fileOutputStream.close();
								}
							}
							
						}
					}
				}
			} catch (IOException e) {
				throw new PdRuntimeException("unable to copy patch", e);
			} finally {
				if(jarFile != null) {
					try {
						jarFile.close();
					} catch (IOException e) {}
				}
			}
		}
		else {
			FileHandle patchParent = patchFile.parent();
			if(patchParent.file().exists()) {
				patchParent.copyTo(destDir);
			}
			else {
				patchParent = Gdx.files.getFileHandle("bin/" + patchParent.path(), patchParent.type());
				if(patchParent.file().exists()) {
					patchParent.copyTo(destDir);
				}
				else {
					throw new PdRuntimeException("Patch parent doesn't exist");
				}
			}
		}
	}
	
	private static boolean ensureDirectoryExists(File f) {
		return f.exists() || f.mkdir();
	}

}