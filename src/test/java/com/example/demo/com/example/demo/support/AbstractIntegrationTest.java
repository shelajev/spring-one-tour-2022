package com.example.demo.com.example.demo.support;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.terma.javaniotcpproxy.StaticTcpProxyConfig;
import com.github.terma.javaniotcpproxy.TcpProxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import net.bytebuddy.description.type.TypeList;
import org.checkerframework.checker.units.qual.K;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {
//    "spring.datasource.url=jdbc:tc:postgresql:14-alpine:///"
  }
)
public class AbstractIntegrationTest {
//
  static Network network = Network.newNetwork();


  static GenericContainer<?> redis = new GenericContainer<>("redis:6-alpine")
    .withExposedPorts(6379).withNetwork(network);

  static ToxiproxyContainer toxiproxyContainer = new ToxiproxyContainer("shopify/toxiproxy:2.1.0").withNetwork(network);;

  static PostgreSQLContainer<?> postgreSQLContainer =
    new PostgreSQLContainer<>("postgres:14-alpine")
      .withCopyFileToContainer(
        MountableFile.forClasspathResource("schema.sql"), "/docker-entrypoint-initdb.d/");
//
  static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:5.4.6"));

  @DynamicPropertySource
  public static void setupThings(DynamicPropertyRegistry registry) throws IOException {
    Startables.deepStart(redis, kafka, postgreSQLContainer, toxiproxyContainer).join();

    ToxiproxyContainer.ContainerProxy redis1 = toxiproxyContainer.getProxy(redis, 6379);

    registry.add("spring.redis.host", redis1::getContainerIpAddress);
    registry.add("spring.redis.port", redis1::getProxyPort);

    redis1.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 2000);

    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  protected RequestSpecification requestSpecification;

  @LocalServerPort
  protected int localServerPort;

  @BeforeEach
  public void setUpAbstractIntegrationTest() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    requestSpecification = new RequestSpecBuilder()
      .setPort(localServerPort)
      .addHeader(
        HttpHeaders.CONTENT_TYPE,
        MediaType.APPLICATION_JSON_VALUE
      )
      .build();
  }


}
