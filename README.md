# Naked Kotlin Compiler Plugin

The plugin which lets you easily inline your value classes with a single annotation:

```kotlin
import dev.jvmusin.naked.Naked

@JvmInline
value class Wrapper(val data: String)

@JvmInline
@Naked
value class InlinedWrapper(val data: String)

fun main() {
    println("Hello, ${Wrapper("World")}!") // Prints "Hello, Wrapper(data=World)!"
    println("Hello, ${InlinedWrapper("World")}!") // Prints "Hello, World!"
}
```

## Motivation

The reason you may want to inline value classes entirely is to avoid unnecessary boxing when value classes are used as
generic or nullable types.

Applying `@Naked` annotation to the value class causes all the real objects of this type to be replaced with a
wrapped value – with a string in the example above.

## Usage

1. Register the plugin `dev.jvmusin.naked` in the `plugins` section of your project's `build.gradle.kts` file:
    ```
    plugins {
        ... your other plugins
        id("dev.jvmusin.naked") version "0.0.1"
    }
    ```

2. Annotate your value classes with `@dev.jvmusin.naked.Naked`
3. That's it! All usages of the annotated class will be replaced with usages of the wrapped value!

You can disable the plugin using `naked` extension in `build.gradle.kts` file:

```kotlin
naked {
    enabled = false
}
```