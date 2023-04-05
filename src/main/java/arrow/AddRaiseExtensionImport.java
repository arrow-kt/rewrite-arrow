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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.lang.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Previously `Effect` and `EagerEffect` were interfaces, and now they are type aliases for `Raise` based lambdas.
 * As a result of this change, the all methods became extension functions on `Raise` and thus require an import.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class AddRaiseExtensionImport extends Recipe {

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

    // Minor change. Generally, we consider it best practice to add the recipe `Options` first to communicate relevant APIs to the user.
    // Removed private final since it's added by `@Value`
    String foldPattern;
    String eagerFoldPattern;

    public AddRaiseExtensionImport(
            @JsonProperty("methodPattern") String methodPattern,
            @JsonProperty("newMethodName") @Nullable String newMethodName, // Added @Nullable annoation to match nullability of the option.
            @JsonProperty("methodImport") String methodImport) {
        this.methodPattern = methodPattern;
        this.newMethodName = newMethodName;
        this.methodImport = methodImport;
        this.foldPattern = "arrow.core.raise.Effect " + methodPattern;
        this.eagerFoldPattern = "arrow.core.raise.EagerEffect " + methodPattern;
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
                    // Update the type along with the name. Note: if possible ChangeMethodName is a better choice.
                    JavaType.Method type = m.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    m = m.withName(m.getName().withSimpleName(newMethodName)).withMethodType(type);
                }

                // Add import for raise that previously was a method on Effect/EagerEffect
                maybeAddImport(methodImport, false);
            }
            return m;
        }
    }
}
