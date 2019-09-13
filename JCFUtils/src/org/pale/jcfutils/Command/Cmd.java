package org.pale.jcfutils.Command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface Cmd {
	String name() default "";
	String desc();
	String usage();
	String permission() default "";
	int argc() default -1; // means "varargs"
	boolean player() default false; // requires a player
}
