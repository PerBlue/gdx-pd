package net.mgsx.pd.patch;

import java.util.concurrent.CountDownLatch;

import org.puredata.core.PdListener;

import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import net.mgsx.pd.Pd;
import net.mgsx.pd.utils.PdRuntimeException;

public class PatchLoader extends AsynchronousAssetLoader<PdPatch, AssetLoaderParameters<PdPatch>>
{
	private PdPatch patch;
	private boolean asyncPatches;
	private volatile int loadingPatch;
	private volatile CountDownLatch loadingLatch;
	
	public PatchLoader(FileHandleResolver resolver, boolean enableAsync) {
		super(resolver);
		asyncPatches = enableAsync;
		if(enableAsync) {
			Pd.audio.addListener("patchLoaded", new PdListener.Adapter() {
				@Override
				public void receiveFloat(String source, float x) {
					if(MathUtils.isEqual(loadingPatch, x)) {
						loadingLatch.countDown();
					}
				}
			});
		}
	}

	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file,
			AssetLoaderParameters<PdPatch> parameter) {
		patch = Pd.audio.open(file);
		if(asyncPatches) {
			loadingPatch = patch.getPdHandle();
			loadingLatch = new CountDownLatch(1);
			try {
				loadingLatch.await();
			} catch (InterruptedException e) {
				throw new PdRuntimeException("Error waiting for patch load", e);
			}
		}
	}

	@Override
	public PdPatch loadSync(AssetManager manager, String fileName, FileHandle file,
			AssetLoaderParameters<PdPatch> parameter) {
		return patch;
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
			AssetLoaderParameters<PdPatch> parameter) {
		// no deps
		return null;
	}

}
