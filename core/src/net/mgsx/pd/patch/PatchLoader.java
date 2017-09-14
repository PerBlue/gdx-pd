package net.mgsx.pd.patch;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.puredata.core.PdListener;

import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import net.mgsx.pd.Pd;
import net.mgsx.pd.utils.PdRuntimeException;

public class PatchLoader extends AsynchronousAssetLoader<PdPatch, PatchLoader.PdPatchParameter>
{
	private PdPatch patch;
	private boolean defaultAsync;
	private volatile CountDownLatch loadingLatch;//using a single latch works since patches are loaded 1 at a time
	
	public PatchLoader(FileHandleResolver resolver, boolean defaultAsync) {
		super(resolver);
		this.defaultAsync = defaultAsync;
		Pd.audio.addListener("patchLoaded", new PdListener.Adapter() {
			@Override
			public void receiveFloat(String source, float x) {
				loadingLatch.countDown();
			}
		});
	}

	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file,
			PatchLoader.PdPatchParameter parameter) {
		boolean checkAsync = (parameter == null && defaultAsync) || (parameter != null && parameter.loadAsync);
		if(checkAsync) {
			loadingLatch = new CountDownLatch(1);
		}
		patch = Pd.audio.open(file);
		if(checkAsync) {
			try {
				loadingLatch.await(30, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new PdRuntimeException("Error waiting for patch load", e);
			}
		}
	}

	@Override
	public PdPatch loadSync(AssetManager manager, String fileName, FileHandle file,
			PatchLoader.PdPatchParameter parameter) {
		return patch;
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
			PatchLoader.PdPatchParameter parameter) {
		// no deps
		return null;
	}
	
	public static class PdPatchParameter extends AssetLoaderParameters<PdPatch> {
		public boolean loadAsync;
	}

}
