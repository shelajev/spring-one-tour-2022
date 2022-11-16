package com.example.demo.com.example.demo.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

// This test shows how to access container logs (all or stdout/stderr separately)
// either all at once or as a stream using Slf4jLogConsumer

// This test also illustrates:
//   - Manual control of container create/start
//   - Setting/getting runtime environment variables
//   - Setting container start command

@Testcontainers
public class ContainerLogsTest {

  Logger log = LoggerFactory.getLogger(ContainerLogsTest.class);

  @Container
  GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("alpine:3.16.3"))
    .withEnv("GREETING", "Hello, world!")
    .withCommand("/bin/sh", "-c",
          "echo -e \">> Starting up with GREETING=$GREETING\" && " +
          "echo -e \">> Oops! Something went wrong\" 1>&2")

    // this is a short lived container so we only check the Docker container status,
    // and not that the service within is started or ports are open
    .withStartupCheckStrategy(new OneShotStartupCheckStrategy());


  @Test
  @DisplayName("get_logs_as_string")
  public void test1() {
    String logs = container.getLogs();
    log.info("Container logs (all):\n{}", logs);
    Assertions.assertTrue(logs.contains("Starting up with GREETING"));
    log.info("Container logs (stdout only):\n{}", container.getLogs(STDOUT));
    log.error("Container logs (stderr only):\n{}", container.getLogs(STDERR));

    Assertions.assertTrue(container.getEnvMap().get("GREETING").equalsIgnoreCase("Hello, world!"));
  }

  @Test
  @DisplayName("get_logs_as_stream")
  public void test2() {

    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log).withSeparateOutputStreams();
    container.followOutput(logConsumer);

    Assertions.assertTrue(container.getEnvMap().get("GREETING").equalsIgnoreCase("Hello, world!"));
  }
}