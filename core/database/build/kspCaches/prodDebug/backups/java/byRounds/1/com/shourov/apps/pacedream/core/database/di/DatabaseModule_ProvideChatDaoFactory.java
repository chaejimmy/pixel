package com.shourov.apps.pacedream.core.database.di;

import com.shourov.apps.pacedream.core.database.PaceDreamDatabase;
import com.shourov.apps.pacedream.core.database.dao.ChatDao;
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
public final class DatabaseModule_ProvideChatDaoFactory implements Factory<ChatDao> {
  private final Provider<PaceDreamDatabase> databaseProvider;

  public DatabaseModule_ProvideChatDaoFactory(Provider<PaceDreamDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatDao get() {
    return provideChatDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideChatDaoFactory create(
      Provider<PaceDreamDatabase> databaseProvider) {
    return new DatabaseModule_ProvideChatDaoFactory(databaseProvider);
  }

  public static ChatDao provideChatDao(PaceDreamDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideChatDao(database));
  }
}
