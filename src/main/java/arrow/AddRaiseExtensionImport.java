package arrow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.lang.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Previously `Effect` and `EagerEffect` were interfaces, and now they are type aliases for `Raise` based lambdas.
 * As a result of this change, the all methods became extension functions on `Raise` and thus require an import.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class AddRaiseExtensionImport extends Recipe {

    private final String foldPattern;
    private final String eagerFoldPattern;

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "methodName(..)")
    String methodPattern;

    @Option(displayName = "New Method Name",
            description = "The name of the method that will replace the existing name.",
            example = "methodName")
    @Nullable
    String newMethodName;

    @Option(displayName = "New Method Import",
            description = "The import for the new method name that will replace the existing name.",
            example = "my.package.methodName")
    String methodImport;

    public AddRaiseExtensionImport(
            @JsonProperty("methodPattern") String methodPattern,
            @JsonProperty("methodName") String newMethodName,
            @JsonProperty("methodImport") String methodImport) {
        this.methodPattern = methodPattern;
        this.newMethodName = newMethodName;
        this.methodImport = methodImport;
        this.foldPattern = "arrow.core.continuations.Effect " + methodPattern;
        this.eagerFoldPattern = "arrow.core.continuations.EagerEffect " + methodPattern;
    }

    @Override
    public String getDisplayName() {
        return "Add Raise Extension Import";
    }

    @Override
    public String getDescription() {
        return "Adds an import for a method of `Effect` and `EffectScope` that is now an extension function on `Raise`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RaiseFoldImportVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(new UsesMethod<>(foldPattern), new UsesMethod<>(eagerFoldPattern));
    }

    private class RaiseFoldImportVisitor extends KotlinIsoVisitor<ExecutionContext> {
        private final MethodMatcher foldEffectMatcher = new MethodMatcher(foldPattern);
        private final MethodMatcher foldEagerEffectMatcher = new MethodMatcher(eagerFoldPattern);

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
            if (foldEffectMatcher.matches(m) || foldEagerEffectMatcher.matches(m)) {
                // Rename method if name changed (orNull -> getOrNull)
                if (newMethodName != null) {
                    m = m.withName(m.getName().withSimpleName(newMethodName));
                }

                // Add import for raise that previously was a method on Effect/EagerEffect
                maybeAddImport(methodImport, false);
            }
            return m;
        }
    }
}
