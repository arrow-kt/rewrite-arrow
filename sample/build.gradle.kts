@Suppress("DSL_SCOPE_VIOLATION") plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.openrewrite)
}

repositories {
  mavenCentral()
}

kotlin {
  jvm()
  macosArm64()

  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.arrow.core)
      }
    }
  }
}

dependencies {
  rewrite(rootProject)
}

tasks.findByName("rewriteDryRun")!!.dependsOn(rootProject.tasks.findByName("jar"))
tasks.findByName("rewriteRun")!!.dependsOn(rootProject.tasks.findByName("jar"))
tasks.findByName("rewriteDiscover")!!.dependsOn(rootProject.tasks.findByName("jar"))

rewrite {
  activeRecipe("arrow.RaiseRefactor")
}
