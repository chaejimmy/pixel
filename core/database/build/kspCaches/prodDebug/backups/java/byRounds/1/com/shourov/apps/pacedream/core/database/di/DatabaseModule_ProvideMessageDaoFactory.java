package com.shourov.apps.pacedream.core.database.di;

import com.shourov.apps.pacedream.core.database.PaceDreamDatabase;
import com.shourov.apps.pacedream.core.database.dao.MessageDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class DatabaseModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<PaceDreamDatabase> databaseProvider;

  public DatabaseModule_ProvideMessageDaoFactory(Provider<PaceDreamDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideMessageDaoFactory create(
      Provider<PaceDreamDatabase> databaseProvider) {
    return new DatabaseModule_ProvideMessageDaoFactory(databaseProvider);
  }

  public static MessageDao provideMessageDao(PaceDreamDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideMessageDao(database));
  }
}
