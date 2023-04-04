import java.lang.System.getenv

@Suppress("DSL_SCOPE_VIOLATION") plugins {
  java
  `maven-publish`
  signing
  alias(libs.plugins.nexus)
}

group = "io.arrow-kt"

repositories {
  mavenCentral()
}

tasks.named<JavaCompile>("compileJava") {
  options.release.set(8)
}

tasks.test {
  useJUnitPlatform()
}

dependencies {
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)

  implementation(libs.checkstyle)
  implementation(libs.rewrite.core)
  implementation(libs.bundles.rewrite)

  testImplementation(libs.bundles.rewrite.test)
  testRuntimeOnly(libs.arrow.core)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.lombok)
}

java {
  withJavadocJar()
  withSourcesJar()
}

nexusPublishing {
  repositories {
    sonatype {
      username.set(getenv("OSS_USER"))
      password.set(getenv("OSS_TOKEN"))
      stagingProfileId.set(getenv("OSS_STAGING_PROFILE_ID"))
    }
  }
}

signing {
  useInMemoryPgpKeys(
    getenv("SIGNING_KEY_ID"),
    getenv("SIGNING_KEY"),
    getenv("SIGNING_KEY_PASSPHRASE")
  )
  sign(project.extensions.getByName<PublishingExtension>("publishing").publications)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifactId = "rewrite-arrow"
      from(components["java"])
      pom {
        name.set("Rewrite Arrow")
        description.set("OpenRewrite recipes for Arrow 1.2.0-RC / 2.0.0")
        url.set("https://github.com/arrow-kt/rewrite-arrow")
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            id.set("nomisRev")
            name.set("Simon Vergauwen")
            email.set("vergauwen.simon@gmail.com")
          }
        }
        scm {
          connection.set("scm:git:git://github.com/arrow-kt/rewrite-arrow.git")
          developerConnection.set("scm:git:ssh://github.com/arrow-kt/rewrite-arrow.git")
          url.set("https://github.com/arrow-kt/rewrite-arrow")
        }
      }
    }
  }
}
