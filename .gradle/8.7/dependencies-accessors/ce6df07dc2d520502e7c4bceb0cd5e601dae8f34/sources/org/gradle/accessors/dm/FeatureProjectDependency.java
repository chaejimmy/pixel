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
public class FeatureProjectDependency extends DelegatingProjectDependency {

    @Inject
    public FeatureProjectDependency(TypeSafeProjectDependencyFactory factory, ProjectDependencyInternal delegate) {
        super(factory, delegate);
    }

    /**
     * Creates a project dependency on the project at path ":feature:auth"
     */
    public Feature_AuthProjectDependency getAuth() { return new Feature_AuthProjectDependency(getFactory(), create(":feature:auth")); }

    /**
     * Creates a project dependency on the project at path ":feature:booking"
     */
    public Feature_BookingProjectDependency getBooking() { return new Feature_BookingProjectDependency(getFactory(), create(":feature:booking")); }

    /**
     * Creates a project dependency on the project at path ":feature:chat"
     */
    public Feature_ChatProjectDependency getChat() { return new Feature_ChatProjectDependency(getFactory(), create(":feature:chat")); }

    /**
     * Creates a project dependency on the project at path ":feature:create-account"
     */
    public Feature_CreateAccountProjectDependency getCreateAccount() { return new Feature_CreateAccountProjectDependency(getFactory(), create(":feature:create-account")); }

    /**
     * Creates a project dependency on the project at path ":feature:guest"
     */
    public Feature_GuestProjectDependency getGuest() { return new Feature_GuestProjectDependency(getFactory(), create(":feature:guest")); }

    /**
     * Creates a project dependency on the project at path ":feature:home"
     */
    public Feature_HomeProjectDependency getHome() { return new Feature_HomeProjectDependency(getFactory(), create(":feature:home")); }

    /**
     * Creates a project dependency on the project at path ":feature:host"
     */
    public Feature_HostProjectDependency getHost() { return new Feature_HostProjectDependency(getFactory(), create(":feature:host")); }

    /**
     * Creates a project dependency on the project at path ":feature:inbox"
     */
    public Feature_InboxProjectDependency getInbox() { return new Feature_InboxProjectDependency(getFactory(), create(":feature:inbox")); }

    /**
     * Creates a project dependency on the project at path ":feature:notification"
     */
    public Feature_NotificationProjectDependency getNotification() { return new Feature_NotificationProjectDependency(getFactory(), create(":feature:notification")); }

    /**
     * Creates a project dependency on the project at path ":feature:payment"
     */
    public Feature_PaymentProjectDependency getPayment() { return new Feature_PaymentProjectDependency(getFactory(), create(":feature:payment")); }

    /**
     * Creates a project dependency on the project at path ":feature:search"
     */
    public Feature_SearchProjectDependency getSearch() { return new Feature_SearchProjectDependency(getFactory(), create(":feature:search")); }

    /**
     * Creates a project dependency on the project at path ":feature:signin"
     */
    public Feature_SigninProjectDependency getSignin() { return new Feature_SigninProjectDependency(getFactory(), create(":feature:signin")); }

    /**
     * Creates a project dependency on the project at path ":feature:webflow"
     */
    public Feature_WebflowProjectDependency getWebflow() { return new Feature_WebflowProjectDependency(getFactory(), create(":feature:webflow")); }

    /**
     * Creates a project dependency on the project at path ":feature:wishlist"
     */
    public Feature_WishlistProjectDependency getWishlist() { return new Feature_WishlistProjectDependency(getFactory(), create(":feature:wishlist")); }

}
