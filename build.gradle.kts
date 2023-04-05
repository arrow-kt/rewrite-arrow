@Suppress("DSL_SCOPE_VIOLATION") plugins {
  id("org.openrewrite.build.recipe-library") version "1.9.4"
}

group = "io.arrow-kt"
description = "A rewrite module automating Arrow refactorings from 1.x.x to 1.2.x/2.x.x."

// Doesn't affect version being inject by plugin
//rewriteRecipe {
//  version = requireNotNull(libs.rewrite.core.get().version)
//}

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
