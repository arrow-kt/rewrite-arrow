/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package arrow;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class EitherDSLTest implements RewriteTest {
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
    void eitherEagerToEitherDSL() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.either
              import arrow.core.continuations.eager

              fun test(): Either<String, Int> = either.eager {
                1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.either

              fun test(): Either<String, Int> = either {
                1
              }
              """
          )
        );
    }

    @Test
    void eitherImplicitInvokeToEitherDSL() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.either

              fun test(): Either<String, Int> = either {
                1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.either

              fun test(): Either<String, Int> = either {
                1
              }
              """
          )
        );
    }

    @Test
    void eitherOperatorInvokeToEitherDSL() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.continuations.either

              fun test(): Either<String, Int> = either.invoke {
                1
              }
              """,
            """
              package com.yourorg
                            
              import arrow.core.Either
              import arrow.core.raise.either

              fun test(): Either<String, Int> = either {
                1
              }
              """
          )
        );
    }
}