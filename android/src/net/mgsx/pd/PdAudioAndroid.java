package net.mgsx.pd;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.core.PdBase;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplicationBase;
import com.badlogic.gdx.files.FileHandle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import net.mgsx.pd.audio.PdAudioBase;
import net.mgsx.pd.patch.PdPatch;
import net.mgsx.pd.utils.PdRuntimeException;

public class PdAudioAndroid extends PdAudioBase
{
	
	private static final Handler handler = new Handler(Looper.getMainLooper());//TODO decide if this is the correct thread
	private static final Runnable pollRunner = new Runnable() {
		@Override
		public void run() {
			PdBase.pollMidiQueue();
			PdBase.pollPdMessageQueue();
			handler.postDelayed(this, 5);
		}
	};
	
	private final Context context;
	
	public PdAudioAndroid() {
		super();
		this.context = ((AndroidApplicationBase)Gdx.app).getContext();
		
		//TODO make this configurable
		FileHelper.trimCache(context);
	}

	@Override
	public PdConfiguration getSuggestedConfigs() {
		AudioParameters.init(context);
		PdConfiguration config = new PdConfiguration();
		config.sampleRate = AudioParameters.suggestSampleRate();
		config.inputChannels = AudioParameters.suggestInputChannels();
		config.outputChannels = AudioParameters.suggestOutputChannels();
		config.bufferSize = AudioParameters.suggestOutputBufferSize(config.sampleRate);
		return config;
	}
	
	@Override
	public void create(PdConfiguration config) {
		super.create(config);
		checkError(PdBase.openAudio(config.inputChannels, config.outputChannels, config.sampleRate));
	}
	
	@Override
	public void pause() {
		PdBase.pauseAudio();
		handler.removeCallbacks(pollRunner);
		handler.post(new Runnable() {
			@Override
			public void run() {
				PdBase.pollMidiQueue();  // Flush pending messages.
				PdBase.pollPdMessageQueue();
			}
		});
	}
	
	@Override
	public void resume() {
		PdBase.computeAudio(true);
		handler.post(pollRunner);
		PdBase.startAudio();
	}
	
	@Override
	public PdPatch open(FileHandle file) {
		File cachePatchFile;
		if(file.type() == FileType.Internal) {
			cachePatchFile = new File(context.getCacheDir(), file.path());
		}
		else {
			cachePatchFile = file.file();
		}
		if(!cachePatchFile.exists()){
	        String patchFolder = new File(file.path()).getParent();
	        try {
		        if(patchFolder == null){
		        	//FileHelper.copyAssetFolder(context.getAssets(), "", context.getCacheDir().getAbsolutePath());
		        	throw new PdRuntimeException("can't copy patch from root directory");
		        }else{
					File cachePatchFolder = new File(context.getCacheDir(), patchFolder);
			        	FileHelper.copyAssetFolder(context.getAssets(), patchFolder, 
								cachePatchFolder.getAbsolutePath());
		        }
	        } catch (IOException e) {
	        	throw new PdRuntimeException("unable to copy patch", e);
	        }
		}
        try {
			int handle = PdBase.openPatch(cachePatchFile);
			return new PdPatch(handle);
		} catch (IOException e) {
			throw new PdRuntimeException("unable to open patch", e);
		}
	}
	
}
