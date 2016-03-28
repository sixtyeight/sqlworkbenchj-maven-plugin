# Maven Plugins in a nutshell, or, How I stopped worrying and learned to love the Mojo

### Mojo / Plugin?
- a Mojo (Maven plain Old Java Object) represents a Maven Goal
- each Maven Plugin consists of at least one Mojo
- a Mojo is a Java class extending AbstractMojo (annotated with @Mojo, the "name" is the goal)
- new instance for every execution by default(?)

### Plugin project
Packaging: maven-plugin

		<!-- maven plugin dependencies -->
		<dependency>
			<groupId>org.apache.maven</groupId>
			<artifactId>maven-plugin-api</artifactId>
			<version>3.0</version>
		</dependency>
		<!-- dependencies to maven project and setttings -->
		<dependency>
			<groupId>org.apache.maven</groupId>
			<artifactId>maven-core</artifactId>
			<version>3.0</version>
		</dependency>
		<!-- dependencies to annotations -->
		<dependency>
			<groupId>org.apache.maven.plugin-tools</groupId>
			<artifactId>maven-plugin-annotations</artifactId>
			<version>3.0</version>
			<scope>provided</scope>
		</dependency>

### Properties
- Mojo related (represented as an entry in a configuration entry)
- ${} are already substituted
- private field (no setter needed)
- annotated with @Parameter
- File, Boolean, String ...
- multiple values possible (e.g. File[] mkdirs)

		<mkdirs>
			<mkdir>${project.build.directory}/foo</mkdir>
			<mkdir>${project.build.directory}/bar</mkdir>
		</mkdirs>

- The name of the elements in the list is not verified, the following snippet will still give 
you two files in the array.

		<mkdirs>
			<sdfsf>${project.build.directory}/foo</sdfsf>
			<ad>${project.build.directory}/bar</ad>
		</mkdirs>


### Components
- Maven related objects (access to the settings and metadata, writeable for e.g. properties!)
- private field (no setter needed)
- annotated with @Component
- e.g. MavenProject, MavenSession, MojoExecution, BuildPluginManager et al.

### XML handling
- Server.getConfiguration() returns Object ?!
- XML Configuration ->  Xpp3Dom configuration = (Xpp3Dom) server.getConfiguration();
- configuration.getChild("driverjar").getValue() returns String, but it is "${h2-driverjar}" ?!

### Programatic expression evaluation (${foo.dotnotation.bar})
- new org.apache.maven.plugin.PluginParameterExpressionEvaluator.PluginParameterExpressionEvaluator(MavenSession, MojoExecution)

### Execute a Mojo from within another Mojo
- https://github.com/TimMoore/mojo-executor

Example: Use the exec-maven-plugin to start a Java VM

		List<String> arguments ....
		
		[snip]

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
				
### Links
* https://maven.apache.org/developers/mojo-api-specification.html
* https://maven.apache.org/plugin-developers/index.html
* https://maven.apache.org/plugin-developers/common-bugs.html
