package arrow;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ValidatedToEitherTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath()
              .build()
              .activateRecipes("arrow.ValidatedToEitherRecipe")
          )
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void rewriteValidToRight() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.valid

              val x: Validated<String, Int> = 1.valid()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.right

              val x: Either<String, Int> = 1.right()
              """
          )
        );
    }

    @Test
    void rewriteInvalidToLeft() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.invalid

              val x: Validated<String, Int> = "failure".invalid()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.left

              val x: Either<String, Int> = "failure".left()
              """
          )
        );
    }

    @Test
    void rewriteInvalidNelToLeftNel() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg

              import arrow.core.NonEmptyList
              import arrow.core.Validated
              import arrow.core.invalidNel

              val x: Validated<NonEmptyList<String>, Int> = "failure".invalidNel()
              """,
            """
              package com.yourorg

              import arrow.core.Either
              import arrow.core.NonEmptyList
              import arrow.core.leftNel

              val x: Either<NonEmptyList<String>, Int> = "failure".leftNel()
              """
          )
        );
    }

    @Test
    @Disabled("TypeAlias refactor not working")
    void rewriteAlias() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg

              import arrow.core.ValidatedNel
              import arrow.core.invalidNel

              val x: ValidatedNel<String>, Int> = "failure".invalidNel()
              """,
            """
              package com.yourorg

              import arrow.core.EitherNel
              import arrow.core.leftNel

              val x: EitherNel<String>, Int> = "failure".leftNel()
              """
          )
        );
    }

    @Test
    @Disabled("Not yet implemented")
    void rewriteZipToZipOrAccumulate() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg

              import arrow.core.NonEmptyList
              import arrow.core.Validated
              import arrow.core.invalidNel
              import arrow.core.zip

              val x: Validated<NonEmptyList<String>, Int> = "failure".invalidNel()
              val y: Validated<NonEmptyList<String>, Int> = "failure".invalidNel()
              val z: Validated<NonEmptyList<String>, Int> = x.zip(y) { a, b -> a + b }
              """,
            """
              package com.yourorg

              import arrow.core.Either
              import arrow.core.NonEmptyList
              import arrow.core.leftNel

              val x: Either<NonEmptyList<String>, Int> = "failure".leftNel()
              val y: Either<NonEmptyList<String>, Int> = "failure".leftNel()
              val z: Either<NonEmptyList<String>, Int> = Either.zipOrAccumulate(x, y) { a, b -> a + b }
              """
          )
        );
    }
}