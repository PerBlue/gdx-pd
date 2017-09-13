/*******************************************************************************
 * Copyright 2014 Manh Luong   Bui.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package net.mgsx.pd;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import org.puredata.core.PdListener;
import org.robovm.apple.foundation.*;
import org.robovm.rt.bro.Struct;
import org.robovm.rt.bro.ptr.FloatPtr;
import org.robovm.rt.bro.ptr.VoidPtr;

import com.badlogic.gdx.files.FileHandle;

import net.mgsx.pd.PdConfiguration;
import net.mgsx.pd.audio.PdAudioBase;
import net.mgsx.pd.bindings.*;
import net.mgsx.pd.patch.PdPatch;
import net.mgsx.pd.utils.PdRuntimeException;

public class PdAudioiOS extends PdAudioBase {
	
	private static final Runnable POLL_RUNNER = new Runnable() {
		
		@Override
		public void run() {
			PdBase.receiveMessages();
		}
	};

	private final Map<PdListener, net.mgsx.pd.bindings.PdListener> listeners = new HashMap<>();
	private final Map<Integer, VoidPtr> patches = new HashMap<>();
	
	private PdAudioController audioController;
	private PdDispatcher dispatcher;
	private final ScheduledExecutorService messageExecutor;
	private ScheduledFuture<?> pollFuture;
	
	public PdAudioiOS() {
		dispatcher = new PdDispatcher();
		messageExecutor = Executors.newSingleThreadScheduledExecutor();
	}
	
	@Override
	public void create(PdConfiguration config) {
		this.config = config;
		PdBase.setDelegate(dispatcher, false);
		
		audioController = new PdAudioController();
		if(config.inputChannels <= 0) {
			if(audioController.configureAmbientWithSampleRate(config.sampleRate, config.outputChannels, config.mixingEnabled) == PdAudioStatus.PdAudioError)
				throw new PdRuntimeException("Failed to initialize audio components!");
		}
		else {
			if(audioController.configurePlaybackWithSampleRate(config.sampleRate, config.outputChannels, true, config.mixingEnabled) == PdAudioStatus.PdAudioError) {
				throw new PdRuntimeException("Failed to initialize audio components!");
			}
		}
		
		pollFuture = messageExecutor.scheduleWithFixedDelay(POLL_RUNNER, 1, 20, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void release() {
		pause();
		listeners.clear();
		PdBase.setDelegate(null);
		for(VoidPtr pointer : patches.values()) {
			PdBase.closeFile(pointer);
		}
		patches.clear();
		audioController.dealloc();
		messageExecutor.shutdown();
	}
	
	@Override
	public void pause() {
		audioController.setActive(false);
		if(pollFuture != null) {
			pollFuture.cancel(false);
			pollFuture = null;
		}
		messageExecutor.execute(POLL_RUNNER);// Flush pending messages.
	}

	@Override
	public void resume() {
		PdBase.computeAudio(true);
		audioController.setActive(true);
		if(pollFuture == null) {
			pollFuture = messageExecutor.scheduleWithFixedDelay(POLL_RUNNER, 1, 20, TimeUnit.MILLISECONDS);
		}
	}
	
	@Override
	public void addListener(String source, PdListener listener) {
		net.mgsx.pd.bindings.PdListener nativeListener = new ForwardingListener(listener);
		listeners.put(listener, nativeListener);
		dispatcher.addListener(nativeListener, new NSString(source));
	}
	
	@Override
	public void removeListener(String source, PdListener listener) {
		net.mgsx.pd.bindings.PdListener nativeListener = listeners.remove(listener);
		if(nativeListener != null) {
			dispatcher.removeListener(nativeListener, new NSString(source));
		}
	}
	
	@Override
	public PdPatch open(FileHandle file) {
		File f = file.file();
		VoidPtr pointer = PdBase.openFile(new NSString(f.getName()), new NSString(f.getParent()));
		if(pointer == null) {
			throw new PdRuntimeException("Failed to open patch");
		}
		
		int handle = PdBase.dollarZeroForFile(pointer);
		patches.put(handle, pointer);
		
		return new PdPatch(handle);
	}
	
	@Override
	public void close(PdPatch patch) {
		VoidPtr pointer = patches.remove(patch.getPdHandle());
		if(pointer != null) {
			PdBase.closeFile(pointer);
		}
	}
	
	@Override
	public void sendBang(String recv) {
		checkError(PdBase.sendBangToReceiver(new NSString(recv)));
	}
	
	@Override
	public void sendFloat(String recv, float x) {
		checkError(PdBase.sendFloat(x, new NSString(recv)));
	}
	
	@Override
	public void sendSymbol(String recv, String sym) {
		checkError(PdBase.sendSymbol(new NSString(sym), new NSString(recv)));
	}
	
	@Override
	public void sendList(String recv, Object... args) {
		checkError(PdBase.sendList(convertToArray(args), new NSString(recv)));
	}
	
	/**
	 * In the Android version of libpd, it is stated that args is a "list of arguments of type Integer, Float, or String".<br>
	 * <br>
	 * In the iOS version of libpd, we can see from this method of PdBase.m:<br>
	 * <br>
	 * 		static void encodeList(NSArray *list)<br>
	 * <br>
	 * That the list can have NSObject of type NSNumber (from which a float is extracted) or an NSString.<br>
	 * <br>
	 * So what I do internally is to check all elements of args and convert them to NSNumber if they are
	 * a Float or an Integer and to NSString if they are a String.<br>
	 * <br>
	 * It seems that in case of errors, no exceptions are thrown nor return values are returned.<br>
	 */
	private NSArray<?> convertToArray(Object... args) {
		NSArray<NSObject> list = new NSArray<NSObject>();
		for(Object obj : args) {
			if(obj instanceof Integer)
				list.add( NSNumber.valueOf( ((Integer)obj).intValue() ) );
			else if(obj instanceof Float)
				list.add( NSNumber.valueOf( ((Float)obj).floatValue() ) );
			else if(obj instanceof String)
				list.add(new NSString((String)obj));
		}
		return list;
	}

	@Override
	public void sendMessage(String recv, String msg, Object... args) {
		checkError(PdBase.sendMessage(new NSString(msg), convertToArray(args), new NSString(recv)));
	}
	
	@Override
	public int arraySize(String name) {
		return PdBase.arraySizeForArrayNamed(new NSString(name));
	}
	
	/**
	 * TODO I have to rethink this, as I am allocating another array. Each time.
	 */
	@Override
	public void readArray(float[] destination, int destOffset, String source, int srcOffset, int n) {
		int res;
		if (destOffset < 0 || destOffset + n > destination.length)
			res = -2;
		else {
			FloatPtr destBuf = Struct.allocate(FloatPtr.class, n);
			res = PdBase.copyArrayNamed(new NSString(source), srcOffset, destBuf, n);
			for(int index = destOffset; index<n; index++) {
				destination[index] = destBuf.get();
				destBuf.next();
			}
		}
		checkError(res);
	}
	
	/**
	 * TODO I have to rethink this, as I am allocating another array. Each time.
	 */
	@Override
	public void writeArray(String destination, int destOffset, float[] source, int srcOffset, int n) {
		int res;
		if (srcOffset < 0 || srcOffset + n > source.length)
			res = -2;
		else {
			FloatPtr srcBuf = Struct.allocate(FloatPtr.class, n);
			for(int index = srcOffset; index<n; index++) {
				srcBuf.set(source[index]);
				srcBuf.next();
			}
			res = PdBase.copyArray(srcBuf, new NSString(destination), destOffset, n);
		}
		checkError(res);
	}
	
	private static class ForwardingListener extends net.mgsx.pd.bindings.PdListener.Adapter {
		
		private final PdListener javaListener;
		
		ForwardingListener(PdListener javaListener) {
			this.javaListener = javaListener;
		}

		@Override
		public void receiveBangFromSource(NSString source) {
			javaListener.receiveBang(source.toString());
		}
		
		@Override
		public void receiveFloat(float received, NSString source) {
			javaListener.receiveFloat(source.toString(), received);
		}

		@Override
		public void receiveSymbol(NSString symbol, NSString source) {
			javaListener.receiveSymbol(source.toString(), symbol.toString());
		}

		@Override
		public void receiveList(NSArray<?> list, NSString source) {
			javaListener.receiveList(source.toString(), convertArray(list));
		}

		@Override
		public void receiveMessage(NSString message, NSArray<?> arguments, NSString source) {
			javaListener.receiveMessage(source.toString(), message.toString(), convertArray(arguments));
		}
		
		private Object[] convertArray(NSArray<?> list) {
			Object[] javaList = new Object[list.size()];
			for(int i = 0; i < list.size(); i++) {
				NSObject object = list.get(i);
				if(object instanceof NSNumber) {
					javaList[i] = ((NSNumber) object).floatValue();//don't know if float or int
				}
				else {
					javaList[i] = object.toString();
				}
			}
			return javaList;
		}

	}

}
