import io.github.gradlenexus.publishplugin.NexusPublishExtension
import java.lang.System.getenv

plugins {
  java
  `maven-publish`
  signing
  id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "io.arrow-kt"
version = "1.0-SNAPSHOT"

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

  implementation("com.puppycrawl.tools:checkstyle:10.9.3")
  implementation(platform(libs.rewrite.recipe.bom))
  compileOnly(libs.rewrite.core)
  implementation(libs.bundles.rewrite)

  testImplementation(libs.bundles.rewrite.test)
  testRuntimeOnly(libs.arrow.core)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.lombok)
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

configure<NexusPublishExtension> {
  repositories {
    sonatype {
      username.set(getenv("OSS_USER"))
      password.set(getenv("OSS_TOKEN"))
      stagingProfileId.set(getenv("OSS_STAGING_PROFILE_ID"))
    }
  }
}

afterEvaluate {
  signing {
    if (shouldSign) {
      try {
        useInMemoryPgpKeys(getenv("SIGNING_KEY_ID"), getenv("SIGNING_KEY"), getenv("SIGNING_KEY_PASSPHRASE"))
      } catch (_: Throwable) {
        useGpgCmd()
      }
      sign(project.extensions.getByName<PublishingExtension>("publishing").publications)
    }
  }
}

private val SigningExtension.shouldSign: Boolean
  get() = !isSnapshot && !isLocal

val SigningExtension.isSnapshot: Boolean
  get() = project.version.toString().endsWith("-SNAPSHOT", ignoreCase = true)

val SigningExtension.isLocal: Boolean
  get() = project.gradle.startParameter.taskNames.any {
    it.contains("publishToMavenLocal", ignoreCase = true)
  }
