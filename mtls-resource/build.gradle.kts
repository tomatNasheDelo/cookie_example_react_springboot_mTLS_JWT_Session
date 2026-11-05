plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
}
group = "com.example"
version = "0.0.1-SNAPSHOT"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
repositories { mavenCentral() }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(kotlin("reflect"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }
tasks.withType<Test> { useJUnitPlatform() }
