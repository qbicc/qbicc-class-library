package cc.quarkus.qccrt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that this class tracks the associated file name.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(Tracking.List.class)
public @interface Tracking {
    String value();

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.TYPE)
    @interface List {
        Tracking[] value();
    }
}
