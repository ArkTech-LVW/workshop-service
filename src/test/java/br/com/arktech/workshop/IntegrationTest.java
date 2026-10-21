package br.com.arktech.workshop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

// Boots the whole application against a real PostgreSQL (Testcontainers).
// Unit tests use plain JUnit and never need this annotation.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Tag("integration")
public @interface IntegrationTest {

    @AliasFor(annotation = SpringBootTest.class)
    WebEnvironment webEnvironment() default WebEnvironment.MOCK;
}
