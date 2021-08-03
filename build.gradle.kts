import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.com.google.common.collect.Lists
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
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
    id("org.openapi.generator") version "5.1.1"
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
        url = uri("http://nexusint1.id.dvag.com:8081/nexus/content/groups/public/")
    }
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
    maven {
        url = uri("https://maven.pkg.github.com/dvag/rest-security-utils")
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
    implementation("com.microsoft.azure:applicationinsights-spring-boot-starter:2.6.3")
    implementation("org.springframework.boot:spring-boot-starter:2.5.2")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.5.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.20")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.20")
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("io.springfox:springfox-swagger2:3.0.0")
    implementation("io.springfox:springfox-swagger-ui:3.0.0")
    implementation("io.springfox:springfox-spring-web:3.0.0")
    implementation("io.springfox:springfox-bean-validators:3.0.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.1.10")
    implementation("org.openapitools:jackson-databind-nullable:0.2.1")
    implementation("org.zalando:problem-spring-web:0.26.2")
    implementation("org.springframework.boot:spring-boot-starter-web:2.5.2")
    implementation("org.springframework.boot:spring-boot-starter-aop:2.5.2")
    implementation("org.springframework.boot:spring-boot-starter-validation:2.5.2")
    implementation("ch.sbb:springboot-graceful-shutdown:2.0.1")
    implementation("org.springframework.boot:spring-boot-starter-security:2.5.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.7.1")
    implementation("org.slf4j:log4j-over-slf4j:1.7.31")
    implementation("ch.qos.logback:logback-access:1.2.3")
    implementation("net.logstash.logback:logstash-logback-encoder:6.6")
    implementation("net.rakugakibox.spring.boot:logback-access-spring-boot-starter:2.7.1")
    implementation("io.swagger:swagger-annotations:1.6.2")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.5.2")
    testImplementation("org.hamcrest:hamcrest-core:2.2")
    testImplementation("org.hamcrest:hamcrest-library:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("com.intuit.karate:karate-apache:0.9.6")
    testImplementation("com.intuit.karate:karate-junit5:0.9.6")
    testImplementation("net.masterthought:cucumber-reporting:5.5.4")
    testImplementation("commons-io:commons-io:2.11.0")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:2.5.2")
}

tasks.jar {
    destinationDirectory.set(buildDir)
}

tasks.bootJar {
    destinationDirectory.set(buildDir)
}

tasks.register<GenerateTask>("generateRestApiClient") {
    generatorName.set("spring")
    modelNameSuffix.set("Dto")
    inputSpec.set("${projectDir}/spec/service.yml")
    outputDir.set("${buildDir}/generated-sources/${project.name}-server-v1")
    version.set("0.0.1-SNAPSHOT")
    id.set("${project.name}-server-v1")
    supportingFilesConstrainedTo.set(Lists.newArrayList())
    apiPackage.set("com.dvag.${project.name}.build.server.v1.api")
    invokerPackage.set("com.dvag.${project.name}.build.server.v1")
    modelPackage.set("com.dvag.${project.name}.build.server.v1.model")
    generateApiTests.set(false)
    typeMappings.put("OffsetDateTime","LocalDateTime")
    importMappings.put("java.time.OffsetDateTime","java.time.LocalDateTime")
    configOptions.put("dateLibrary", "java8")
    configOptions.put("enumPropertyNaming", "UPPERCASE")
    configOptions.put("lombok", "false")
    configOptions.put("useTags", "true")
    configOptions.put("validateSpec", "true")
    configOptions.put("interfaceOnly", "true")
}

tasks.withType<ProcessResources> {
//    dependsOn(tasks.findByName("generateGitProperties"))
//    mustRunAfter(tasks["generateGitProperties"])
    duplicatesStrategy=DuplicatesStrategy.INCLUDE
    filesMatching("application.yml") {
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

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("$buildDir/generated-sources/${project.name}-server-v1/src/main/java")
        resources.srcDir("$projectDir/src/main/resources")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(
        tasks.findByName("generateRestApiClient"),
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




