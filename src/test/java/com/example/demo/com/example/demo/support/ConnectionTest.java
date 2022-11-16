package com.example.demo.com.example.demo.support;


import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// This test exposes a port on the container and dynamically retrieves the
// port in the test method in order to communicate with the container.
// The first test with the hard-coded host and port is expected to fail
// for illustration purposes.

@Testcontainers
public class ConnectionTest {

  Logger log = LoggerFactory.getLogger(ConnectionTest.class);

  RestTemplate restTemplate = new RestTemplate();

  @Container
  static GenericContainer container = new GenericContainer(DockerImageName.parse("kennethreitz/httpbin:latest"))
    .withExposedPorts(80);

  @Test
  @DisplayName("withFixedUrlShouldFail")
  public void test1() {

    String url = "http://localhost:80";
    log.info("URL: {}", url);
    ResponseEntity<String> response
      = restTemplate.getForEntity(url + "/uuid", String.class);
    log.info("Response: {}", response.getBody());
    Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK, () -> "This is not okay.");
  }

  @Test
  @DisplayName("withDynamicUrlShouldPass")
  public void test2() {

    String url = "http://" + container.getHost() + ":" + container.getFirstMappedPort();
    log.info("URL: {}", url);
    ResponseEntity<String> response
      = restTemplate.getForEntity(url + "/uuid", String.class);
    log.info("Response: {}", response.getBody());
    Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK, () -> "This is not okay.");
  }

  @BeforeEach
  public void beforeEachMethod(TestInfo testInfo) {

    log.info("{}Test: {}\nClass instance: {}\nContainer id: {}\n", "\n#####################################\n", testInfo.getDisplayName(), this, container.getContainerId());
  }
}

