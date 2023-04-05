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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinIsoVisitor;

/**
 * Rewrite object method invocations to top level functions.
 * <p>
 * So we need to rewrite:
 *  - `arrow.core.continuations.either eager(..)` to `arrow.core.raise either(..)`
 *  - `arrow.core.continuations.either invoke(..)` to `either(..)`
 *  - `arrow.core.continuations.either either(..)` to `either(..)`
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class RewriteEffectDSL extends Recipe {

    String oldPackage = "arrow.core.continuations.";
    String dslName = "either";
    String fullyQualifiedObject = oldPackage + dslName;
    String eagerPattern = fullyQualifiedObject + " eager(..)";
    String invokePattern = fullyQualifiedObject + " invoke(..)";
    String implicitInvokePattern = fullyQualifiedObject + " " + dslName + "(..)";

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

            // Note: I am not sure if there is an issue using `ChangeMethodName` here.
            // But it may be simpler to use the `ChangeMethodName` recipe.
            if (matches) {
                JavaType.Method type = m.getMethodType();
                if (type != null) {
                    type = type.withName(dslName);
                }

                /* Rename the method to the top-level object name.
                 * `eager`, `invoke` -> `either`
                 *
                 * If we encounter an implicit invoke call,
                 * the method name is assigned the name of the object, so we end up with: either.either {
                 * In that case we need to remove the receiver (select).
                 */
                m = m.withName(m.getName().withSimpleName(dslName)).withMethodType(type);

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
