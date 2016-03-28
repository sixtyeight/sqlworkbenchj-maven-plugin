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

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

/**
 * Execute SQL Workbech/J in batch mode.
 */
@Mojo(name = "batch", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SqlWorkbenchBatchMojo extends AbstractMojo {

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private MojoExecution mojoExecution;

	// sqlworkbench / java infrastructure settings

	@Parameter(defaultValue = "${sqlwb-jar}", required = true)
	private File sqlWorkbenchJar;

	@Parameter(defaultValue = "java")
	private String javaName;

	@Parameter(defaultValue = "${sqlwb-javahome}")
	private File javaHome;

	// properties for maven features

	@Parameter
	private String connectServerId;

	@Parameter
	private String credentialServerId;

	@Parameter
	private File[] mkdirs;

	// sqlworkbench command line arguments

	@Parameter
	private String script;

	@Parameter
	private String username;

	@Parameter
	private String password;

	@Parameter
	private File driverjar;

	@Parameter
	private String driverclass;

	@Parameter
	private String url;

	@Parameter
	private String command;

	@Parameter
	private Boolean abortOnError;

	@Parameter
	private Boolean readOnly;

	@Parameter
	private Boolean displayResult;

	@Parameter
	private Boolean autocommit;

	@Parameter
	private File logfile;

	@Parameter
	private String varDef;

	@Parameter
	private File varFile;

	@Parameter
	private String variable;

	@Parameter
	private Boolean trimCharData;

	@Parameter
	private Boolean showProgress;

	@Parameter
	private String encoding;

	@Parameter
	private Boolean emptyStringIsNull;

	@Parameter
	private String delimiter;

	@Parameter
	private Boolean bufferResults;

	@Parameter
	private Boolean checkUncommited;

	@Parameter
	private File cleanupSuccess;

	@Parameter
	private File cleanupError;

	@Parameter
	private Boolean ignoreDropErrors;

	@Parameter
	private Long fetchSize;

	@Parameter
	private Boolean hideWarnings;

	@Parameter
	private Boolean feedback;

	@Parameter
	private Boolean consolidateMessages;

	@Parameter
	private String altDelimiter;

	@Parameter
	private Boolean logAllStatements;

	public void execute() throws MojoExecutionException {
		if (!sqlWorkbenchJar.exists()) {
			throw new MojoExecutionException(
					"sqlWorkbenchJar could not be found at: "
							+ sqlWorkbenchJar.getAbsolutePath());
		} else {
			getLog().debug(
					"sqlWorkbenchJar: " + sqlWorkbenchJar.getAbsolutePath());
		}

		if (command == null && script == null) {
			getLog().error("either 'command' or 'script' must be set");
			return;
		} else if (command != null && script != null) {
			getLog().error("only 'command' or 'script' may be set");
			return;
		}

		if (mkdirs != null) {
			for (File dir : mkdirs) {
				if (!dir.isAbsolute()) {
					throw new MojoExecutionException(
							"Directory must be absolute: " + dir.getName());
				}

				if (!dir.mkdirs()) {
					getLog().error(
							"Could not create directories: "
									+ dir.getAbsolutePath());
				} else {
					getLog().info("Created directory: " + dir.getAbsolutePath());
				}
			}
		}

		String lUsername = null;
		String lPassword = null;

		// connect via maven server element settings
		if (connectServerId != null) {
			Server server = mavenSession.getSettings().getServer(
					connectServerId);
			if (server == null) {
				throw new MojoExecutionException("connectServerId: server "
						+ connectServerId + " not found");
			}

			lUsername = server.getUsername();
			lPassword = server.getPassword();

			Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
			if (configuration != null) {
				url = eval(getChildValue(configuration, "url"));
				driverclass = eval(getChildValue(configuration, "driverclass"));

				String strDriverjar = eval(getChildValue(configuration,
						"driverjar"));
				if (strDriverjar != null) {
					driverjar = new File(strDriverjar);
				}
			}
		}

		// credentials via maven server element settings
		if (credentialServerId != null) {
			Server server = mavenSession.getSettings().getServer(
					credentialServerId);
			if (server == null) {
				throw new MojoExecutionException("credentialServerId: server "
						+ credentialServerId + " not found");
			}

			lUsername = server.getUsername();
			lPassword = server.getPassword();
		}

		if (username == null) {
			username = lUsername;
		}

		if (password == null) {
			password = lPassword;
		}

		List<String> args = new ArrayList<String>();

		addIfSet(args, "script", script);
		addIfSet(args, "username", username);
		addIfSet(args, "password", password);
		addIfSet(args, "driverjar", driverjar);
		addIfSet(args, "driverclass", driverclass);
		addIfSet(args, "url", url);
		addIfSet(args, "command", command);
		addIfSet(args, "abortOnError", abortOnError);
		addIfSet(args, "readOnly", readOnly);
		addIfSet(args, "displayResult", displayResult);
		addIfSet(args, "autocommit", autocommit);
		addIfSet(args, "logfile", logfile);
		addIfSet(args, "varFile", varFile);
		addIfSet(args, "varDef", varDef);
		addIfSet(args, "variable", variable);
		addIfSet(args, "trimCharData", trimCharData);
		addIfSet(args, "showProgress", showProgress);
		addIfSet(args, "encoding", encoding);
		addIfSet(args, "emptyStringIsNull", emptyStringIsNull);
		addIfSet(args, "delimiter", delimiter);
		addIfSet(args, "bufferResults", bufferResults);
		addIfSet(args, "checkUncommited", checkUncommited);
		addIfSet(args, "cleanupError", cleanupError);
		addIfSet(args, "cleanupSuccess", cleanupSuccess);
		addIfSet(args, "fetchSize", fetchSize);
		addIfSet(args, "ignoreDropErrors", ignoreDropErrors);
		addIfSet(args, "hideWarnings", hideWarnings);
		addIfSet(args, "feedback", feedback);
		addIfSet(args, "consolidateMessages", consolidateMessages);
		addIfSet(args, "altDelimiter", altDelimiter);
		addIfSet(args, "logAllStatements", logAllStatements);

		if (args.isEmpty()) {
			getLog().warn(
					"no arguments provided. sqlworkbench will not be started");
		} else {
			startSqlWorkbench(args);
		}
	}

	private String getChildValue(Xpp3Dom dom, String name) {
		if (dom.getChild(name) != null) {
			return dom.getChild(name).getValue();
		}
		return null;
	}

	private String eval(String expr) throws MojoExecutionException {
		if (expr == null) {
			return null;
		}

		ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(
				mavenSession, mojoExecution);

		try {
			return (String) evaluator.evaluate(expr);
		} catch (Exception e) {
			getLog().error(e);
			throw new MojoExecutionException("Could not evaluate: " + expr);
		}
	}

	private void addIfSet(List<String> args, String name, Long value) {
		if (value != null) {
			addIfSet(args, name, value.toString());
		}
	}

	private void addIfSet(List<String> args, String name, Boolean value) {
		if (value != null) {
			addIfSet(args, name, value.toString());
		}
	}

	private void addIfSet(List<String> args, String name, File value) {
		if (value != null) {
			addIfSet(args, name, value.getAbsolutePath());
		}
	}

	private void addIfSet(List<String> args, String name, String value) {
		if (value != null) {
			String argValue = String.format("-%s=%s", name, value);
			args.add(argValue);

			getLog().debug("added: " + argValue);
		}
	}

	private void startSqlWorkbench(List<String> args)
			throws MojoExecutionException {
		runJavaWithExecExecMojo("workbench.WbStarter", sqlWorkbenchJar, args);
	}

	private void runJavaWithExecExecMojo(String mainClass, File jar,
			List<String> args) throws MojoExecutionException {
		List<Element> arguments = new ArrayList<Element>();

		// System Properties
		for (String property : new LinkedList<String>()) {
			arguments.add(element("argument", property));
		}

		arguments.add(element("argument", "-classpath"));
		if (jar != null) {
			arguments.add(element("argument", jar.getAbsolutePath()));
		} else {
			// special, this will add all dependencies
			arguments.add(element("classpath"));
		}

		// The main class
		arguments.add(element("argument", mainClass));

		// Arguments passed into the main class via main()
		if (args != null) {
			for (String arg : args) {
				arguments.add(element("argument", arg));
			}
		}

		List<Element> configuration = new ArrayList<Element>();

		StringBuilder javaExecutable = new StringBuilder();
		if (javaHome != null) {
			javaExecutable.append(javaHome.getAbsolutePath() + "/");
		}
		javaExecutable.append(javaName);

		configuration.add(element("executable", javaExecutable.toString()));
		configuration.add(element(name("arguments"), asElements(arguments)));

		getLog().info(
				"starting " + mainClass + " with " + javaExecutable.toString());

		executeMojo(
				plugin(groupId("org.codehaus.mojo"),
						artifactId("exec-maven-plugin"), version("1.4.0")),
				goal("exec"), configuration(asElements(configuration)),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
	}

	/*
	 * private void runJavaWithExecJavaMojo(String mainClass, List<String> args)
	 * throws MojoExecutionException { List<Element> arguments = new
	 * ArrayList<Element>(); for (String arg : args) {
	 * arguments.add(element("argument", arg)); }
	 * 
	 * List<Element> configuration = new ArrayList<Element>();
	 * configuration.add(element(name("mainClass"), mainClass));
	 * configuration.add(element(name("arguments"), asElements(arguments)));
	 * 
	 * executeMojo( plugin(groupId("org.codehaus.mojo"),
	 * artifactId("exec-maven-plugin"), version("1.4.0")), goal("java"),
	 * configuration(asElements(configuration)),
	 * executionEnvironment(mavenProject, mavenSession, pluginManager)); }
	 */
	private static Element[] asElements(List<Element> elements) {
		Element[] r = new Element[elements.size()];
		int i = 0;
		for (Element element : elements) {
			r[i] = element;
			i++;
		}

		return r;
	}

}
