/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.openrewrite.Formatting.EMPTY;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

public class ChangeMethodTargetToStatic extends JavaIsoRefactorVisitor {
    private MethodMatcher methodMatcher;
    private String targetType;

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher)
                .and(required("target.type", targetType));
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
        if(methodMatcher.matches(method)) {
            andThen(new Scoped(method, targetType));
        }
        return super.visitMethodInvocation(method);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final J.MethodInvocation scope;
        private final String targetType;

        public Scoped(J.MethodInvocation scope, String clazz) {
            this.scope = scope;
            this.targetType = clazz;
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("to", targetType);
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method) {
            if (scope.isScope(method)) {
                JavaType.FullyQualified classType = JavaType.Class.build(targetType);
                J.MethodInvocation m = method.withSelect(
                        J.Ident.build(randomId(), classType.getClassName(), classType,
                                method.getSelect() == null ? EMPTY : method.getSelect().getFormatting()));

                maybeAddImport(targetType);

                JavaType.Method transformedType = null;
                if (method.getType() != null) {
                    maybeRemoveImport(method.getType().getDeclaringType());
                    transformedType = method.getType().withDeclaringType(classType);
                    if (!method.getType().hasFlags(Flag.Static)) {
                        Set<Flag> flags = new LinkedHashSet<>(method.getType().getFlags());
                        flags.add(Flag.Static);
                        transformedType = transformedType.withFlags(flags);
                    }
                }

                return m.withType(transformedType);
            }

            return super.visitMethodInvocation(method);
        }
    }
}
