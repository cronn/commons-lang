buildscript {
    repositories {
        mavenCentral()
    }
    dependencyLocking {
        lockAllConfigurations()
    }
}

plugins {
    `java-library`
    jacoco
    `maven-publish`
    signing
    id("org.jreleaser") version "latest.release"
    id("com.diffplug.spotless") version "latest.release"
}

group = "de.cronn"
version = "1.6-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "256m"
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.assertj:assertj-core:latest.release")

    components.all {
        if (id.version.matches(Regex("(?i).+([-.])(CANDIDATE|RC|BETA|ALPHA|M\\d+).*"))) {
            status = "milestone"
        }
    }
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.wrapper {
    gradleVersion = "9.5.1"
    distributionType = Wrapper.DistributionType.ALL
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
    dependsOn(tasks.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = project.name
                description = "cronn Java common language extensions"
                url = "https://github.com/cronn/commons-lang"

                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }

                developers {
                    developer {
                        id = "benedikt.waldvogel"
                        name = "Benedikt Waldvogel"
                        email = "benedikt.waldvogel@cronn.de"
                    }
                }

                scm {
                    url = "https://github.com/cronn/commons-lang"
                }
            }

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

jreleaser {
    signing {
        active = org.jreleaser.model.Active.NEVER
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = org.jreleaser.model.Active.RELEASE
                    sign = false
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
