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
package org.openrewrite.maven;

import org.openrewrite.AbstractSourceVisitor;
import org.openrewrite.maven.tree.Maven;

public abstract class AbstractMavenSourceVisitor<R> extends AbstractSourceVisitor<R>
        implements MavenSourceVisitor<R> {

    public R visitPom(Maven.Pom pom) {
        return reduce(
                defaultTo(pom),
                reduce(
                        reduce(
                                visit(pom.getDependencyManagement()),
                                visit(pom.getDependencies())
                        ),
                        visit(pom.getProperties())
                )
        );
    }

    public R visitParent(Maven.Parent parent) {
        return defaultTo(parent);
    }

    public R visitDependency(Maven.Dependency dependency) {
        return defaultTo(dependency);
    }

    public R visitDependencyManagement(Maven.DependencyManagement dependencyManagement) {
        return reduce(
                defaultTo(dependencyManagement),
                visit(dependencyManagement.getDependencies())
        );
    }

    public R visitProperty(Maven.Property property) {
        return defaultTo(property);
    }
}
