package com.mohgwatch.phone.di;

import com.mohgwatch.core.api.LibreLinkUpClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideLibreLinkUpClientFactory implements Factory<LibreLinkUpClient> {
  @Override
  public LibreLinkUpClient get() {
    return provideLibreLinkUpClient();
  }

  public static AppModule_ProvideLibreLinkUpClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static LibreLinkUpClient provideLibreLinkUpClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideLibreLinkUpClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideLibreLinkUpClientFactory INSTANCE = new AppModule_ProvideLibreLinkUpClientFactory();
  }
}
