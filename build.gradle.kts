import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


group = "me.uj347"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.5.31"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}
dependencies {
    testImplementation(kotlin("test"))
    implementation("net.bramp.ffmpeg:ffmpeg:0.6.2")
    implementation ("com.google.dagger:dagger:2.40.5")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    implementation("commons-io:commons-io:2.11.0")
    implementation(files("src/libs/CLI.jar"))
    testImplementation("org.mockito:mockito-inline:4.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:4.3.1")
    testImplementation("org.mockito:mockito-core:4.3.1")
    testImplementation ("org.mockito.kotlin:mockito-kotlin:4.0.0")

}

tasks.jar{
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
    archiveFileName.set("NyaMerger.jar")// Naming the jar
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to "com.uj.nyamerger.MainKt")) } // Provided we set it up in the application plugin configuration
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
    from(contents)
}



tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}