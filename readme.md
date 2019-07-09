
Install the library locally (does not present in global repos):

    mvn clean install

Add artifact dependency to your project

    mavenLocal()
    ...
    compile "org.jdbcmon:jdbcmon:1.0-SNAPSHOT"

or

    <dependency>
        <groupId>org.jdbcmon</groupId>
        <artifactId>jdbcmon</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

Wrap your DataSource to MonitoringDataSource

    MonitoringDataSource monitoringDataSource = new MonitoringDataSource(pooledDataSource);
    JdbcTemplate jdbc = new JdbcTemplate(monitoringDataSource);

Print the SQL report

    ...
    List<Map<String, ?>> report = monitoringDataSource.report();
    String strReport = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .writeValueAsString(report);
    System.out.println(strReport);

