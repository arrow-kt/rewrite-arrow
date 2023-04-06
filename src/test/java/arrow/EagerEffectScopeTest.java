package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class EagerEffectScopeTest implements RewriteTest {
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
    void eagerEffectScopeParameter() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun test(scope: EagerEffectScope<String>): Int {
                return scope.shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun test(scope: Raise<String>): Int {
                return scope.raise("failure")
              }
              """
          )
        );
    }

    @Test
    void eagerEffectScopeReceiver() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun EagerEffectScope<String>.test(): Int {
                return shift("failure")
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun Raise<String>.test(): Int {
                return raise("failure")
              }
              """
          )
        );
    }

    @Test
    void eagerEffectScopeReceiverExpression() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              fun EagerEffectScope<String>.test(): Int =
                shift("failure")
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.Raise

              fun Raise<String>.test(): Int =
                raise("failure")
              """
          )
        );
    }

    @Test
    void eagerEffectScopeEnsure() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffectScope

              suspend fun EagerEffectScope<Int>.test(): Unit =
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
    void eagerEffectDSLRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffect
              import arrow.core.continuations.eagerEffect

              val x: EagerEffect<String, Int> = eagerEffect { 1 }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.EagerEffect
              import arrow.core.raise.eagerEffect

              val x: EagerEffect<String, Int> = eagerEffect { 1 }
              """
          )
        );
    }

    @Test
    void eagerEffectAndEnsureDSLRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.EagerEffect
              import arrow.core.continuations.eagerEffect

              val x: EagerEffect<String, Int> = eagerEffect {
                ensure(false) { "failure" }
                1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.EagerEffect
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.ensure

              val x: EagerEffect<String, Int> = eagerEffect {
                ensure(false) { "failure" }
                1
              }
              """
          )
        );
    }

    @Test
    void eagerEffectFoldRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.eagerEffect
                            
              suspend fun example() {
                eagerEffect<String, Int> {
                  1
                }.fold(
                  { 0 },
                  { it }
                )
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.fold
                            
              suspend fun example() {
                eagerEffect<String, Int> {
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
    void eagerEffectThreeParamsFoldRewrite() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.continuations.eagerEffect
                            
              suspend fun example() {
                eagerEffect<String, Int> {
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
                            
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.fold
                            
              suspend fun example() {
                eagerEffect<String, Int> {
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
              import arrow.core.continuations.eagerEffect
                            
              fun example(): Either<String, Int> =
                eagerEffect<String, Int> {
                  1
                }.toEither()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.toEither
                            
              fun example(): Either<String, Int> =
                eagerEffect<String, Int> {
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
              import arrow.core.continuations.eagerEffect
                            
              fun example(): Ior<String, Int> =
                eagerEffect<String, Int> {
                  1
                }.toIor()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Ior
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.toIor
                            
              fun example(): Ior<String, Int> =
                eagerEffect<String, Int> {
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
              import arrow.core.continuations.eagerEffect
                            
              fun example(): Validated<String, Int> =
                eagerEffect<String, Int> {
                  1
                }.toValidated()
              """,
            """
              package com.yourorg
                            
              import arrow.core.Validated
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.toValidated
                            
              fun example(): Validated<String, Int> =
                eagerEffect<String, Int> {
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
                            
              import arrow.core.continuations.eagerEffect
                            
              fun example(): Int? =
                eagerEffect<String, Int> {
                  1
                }.orNull()
              """,
            """
              package com.yourorg
                            
              import arrow.core.raise.eagerEffect
              import arrow.core.raise.getOrNull
                            
              fun example(): Int? =
                eagerEffect<String, Int> {
                  1
                }.getOrNull()
              """
          )
        );
    }
}
