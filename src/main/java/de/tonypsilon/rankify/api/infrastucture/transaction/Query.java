package de.tonypsilon.rankify.api.infrastucture.transaction;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that are transactional queries.
 * This annotation ensures that the method is executed within a read-only transaction context.
 * It is typically used for methods that retrieve data without modifying the state of the application.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Transactional(readOnly = true)
public @interface Query {
}
