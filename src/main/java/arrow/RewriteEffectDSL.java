package arrow;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Rewrite object method invocations to top level functions.
 * <p>
 * Library code:
 * ```kotlin
 * package arrow.core.continuations
 * <p>
 * public object either {
 *   public inline fun <E, A> eager(noinline f: suspend EagerEffectScope<E>.() -> A): Either<E, A> =
 *     eagerEffect(f).toEither()
 * <p>
 *   public suspend operator fun <E, A> invoke(f: suspend EffectScope<E>.() -> A): Either<E, A> =
 *     effect(f).toEither()
 * }
 * ```
 * <p>
 * needs to be rewritten to:
 * <p>
 * ```kotlin
 * package arrow.core.raise
 * <p>
 * public inline fun <E, A> either(@BuilderInference block: Raise<E>.() -> A): Either<E, A> =
 *   fold({ block.invoke(this) }, { Either.Left(it) }, { Either.Right(it) })
 * ```
 * <p>
 * So we need to rewrite:
 *  - `arrow.core.continuations.either eager(..)` to `arrow.core.raise either(..)`
 *  - `arrow.core.continuations.either invoke(..)` to `either(..)`
 *  - `arrow.core.continuations.either either(..)` to `either(..)`
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class RewriteEffectDSL extends Recipe {

    private final String oldPackage = "arrow.core.continuations.";
    private final String dslName = "either";
    private final String fullyQualifiedObject = oldPackage + dslName;
    private final String eagerPattern = fullyQualifiedObject + " eager(..)";
    private final String invokePattern = fullyQualifiedObject + " invoke(..)";
    private final String implicitInvokePattern = fullyQualifiedObject + " " + dslName + "(..)";

    @Override
    public String getDisplayName() {
        return "Raise Rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites Kotlin's object method invocations to top level functions.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesMethod<>(eagerPattern),
                new UsesMethod<>(invokePattern),
                new UsesMethod<>(implicitInvokePattern)
        );
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangeObjectMethodToTopLevelFunctionVisitor();
    }

    public class ChangeObjectMethodToTopLevelFunctionVisitor extends KotlinIsoVisitor<ExecutionContext> {
        MethodMatcher eager = new MethodMatcher(eagerPattern);
        MethodMatcher implicitInvoke = new MethodMatcher(implicitInvokePattern);
        MethodMatcher invoke = new MethodMatcher(invokePattern);

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            boolean implicit = implicitInvoke.matches(m);
            boolean matches = eager.matches(m) || implicit || invoke.matches(m);

            if (matches) {
                /* Rename the method to the top-level object name.
                 * `eager`, `invoke` -> `either`
                 *
                 * If we encounter an implicit invoke call,
                 * the method name is assigned the name of the object, so we end up with: either.either {
                 * In that case we need to remove the receiver (select).
                 */
                m = m.withName(m.getName().withSimpleName(dslName));

                if (!implicit) {
                    m = m.withSelect(null);
                }

                // Add the import to the top-level DSL function.
                maybeAddImport("arrow.core.raise." + dslName, null, false);

                // Remove the import to the object & the eager method.
                maybeRemoveImport(fullyQualifiedObject);
                maybeRemoveImport(oldPackage + "eager");
            }
            return m;
        }
    }
}
