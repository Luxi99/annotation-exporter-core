plugins {
    `java-library`
    `maven-publish`
}

group = "io.github.Luxi99"
version = "0.1.1"

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.scijava.org/content/repositories/releases")
    }
}

dependencies {
    api("io.github.qupath:qupath-core:0.7.0")

    implementation("org.jetbrains:annotations:26.0.2")
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        encoding = "UTF-8"
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("Annotation Exporter Core")
                description.set("Logica di estrazione di label mask e tabelle da annotazioni manuali QuPath")
                url.set("https://github.com/Luxi99/annotation-exporter-core")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Luxi99/annotation-exporter-core")
            credentials {
                username = (project.findProperty("gpr.user") ?: System.getenv("GITHUB_ACTOR"))?.toString()
                password = (project.findProperty("gpr.token") ?: System.getenv("GITHUB_TOKEN"))?.toString()
            }
        }
    }
}