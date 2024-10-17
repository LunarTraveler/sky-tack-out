package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {

    // 数据库操作类型 UPDATE INSERT
    // 使用的是OperationType这个枚举类型的值，这样就更加规范，不至于随便或是写错类型
    OperationType value();

}
