package com.shourov.apps.pacedream.core.database.di;

import com.shourov.apps.pacedream.core.database.PaceDreamDatabase;
import com.shourov.apps.pacedream.core.database.dao.BookingDao;
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
public final class DatabaseModule_ProvideBookingDaoFactory implements Factory<BookingDao> {
  private final Provider<PaceDreamDatabase> databaseProvider;

  public DatabaseModule_ProvideBookingDaoFactory(Provider<PaceDreamDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BookingDao get() {
    return provideBookingDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideBookingDaoFactory create(
      Provider<PaceDreamDatabase> databaseProvider) {
    return new DatabaseModule_ProvideBookingDaoFactory(databaseProvider);
  }

  public static BookingDao provideBookingDao(PaceDreamDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBookingDao(database));
  }
}
