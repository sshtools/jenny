/**
 * Copyright © 2023 JAdaptive Limited (support@jadaptive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.jenny.boot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Optional;
import java.util.logging.LogManager;

import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.sshtools.bootlace.api.BootContext;
import com.sshtools.bootlace.api.ChildLayer;
import com.sshtools.bootlace.api.GAV;
import com.sshtools.bootlace.api.Repository;
import com.sshtools.bootlace.api.ResolutionMonitor;
import com.sshtools.bootlace.platform.Bootlace;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;

public class Boot {
	
	static {
		var stream = Boot.class.getClassLoader().getResourceAsStream("logging.properties");
		try {
			LogManager.getLogManager().readConfiguration(stream);
//          LOGGER= Logger.getLogger(MyClass.class.getName());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static final AttributedStyle S = AttributedStyle.DEFAULT;

	public final static void main(String[] args) throws Exception {
		
		System.out.println("""
     .-./`)     .-''-.  ,---.   .--.,---.   .--.   ____     __  
     \\ '_ .') .'_ _   \\ |    \\  |  ||    \\  |  |   \\   \\   /  / 
    (_ (_) _)/ ( ` )   '|  ,  \\ |  ||  ,  \\ |  |    \\  _. /  '  
      / .  \\. (_ o _)  ||  |\\_ \\|  ||  |\\_ \\|  |     _( )_ .'   
 ___  |-'`| |  (_,_)___||  _( )_\\  ||  _( )_\\  | ___(_ o _)'    
|   | |   ' '  \\   .---.| (_ o _)  || (_ o _)  ||   |(_,_)'     
|   `-'  /   \\  `-'    /|  (_,_)\\  ||  (_,_)\\  ||   `-'  /      
 \\      /     \\       / |  |    |  ||  |    |  | \\      /       
  `-..-'       `'-..-'  '--'    '--''--'    '--'  `-..-'       
				""");
		
		Bootlace.build().
			withContext(BootContext.named("Jenny")).
			fromINIResource().
			withMonitor(createMonitor()).
			build().
			waitFor();
	}

	static ResolutionMonitor createMonitor() {
		try {
			var terminal = TerminalBuilder.builder().build();
			return new ResolutionMonitor() {
				
				private ProgressBar progress;

				@Override
				public void loadingLayer(ChildLayer layerDef) {
					new AttributedStringBuilder().
						style(S.bold()).
						append("[Loading   ] ").
						style(S.boldOff()).
						append(String.format("%s", layerDef.id())).
						append("   ").
//						append("[").
//						style(S.bold()).
//						append(String.join(", ", layerDef.parents())).
//						style(S.boldOff()).
//						append("]").
						println(terminal);
					terminal.flush();
				}
	
				@Override
				public void have(GAV gav, URI uri, Repository remoteRepository) {
					new AttributedStringBuilder().
						style(S.bold()).
						style(S.foreground(AttributedStyle.GREEN)).
						append("  [Have      ] ").
						style(S.boldOff()).
						style(S.foregroundOff()).
						append(String.format("%s", gav)).
						println(terminal);
					terminal.flush();
				}	
	
				@Override
				public void need(GAV gav, URI uri, Repository remoteRepository) {
					new AttributedStringBuilder().
						style(S.bold()).
						style(S.foreground(AttributedStyle.YELLOW)).
						append("  [Need      ] ").
						style(S.boldOff()).
						style(S.foregroundOff()).
						append(String.format("%s", gav)).
						println(terminal);
					terminal.flush();
				}
	
				@Override
				public void found(GAV gav, URI uri, Repository remoteRepository, Optional<Long> size) {
					new AttributedStringBuilder().
						style(S.foreground(AttributedStyle.GREEN)).
						append("  [Found     ] ").
						style(S.foregroundOff()).
						append(String.format("%s @ %s", gav, remoteRepository.name())).
						println(terminal);
					terminal.flush();

					var pbBldr = new ProgressBarBuilder().
							showSpeed().
							setTaskName(gav.toString()).
							setInitialMax(100).
							setStyle(ProgressBarStyle.COLORFUL_UNICODE_BAR);
			
					progress = pbBldr.build();
				}
	
				@Override
				public void downloading(GAV gav, URI uri, Repository remoteRepository, Optional<Long> bytes) {
					bytes.ifPresent(p -> progress.stepTo(p));
				}
	
				@Override
				public void downloaded(GAV gav, URI uri, Repository remoteRepository) {
					closeProgress();
					new AttributedStringBuilder().
						style(S.bold()).
						style(S.foreground(AttributedStyle.GREEN)).
						append("  [Downloaded] ").
						style(S.boldOff()).
						style(S.foregroundOff()).
						append(String.format("%s", gav)).
						println(terminal);
					terminal.flush();
				}

				@Override
				public void failed(GAV gav, String location, Repository remoteRepository, Exception exception) {
					closeProgress();
					new AttributedStringBuilder().
						style(S.bold()).
						style(S.foreground(AttributedStyle.RED)).
						append("  [Failed    ] ").
						style(S.boldOff()).
						style(S.foregroundOff()).
						append(String.format("%s", exception.getMessage())).
						println(terminal);
					terminal.flush();
				}
	
				private void closeProgress() {
					if(progress != null) {
						try {
							progress.close();
						}
						finally {
							progress = null;
						}
					}
				}
				
			};
		}
		catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}
}
