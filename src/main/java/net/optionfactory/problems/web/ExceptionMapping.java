package net.optionfactory.problems.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.optionfactory.problems.web.ExceptionMapping.ExceptionMappings;

import org.springframework.http.HttpStatus;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ExceptionMappings.class)
public @interface ExceptionMapping {

    HttpStatus code() default HttpStatus.BAD_REQUEST;

    String type() default "GENERIC_PROBLEM";
    String context() default "";
    
    Class<?> exception();

    @Documented
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExceptionMappings {

        ExceptionMapping[] value();
    }
}
