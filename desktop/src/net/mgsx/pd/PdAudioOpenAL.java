package net.mgsx.pd;

import java.io.File;
import java.io.IOException;

import org.puredata.core.PdBase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudioDevice;
import com.badlogic.gdx.files.FileHandle;

import net.mgsx.pd.audio.PdAudioBase;
import net.mgsx.pd.audio.PdAudioThread;
import net.mgsx.pd.patch.PdPatch;
import net.mgsx.pd.utils.PdRuntimeException;

/**
 * Pd Audio desktop implementation.
 * 
 * Reason of specific implementation is related to Lwjgl audio configuration confliting
 * with audio process synchronization (buffers size for audio recorder / pd process / audio device).
 * It might be unified in the futur.
 * 
 * @author mgsx
 *
 */
public class PdAudioOpenAL extends PdAudioBase
{
	private static class PdAudioThreadOpenAL extends PdAudioThread
	{
		public PdAudioThreadOpenAL(PdConfiguration config) {
			super(config);
		}
		
		@Override
		protected AudioDevice createAudioDevice() 
		{
			// It could be done like this :
			// AudioDevice device = Gdx.audio.newAudioDevice(config.sampleRate, config.outputChannels < 2);
			// but we need to align buffer size : Pd.process and device.write.
			int samplePerFrame = config.bufferSize;
			int samplePerBuffer = samplePerFrame * config.outputChannels;
			int bufferSizeBytes = samplePerBuffer * 2;
			return new OpenALAudioDevice((OpenALAudio)Gdx.audio, config.sampleRate, config.outputChannels<2, bufferSizeBytes, config.bufferCount);
		}

	}
	
	private PdAudioThread thread;
	
	@Override
	public void create(PdConfiguration config) {
		super.create(config);
		
		thread = createThread(config);
		thread.start();
	}
	
	protected PdAudioThread createThread(PdConfiguration config) 
	{
		return new PdAudioThreadOpenAL(config);
	}
	
	@Override
	public void pause() {
		if(thread != null){
			thread.dispose();
			try {
				thread.join();
			} catch (InterruptedException e) {
				// silently fail.
			}
			thread = null;
		}
	}

	@Override
	public void resume() {
		if(thread == null){
			thread = createThread(config);
			thread.start();
		}
	}
	
	@Override
	public PdPatch open(FileHandle file) {
		File cachePatchFile;
		if(file.file().exists()) {
			cachePatchFile = file.file();
		}
		else {
			cachePatchFile = new File(System.getProperty("java.io.tmpdir"), file.path());
			FileHandle cacheDir = Gdx.files.absolute(cachePatchFile.getParent());
			
			FileUtils.copyPatchFolder(file, cacheDir);
		}
		try {
			int handle = PdBase.openPatch(cachePatchFile);
			return new PdPatch(handle);
		} catch (IOException e) {
			throw new PdRuntimeException("unable to open patch", e);
		}
	}

}
