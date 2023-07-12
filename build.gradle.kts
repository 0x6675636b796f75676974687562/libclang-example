import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.bytedeco:llvm-platform:15.0.3-1.5.8")
    implementation("org.bytedeco:libffi:3.4.4-1.5.8")
    implementation("org.bytedeco:libffi-platform:3.4.4-1.5.8")
    implementation("org.bytedeco:mkl:2022.2-1.5.8")
    implementation("org.bytedeco:mkl-platform:2022.2-1.5.8")
    implementation("org.bytedeco:mkl-platform-redist:2022.2-1.5.8")
    implementation("com.saveourtool.kompiledb:kompiledb-gson:1.0.1")
    implementation("org.checkerframework:checker-qual:3.33.0")

    /*
     * "2.13" here is the Scala version.
     * Alternative implementations exist with the "_3" suffix.
     */
    implementation("io.shiftleft:overflowdb-traversal_2.13:1.171")
    implementation("io.shiftleft:overflowdb-formats_2.13:1.171")
}

internal val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

internal val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
