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
              
              import arrow.core.Validated
              import arrow.core.mapOrAccumulate
              
              fun validate(int: Int): Validated<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).mapOrAccumulate { validate(it).bind() }
              }
              """
          )
        );
    }

    @Test
    void validatedMultilineLambda() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.traverse
              
              fun validate(int: Int): Validated<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).traverse {
                      println("Hello World!")
                      validate(it)
                  }
              }
              """,
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.mapOrAccumulate
              
              fun validate(int: Int): Validated<String, Int> = TODO()
              fun foo() {
                  listOf(1, 2, 3).mapOrAccumulate {
                      println("Hello World!")
                      validate(it).bind()
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled("FIXME: Not supported in ChangeValidatedLambda yet")
    void validatedInlineFunction() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.traverse
              
              fun foo() {
                  listOf(1, 2, 3).traverse {
                      val res: Validated<String, Int> = it.valid()
                      res
                  }
              }
              """,
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.mapOrAccumulate
              
              fun foo() {
                  listOf(1, 2, 3).mapOrAccumulate {
                      val res: Validated<String, Int> = it.valid()
                      res.bind()
                  }
              }
              """
          )
        );
    }

    @Test
    @Disabled("FIXME: Not supported in ChangeValidatedLambda yet")
    void validatedIfExpressionLambda() {
        rewriteRun(
          kotlin(
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.traverse
              
              fun foo() {
                  listOf(1, 2, 3).traverse {
                      if(it == 1) it.invalid()
                      else it.valid()
                  }
              }
              """,
            """
              package com.yourorg
              
              import arrow.core.Validated
              import arrow.core.mapOrAccumulate
              
              fun foo() {
                  listOf(1, 2, 3).mapOrAccumulate {
                      if(it % 2 == 0) it.invalid().bind()
                      else it.valid().bind()
                  }
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
