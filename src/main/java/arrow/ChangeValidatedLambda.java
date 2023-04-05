package arrow;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangeValidatedLambda extends Recipe {
    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getDescription() {
        return ".";
    }

    // Check UsesType.
//    @Override
//    public List<Recipe> getSingleSourceApplicableTests() {
//        return super.getSingleSourceApplicableTests();
//    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinIsoVisitor<ExecutionContext>() {
            private final MethodMatcher traverseMatcher = new MethodMatcher("arrow.core.IterableKt traverse(..)");
            private final String oldFqn = "arrow.core.Validated";
            private final String newFqn = "arrow.core.Either";
            // Fill in method type.
            private final JavaType.Method newMethodType = new JavaType.Method(
                    null,
                    1,
                    JavaType.ShallowClass.build("arrow.core.Either"),
                    "bind",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                JavaType.Method methodType = m.getMethodType();
                if (methodType != null && traverseMatcher.matches(methodType) && containsTargetArgument(m.getArguments())) {
                    m = m.withName(m.getName().withSimpleName("mapOrAccumulate"));
                    m = m.withMethodType(methodType.withName("mapOrAccumulate"));
                    m = (J.MethodInvocation) new AppendBindFunction().visitMethodInvocation(m, executionContext);
                }
                return m;
            }

            class AppendBindFunction extends KotlinIsoVisitor<ExecutionContext> {
                /*
                For Lambda blocks use:
                String uniqueName = VariableNameUtils.generateVariableName(newName, getCursor(), VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER);
                to generate a unique name in the scope of the cursor.
                 */

                @Override
                public J visitLambda(J.Lambda lambda, ExecutionContext executionContext) {
                    // Check for multiple statements in block.
                    // handle multiple statements.
                    // single return can be handled in visitMethodInvocation.
                    return super.visitLambda(lambda, executionContext);
                }

                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                    if (isTargetFqn(method.getMethodType())) {
                        return addBind(method);
                    }
                    return super.visitMethodInvocation(method, executionContext);
                }
            }

            private boolean containsTargetArgument(List<Expression> arguments) {
                for (Expression e : arguments) {
                    if (e instanceof J.Lambda) {
                        J.Lambda lambda = (J.Lambda) e;
                        if (containedInLambda(lambda.getBody())) {
                            return true;
                        }
                    } else if (e instanceof J.MemberReference) {
                        J.MemberReference memberRef = (J.MemberReference) e;
                        if (isTargetFqn(memberRef.getMethodType())) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean containedInLambda(J body) {
                AtomicBoolean found = new AtomicBoolean(false);
                KotlinIsoVisitor<AtomicBoolean> visitor = new KotlinIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                        if (isTargetFqn(method.getMethodType())) {
                            atomicBoolean.set(true);
                        }
                        return atomicBoolean.get() ? method : super.visitMethodInvocation(method, atomicBoolean);
                    }
                };
                visitor.visit(body, found);
                return found.get();
            }

            private boolean isTargetFqn(@Nullable JavaType.Method methodType) {
                return methodType != null && TypeUtils.isOfClassType(methodType.getReturnType(), oldFqn);
            }

            private J.MethodInvocation addBind(J.MethodInvocation method) {
                return new J.MethodInvocation(Tree.randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        JRightPadded.build(method),
                        null,
                        new J.Identifier(Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                "bind",
                                null,
                                null
                        ),
                        JContainer.build(Collections.singletonList(JRightPadded.build(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY)))),
                        newMethodType
                );
            }
        };
    }
}
