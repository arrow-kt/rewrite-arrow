package arrow;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Add an import for a DSL function of `EffectScope` and `EagerEffectScope` that is now an extension function on `Raise`.
 * Currently, this is only `ensure` so we have a single recipe for it.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class RaiseEnsureImport extends Recipe {
    @JsonCreator
    public RaiseEnsureImport() {
    }

    @Override
    public String getDisplayName() {
        return "Raise Rewrite";
    }

    @Override
    public String getDescription() {
        return "Rewrites all imports, and builders from arrow.core.computations.* and arrow.core.continuations.* to arrow.core.raise.*.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RaiseImportVisitor();
    }

    // TODO add this for all DSLs functions
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesType<>("arrow.core.raise.Raise", true),
                new UsesType<>("arrow.core.raise.Effect", true),
                new UsesType<>("arrow.core.raise.EagerEffect", true)
        );
    }

    private static class RaiseImportVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher ensureEffect = new MethodMatcher("arrow.core.continuations.EffectScope ensure(..)");
        private final MethodMatcher ensureEagerEffect = new MethodMatcher("arrow.core.continuations.EagerEffectScope ensure(..)");
        private final MethodMatcher ensureRaise = new MethodMatcher("arrow.core.raise.Raise ensure(..)");

        private final MethodMatcher ensureNotNullEffect = new MethodMatcher("arrow.core.continuations.EffectScopeKt ensureNotNull(..)");
        private final MethodMatcher ensureNotNullEagerEffect = new MethodMatcher("arrow.core.continuations.EagerEffectScopeKt ensureNotNull(..)");
        private final MethodMatcher ensureNotNullRaise = new MethodMatcher("arrow.core.raise.Raise ensureNotNull(..)");


        // We need to override visitLambda, so that visitMethodInvocation will also get called on the lambda's body.
        @Override
        public J visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
            return super.visitLambda(lambda, executionContext);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            // If we get called on a lambda's body, the method invocation will still be typed to EffectScope & EagerEffectScope.
            if (ensureRaise.matches(m) || ensureEffect.matches(m) || ensureEagerEffect.matches(m)) {
                maybeAddImport("arrow.core.raise.ensure", false);
            }
            if (ensureNotNullEffect.matches(m) || ensureNotNullEagerEffect.matches(m) || ensureNotNullRaise.matches(m)) {
                maybeRemoveImport("arrow.core.continuations.ensureNotNull");
                maybeAddImport("arrow.core.raise.ensureNotNull", false);
            }
            return m;
        }
    }
}
