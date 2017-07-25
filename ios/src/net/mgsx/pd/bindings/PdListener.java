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

package net.mgsx.pd.bindings;

import org.robovm.apple.foundation.*;
import org.robovm.objc.annotation.Method;
import org.robovm.rt.bro.annotation.Library;

/**
 * @ protocol PdListener
 */
@Library(Library.INTERNAL)
public interface PdListener extends NSObjectProtocol {
	/**
	 * - (void)receiveBangFromSource:(NSString *)source;
	 */
	public void receiveBangFromSource(NSString source);
	
	/**
	 * - (void)receiveFloat:(float)received
	 * 			 fromSource:(NSString *)source;
	 */
	public void receiveFloat(float received, NSString source);
	
	/**
	 * - (void)receiveSymbol:(NSString *)symbol
	 * 			  fromSource:(NSString *)source;
	 */
	public void receiveSymbol(NSString symbol, NSString source);
	
	/**
	 * - (void)receiveList:(NSArray *)list
	 * 	        fromSource:(NSString *)source;
	 */
	public void receiveList(NSArray<?> list, NSString source);
	
	/**
	 * - (void)receiveMessage:(NSString *)message
	 * 	        withArguments:(NSArray *)arguments
	 * 			   fromSource:(NSString *)source;
	 */
	public void receiveMessage(NSString message, NSArray<?> arguments, NSString source);
	
	public static class Adapter extends NSObject implements PdListener {

		@Override
		@Method(selector = "receiveBangFromSource:")
		public void receiveBangFromSource(NSString source) {}

		@Override
		@Method(selector = "receiveFloat:fromSource:")
		public void receiveFloat(float received, NSString source) {}

		@Override
		@Method(selector = "receiveSymbol:fromSource:")
		public void receiveSymbol(NSString symbol, NSString source) {}

		@Override
		@Method(selector = "receiveList:fromSource:")
		public void receiveList(NSArray<?> list, NSString source) {}

		@Override
		@Method(selector = "receiveMessage:withArguments:fromSource:")
		public void receiveMessage(NSString message, NSArray<?> arguments, NSString source) {}
		
	}
	
}
