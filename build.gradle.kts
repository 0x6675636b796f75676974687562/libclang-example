import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
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
    implementation("org.checkerframework:checker-qual:3.33.0")
}

internal val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

internal val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
