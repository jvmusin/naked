# SeeThrough Kotlin Compiler Plugin

The plugin which lets you easily inline your value classes with a single annotation:

```kotlin
import dev.jvmusin.seethrough.SeeThrough

@JvmInline
value class Wrapper(val data: String)

@JvmInline
@SeeThrough
value class InlinedWrapper(val data: String)

fun main() {
    println("Hello, ${Wrapper("World")}!") // Prints "Hello, Wrapper(data=World)!"
    println("Hello, ${InlinedWrapper("World")}!") // Prints "Hello, World!"
}
```

## Motivation

The reason you want to do this is to avoid unnecessary boxing when value classes are used as generic or nullable types.

Applying just this annotation to the value class replaces all the real objects of this type with an underlying value –
with a string in the example above.

## Usage

1. Register the plugin `dev.jvmusin.see-through` in the `plugins` section of your project's `build.gradle.kts` file:
    ```
    plugins {
        ... your own plugins
        id("dev.jvmusin.see-through") version "0.0.1"
    }
    ```

2. Annotate your value classes with `@dev.jvmusin.seethrough.SeeThrough`
3. That's it! All usages of the annotated class will be replaced with usages of the wrapped value!