package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class EffectScopeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath()
              .build()
              .activateRecipes("arrow.RaiseRefactor")
          )
          .parser(
            KotlinParser.builder()
              .logCompilationWarningsAndErrors(true)
              .classpath("arrow-core-jvm")
          );
    }

    @Test
    void effectScopeParameter() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun test(scope: EffectScope<String>): Int {
                return scope.shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun test(scope: Raise<String>): Int {
                return scope.raise("failure")
              }
              """
          )
        );
    }

    @Test
    void effectScopeBody() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<String>.test(): Int {
                return shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun Raise<String>.test(): Int {
                return raise("failure")
              }
              """
          )
        );
    }

    @Test
    void effectScopeExpression() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<String>.test(): Int =
                shift("failure")
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              suspend fun Raise<String>.test(): Int =
                raise("failure")
              """
          )
        );
    }

    @Test
    void ensureOnExtension() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EffectScope

              suspend fun EffectScope<Int>.test(): Unit =
                ensure(false) { -1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise
              import arrow.core.raise.ensure

              suspend fun Raise<Int>.test(): Unit =
                ensure(false) { -1 }
              """
          )
        );
    }

    @Test
    void effectBuilder() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.Effect
              import arrow.core.continuations.effect

              val x: Effect<String, Int> = effect { 1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Effect
              import arrow.core.raise.effect

              val x: Effect<String, Int> = effect { 1 }
              """
          )
        );
    }

    @Test
    void ensureDSL() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.Effect
              import arrow.core.continuations.effect

              val x: Effect<String, Int> = effect {
                ensure(false) { "failure" }
                1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Effect
              import arrow.core.raise.effect
              import arrow.core.raise.ensure

              val x: Effect<String, Int> = effect {
                ensure(false) { "failure" }
                1
              }
              """
          )
        );
    }

    @Test
    void fold() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.effect
                            
              suspend fun example() {
                effect<String, Int> {
                  1
                }.fold(
                  { 0 },
                  { it }
                )
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.effect
              import arrow.core.raise.fold
                            
              suspend fun example() {
                effect<String, Int> {
                  1
                }.fold(
                  { 0 },
                  { it }
                )
              }
              """
          )
        );
    }

    @Test
    void foldThreeParams() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.effect
                            
              suspend fun example() {
                effect<String, Int> {
                  1
                }.fold(
                  { throw it },
                  { 0 },
                  { it }
                )
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.effect
              import arrow.core.raise.fold
                            
              suspend fun example() {
                effect<String, Int> {
                  1
                }.fold(
                  { throw it },
                  { 0 },
                  { it }
                )
              }
              """
          )
        );
    }

    @Test
    void toEither() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.effect
                            
              suspend fun example(): Either<String, Int> =
                effect<String, Int> {
                  1
                }.toEither()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.effect
              import arrow.core.raise.toEither
                            
              suspend fun example(): Either<String, Int> =
                effect<String, Int> {
                  1
                }.toEither()
              """
          )
        );
    }

    @Test
    void toIor() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Ior
              import arrow.core.continuations.effect
                            
              suspend fun example(): Ior<String, Int> =
                effect<String, Int> {
                  1
                }.toIor()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Ior
              import arrow.core.raise.effect
              import arrow.core.raise.toIor
                            
              suspend fun example(): Ior<String, Int> =
                effect<String, Int> {
                  1
                }.toIor()
              """
          )
        );
    }

    @Test
    void toValidated() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.continuations.effect
                            
              suspend fun example(): Validated<String, Int> =
                effect<String, Int> {
                  1
                }.toValidated()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.raise.effect
              import arrow.core.raise.toValidated
                            
              suspend fun example(): Validated<String, Int> =
                effect<String, Int> {
                  1
                }.toValidated()
              """
          )
        );
    }

    @Test
    void getOrNull() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.effect
                            
              suspend fun example(): Int? =
                effect<String, Int> {
                  1
                }.orNull()
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.effect
              import arrow.core.raise.getOrNull
                            
              suspend fun example(): Int? =
                effect<String, Int> {
                  1
                }.getOrNull()
              """
          )
        );
    }

    @Test
    void multiple() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.Effect
              import arrow.core.continuations.either
              import arrow.core.continuations.effect
              
              fun example2(): Either<String, Int> = either.eager {
                ensure(false) { "failure" }
                1
              }
              
              val x: Effect<String, Int> = effect {
                3
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.Effect
              import arrow.core.raise.effect
              import arrow.core.raise.either
              import arrow.core.raise.ensure
              
              fun example2(): Either<String, Int> = either {
                ensure(false) { "failure" }
                1
              }
              
              val x: Effect<String, Int> = effect {
                3
              }
              """
          )
        );
    }
}