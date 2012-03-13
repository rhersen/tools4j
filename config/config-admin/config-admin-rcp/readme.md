This project uses tycho and PDE which deserve some explanation.

### Important

Having eclipse RCP projects and tools4j projects in same workspace does
not work at the moment. The reason is @ServiceProvider(service = ....class)
that generate a META-INF/services files into target directory which is
not recognized by Eclipse. support-osgi must find META-INF/services in order
to register providers as OSGi services. RCP application will fail otherwise
when doing Lookup.get().lookup(...).

Tycho is also not able to recognize external maven dependencies in the
same reactor build. I.e RCP builds must be separate from their external
OSGi bundles.

### Create workspace.

1) Build local maven osgi bundle dependencies to maven repository.

2) mvn eclipse:eclipse from config-admin-rcp/pom.xml project.

3) mvn install from config-admin-rcp/pom.xml project.

   OBSERVE: eclipse:eclipse and install cannot be run in same
   reactor build since eclipe:eclipse removes the p2 repository
   config-admin-rcp-product/target/repository which breaks all plugins!

4) Import plugin projects into eclipse (config-admin-rcp/*)

5) Choose target platform...

   Window > Preferences > Plug-in Development > Target Platform > config-admin-rcp

   Apply > Reload (in same window)

6) Run "config-admin-rcp" run configuration and application should start.

### HOWTO: Add new osgi bundle dependency accessible as a plug-in.

1) Add dependency to config-admin-rcp/pom.xml (do not rely on transitivity).

    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <scope>compile</scope>
      <version>0.9.30</version>
    </dependency>

2) Add dependency manually to config-admin-rcp-feature/feature.xml

  `<plugin id="ch.qos.logback.core" version="0.9.30"/>`

  `<plugin id="ch.qos.logback.classic" version="0.9.30"/>`

2) mvn clean install from config-admin-rcp/pom.xml. Will recreated
   p2 repository to config-admin-rcp-product/target/repository with new deps.

3) Reload target definition so eclipse find the new osgi bundle deps.

   Window > Preferences > Plug-in Development > Target Platform > Reload ...

4) Select all project in Eclipse right-click > Maven > Update projects...

5) Add new osgi plugins to config-admin-rcp-plugin/config-admin-rcp.launch.
   Run Configuration > Plug-ins > Add Required Plugins

### HOWTO: Plugins get "out of sync" what to do?

Very strange error that happens sometimes. The only know workaround seems to be to 
restart Eclipse and reimport all project again. Worst case create a new workspace.