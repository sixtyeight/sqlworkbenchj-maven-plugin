package at.metalab.m68k.sqlworkbenchj_maven_plugin;

/*
 * Copyright 2016. Christian_Sl@gmx.at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

/**
 * Read username/password from server element in the settings and
 * place them in a property for easy access in maven pom.
 */
@Mojo(name = "credentials", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SqlWorkbenchCredentialsMojo extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Parameter(required = true)
	private String credentialProperty;

	@Parameter(required = true)
	private String serverId;

	public void execute() throws MojoExecutionException {
		Server server = mavenSession.getSettings().getServer(serverId);

		if (server == null) {
			String message = "Server '" + serverId
					+ "' could not be found in settings.";
			throw new MojoExecutionException(message);
		}

		String usernameProperty = credentialProperty + ".username";
		String passwordProperty = credentialProperty + ".password";

		Properties properties = mavenProject.getProperties();

		if (properties.containsKey(usernameProperty)) {
			getLog().warn(
					"overwriting previous property value of "
							+ usernameProperty);
		} else {
			getLog().debug("setting property value of " + usernameProperty);
		}

		if (properties.containsKey(passwordProperty)) {
			getLog().warn(
					"overwriting previous property value of "
							+ passwordProperty);
		} else {
			getLog().debug("setting property value of " + passwordProperty);
		}

		properties.setProperty(usernameProperty, server.getUsername());
		properties.setProperty(passwordProperty, server.getPassword());
	
		getLog().info("credentials for " + serverId + " available in " + credentialProperty + " pair.");
	}

}
