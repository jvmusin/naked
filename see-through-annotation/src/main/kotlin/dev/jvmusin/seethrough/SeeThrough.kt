package dev.jvmusin.seethrough

/**
 * Marker annotation, which forces the compiler to inline all the usages of this clas,
 * so there will be no boxed value class objects in runtime.
 *
 * Be careful with applying this annotation - there are lots of limitations for the value class:
 * - It should have not parametrized - `value class Value<T>(data: T)` will not work
 * - It should not define or override any methods
 * - It should not contain primitive type as a value
 * - It should not implement any interfaces
 * - It should not have companion object
 *
 * The best way of using the annotation is to apply it to a class without any fancy stuff and without a body,
 * for example:
 *
 * ```
 * @JvmInline
 * @SeeThrough
 * value class MyWrapper(val value: String)
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
annotation class SeeThrough
