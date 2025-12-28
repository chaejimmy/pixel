package com.shourov.apps.pacedream.core.database.di;

import android.content.Context;
import com.shourov.apps.pacedream.core.database.PaceDreamDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DatabaseModule_ProvidePaceDreamDatabaseFactory implements Factory<PaceDreamDatabase> {
  private final Provider<Context> contextProvider;

  public DatabaseModule_ProvidePaceDreamDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PaceDreamDatabase get() {
    return providePaceDreamDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvidePaceDreamDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvidePaceDreamDatabaseFactory(contextProvider);
  }

  public static PaceDreamDatabase providePaceDreamDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePaceDreamDatabase(context));
  }
}
