package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal;
import org.gradle.api.internal.artifacts.DefaultProjectDependencyFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.catalog.DelegatingProjectDependency;
import org.gradle.api.internal.catalog.TypeSafeProjectDependencyFactory;
import javax.inject.Inject;

@NonNullApi
public class Feature_HomeProjectDependency extends DelegatingProjectDependency {

    @Inject
    public Feature_HomeProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":feature:home:data"
     */
    public Feature_Home_DataProjectDependency getData() { return new Feature_Home_DataProjectDependency(getFactory(), create(":feature:home:data")); }

    /**
     * Creates a project dependency on the project at path ":feature:home:domain"
     */
    public Feature_Home_DomainProjectDependency getDomain() { return new Feature_Home_DomainProjectDependency(getFactory(), create(":feature:home:domain")); }

    /**
     * Creates a project dependency on the project at path ":feature:home:presentation"
     */
    public Feature_Home_PresentationProjectDependency getPresentation() { return new Feature_Home_PresentationProjectDependency(getFactory(), create(":feature:home:presentation")); }

}
