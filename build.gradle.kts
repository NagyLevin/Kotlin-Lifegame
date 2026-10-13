plugins {
    kotlin("jvm") version "2.4.21"
    application
}
group = "lifegame"
version = "1.0.0"
repositories { mavenCentral() }
kotlin { jvmToolchain(21) }
application { mainClass.set("lifegame.MainKt") }

