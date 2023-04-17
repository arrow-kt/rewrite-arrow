package arrow;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class ValidatedLambdaToEitherTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeValidatedLambda());
    }

    @Disabled("Added base for transformation. Unusure if Validated is transformed in a different visitor.")
    @Test
    void validatedLambda() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.traverse
              
              fun validate(int: Int): Validated<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).traverse { validate(it) }
              }
              """,
            """
              package com.yourorg
              
              import arrow.core.Either
              
              fun validate(int: Int): Either<String, Int> = TODO()
              fun foo() {
                 listOf(1, 2, 3).mapOrAccumulate { validate(it).bind() }
              }
              """
          )
        );
    }

    @Disabled("Requires changes from rewrite-kotlin")
    @Test
    void validatedPropertyInLambda() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.traverse
              
              fun validate(int: Int): Validated<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).traverse(::validate)
              }
              """,
            """
              package com.yourorg
              
              import arrow.core.Either
              
              fun validate(int: Int): Either<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).mapOrAccumulate {
                      validate(it).bind()
                  }
              }
              """
          )
        );
    }
}
