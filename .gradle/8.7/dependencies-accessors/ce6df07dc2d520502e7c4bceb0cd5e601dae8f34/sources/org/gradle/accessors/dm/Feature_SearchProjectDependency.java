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
public class Feature_SearchProjectDependency extends DelegatingProjectDependency {

    @Inject
    public Feature_SearchProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":feature:search:data"
     */
    public Feature_Search_DataProjectDependency getData() { return new Feature_Search_DataProjectDependency(getFactory(), create(":feature:search:data")); }

    /**
     * Creates a project dependency on the project at path ":feature:search:domain"
     */
    public Feature_Search_DomainProjectDependency getDomain() { return new Feature_Search_DomainProjectDependency(getFactory(), create(":feature:search:domain")); }

    /**
     * Creates a project dependency on the project at path ":feature:search:presentation"
     */
    public Feature_Search_PresentationProjectDependency getPresentation() { return new Feature_Search_PresentationProjectDependency(getFactory(), create(":feature:search:presentation")); }

}
