# otaibe-commons-quarkus
Commons utility classes for quarkus based projects. Some of them are very usefull if you plan to migrate from Spring Boot to Quarkus. You can find more details about similar migration in this article: [Spring2quarkus — Spring Boot to Quarkus Migration](https://dzone.com/articles/spring2quarkus-spring-boot-to-quarkus-migration)

## Some tips

### Plug-in Quarkus Based Eureka Client

Add this lines to your pom.xml file:

        <dependency>
            <groupId>org.otaibe.commons.quarkus</groupId>
            <artifactId>otaibe-commons-quarkus-core</artifactId>
            <version>${org.otaibe.commons.quarkus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.otaibe.commons.quarkus</groupId>
            <artifactId>otaibe-commons-quarkus-eureka-client</artifactId>
            <version>${org.otaibe.commons.quarkus.version}</version>
        </dependency>
        <dependency>
            <groupId>org.otaibe.commons.quarkus</groupId>
            <artifactId>otaibe-commons-quarkus-actuator</artifactId>
            <version>${org.otaibe.commons.quarkus.version}</version>
        </dependency>

Also add Git Commit plugin to your pom.xml:

            <plugin>
                <groupId>pl.project13.maven</groupId>
                <artifactId>git-commit-id-plugin</artifactId>
                <version>3.0.1</version>
                <executions>
                    <execution>
                        <id>get-the-git-infos</id>
                        <goals>
                            <goal>revision</goal>
                        </goals>
                        <!-- *NOTE*: The default phase of revision is initialize, but in case you want to change it, you can do so by adding the phase here -->
                        <phase>initialize</phase>
                    </execution>
                    <execution>
                        <id>validate-the-git-infos</id>
                        <goals>
                            <goal>validateRevision</goal>
                        </goals>
                        <!-- *NOTE*: The default phase of validateRevision is verify, but in case you want to change it, you can do so by adding the phase here -->
                        <phase>package</phase>
                    </execution>
                </executions>
                <configuration>
                    <generateGitPropertiesFile>true</generateGitPropertiesFile>
                    <generateGitPropertiesFilename>${project.build.outputDirectory}/META-INF/resources/git.properties</generateGitPropertiesFilename>
                    <format>json</format>
                    <includeOnlyProperties>
                        <includeOnlyProperty>^git.commit.id$</includeOnlyProperty>
                        <includeOnlyProperty>^git.commit.time$</includeOnlyProperty>
                        <includeOnlyProperty>git.branch</includeOnlyProperty>
                        <includeOnlyProperty>git.dirty</includeOnlyProperty>
                        <includeOnlyProperty>git.build.time</includeOnlyProperty>
                    </includeOnlyProperties>
                </configuration>
            </plugin>

This will add Quarkus Based Eureka Client together with the ``/info`` and ``/metrics`` endpoints.
If you want to have and ``/health`` endpoint you can add it directly from the quarkus:

        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-smallrye-health</artifactId>
        </dependency>

Now you will have an working Quarkus Based Eureka Client.

You can configure it by adding this configuration in your application.properties file:

        #hostname to register with eureka client (localhost is for dev environment only)
        eureka.instance.hostname=localhost
        #spring boot style
        eureka.client.serviceUrl.defaultZone=http://eureka-staging:1111/eureka/
        eureka.server.path=/apps

Here is a sample code how to use the Quarkus Based Eureka Client:

    @Inject
    @Getter
    @Setter
    EurekaClient eurekaClient;
    
    ...
        String homePageUrl = getEurekaClient().getNextServer(serviceUrl)
        

