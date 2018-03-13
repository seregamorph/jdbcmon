
    mavenLocal()
    ...
    compile "org.jdbcmon:jdbcmon:1.0-SNAPSHOT"

    MonitoringDataSource monitoringDataSource = new MonitoringDataSource(pooledDataSource);
    JdbcTemplate jdbc = new JdbcTemplate(monitoringDataSource);

    @DirtiesContext
    ...
    List<Map<String, ?>> report = monitoringDataSource.report();
    try {
        System.out.println(new ObjectMapper().writeValueAsString(report));
    } catch (JsonProcessingException e) {
        e.printStackTrace();
    }
    System.out.println("-----------------------");
    report.forEach(System.out::println);
    System.out.println("-----------------------");
