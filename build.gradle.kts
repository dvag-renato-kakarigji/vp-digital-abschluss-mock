import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.com.google.common.collect.Lists
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage


plugins {
    idea
    jacoco
    java
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.spring") version "1.5.20"
    kotlin("plugin.jpa") version "1.5.20"
    kotlin("kapt") version "1.5.20"
    id("org.springframework.boot") version "2.5.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.sonarqube") version "3.3"
}

group = "com.dvag"
version = "0.0.1"
description = "vp-digital-abschluss-mock"
java.sourceCompatibility = JavaVersion.VERSION_11

val kotestVersion by extra("4.6.1")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("http://repo.spring.io/milestone/")
    }
    maven {
        url = uri("http://repo.spring.io/release/")
    }
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
    repositories.all {
        if(this is MavenArtifactRepository) {
            isAllowInsecureProtocol=true
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2020.0.3")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.slf4j:log4j-over-slf4j")
    implementation("net.logstash.logback:logstash-logback-encoder:6.6")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.zalando:problem-spring-webflux:0.26.2")
    implementation("org.zalando:logbook-spring-boot-webflux-autoconfigure:2.11.0")
    implementation("io.swagger:swagger-annotations:1.5.21")
    implementation("javax.validation:validation-api:2.0.1.Final")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring-jvm:4.4.3")
    testImplementation("org.jacoco:org.jacoco.build:0.8.7")
}

tasks.jar {
    destinationDirectory.set(buildDir)
}

tasks.bootJar {
    destinationDirectory.set(buildDir)
}

tasks.withType<ProcessResources> {
//    dependsOn(tasks.findByName("generateGitProperties"))
//    mustRunAfter(tasks["generateGitProperties"])
    filesMatching("application.yaml") {
        expand(project.properties)
    }
    filesMatching("logback-spring.xml") {
        expand(project.properties)
    }
}

tasks.create("ensureResources") {
    doLast {
        mkdir("$buildDir/classes/java/main")
        mkdir("$buildDir/classes/java/test")
        mkdir("$buildDir/resources/test")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(
        tasks.findByName("processResources"),
        tasks.findByName("ensureResources"),
    )
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.assemble{
    shouldRunAfter(tasks["check"])
    dependsOn(tasks["copyDependencies"])
}

tasks.register<Copy>("copyDependencies") {
    dependsOn(tasks["compileJava"])
    dependsOn(tasks["compileKotlin"])
    from(configurations.compileClasspath,configurations.runtimeClasspath)
    into("$buildDir/dependency")
}.get()


tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events(STARTED, PASSED, FAILED, STANDARD_ERROR, SKIPPED)
        exceptionFormat = FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<BootBuildImage> {
    builder = "paketobuildpacks/builder:tiny"
    environment = mapOf("BP_NATIVE_IMAGE" to "true")
}

// jacoco test reports excluded packages
val jacocoExcludes = listOf(
    "**/model/**",
    "**/models/**",
    "**/generated/**",
    "**/config/**",
    "**/configuration/**",
    "**/api/**",
    "**/apis/**",
    "**/**Model**",
    "**/**Config**",
    "**/**Configuration**",
    "**/**Api**",
    "**/**ErrorResponse**",
    "**/**TrackingService**",
    "**/**Resource**",
)

tasks.jacocoTestReport{
    dependsOn(tasks.check)
    classDirectories.setFrom(fileTree("build/classes/kotlin/main").apply {
        exclude(jacocoExcludes)
    }, fileTree("build/classes/java/main").apply {
        exclude(jacocoExcludes)
    })
    sourceDirectories.setFrom(project.fileTree(".") {
        exclude(jacocoExcludes)
    })
    executionData.setFrom(fileTree("build/jacoco") {
        include("test.exec", "integrationTest.exec")
    })
    reports {
        xml.required.set(true)
    }
}
tasks.sonarqube {
    dependsOn(tasks.jacocoTestReport)
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

sonarqube {
    properties {
        property("sonar.host.url", System.getenv("SONAR_URL"))
        property("sonar.login", System.getenv("SONAR_PAT"))
        property("sonar.projectKey", "vp-digital-abschluss-mock")
        property("sonar.projectName", "vp-digital-abschluss-mock")
        property("sonar.sources", "$projectDir/src/main/kotlin")
        property("sonar.language", "kotlin")
        property("sonar.exclusions", "**/generated/**/*, **/model/**/*, **/*Spec.kt, **/**Config**, **/**config**\"")
        property("sonar.coverage.exclusions", "**/generated/**/*, **/model/**/*, **/, **Config**, **/**config**")
        property("sonar.java.binaries", "$buildDir/classes/kotlin")
        property("sonar.junit.reportPath", "$buildDir/test-results")
        property("sonar.java.coveragePlugin", "jacoco")
        property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/jacoco/test/jacocoTestReport.xml")
    }
}

tasks.register("verify") {
    dependsOn(tasks["check"])
    dependsOn(tasks["assemble"])
}




