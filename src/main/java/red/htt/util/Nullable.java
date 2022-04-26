package red.htt.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 表示参数，字段或方法的返回值可以为null。
 * 这是一个标记注释，没有特定属性。
 *
 * @author yui
 * @date 2020/3/6 16:15
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Nullable {
}