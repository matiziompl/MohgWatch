package com.mohgwatch.phone.di;

import android.content.Context;
import com.mohgwatch.phone.data.CredentialStore;
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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideCredentialStoreFactory implements Factory<CredentialStore> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideCredentialStoreFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CredentialStore get() {
    return provideCredentialStore(contextProvider.get());
  }

  public static AppModule_ProvideCredentialStoreFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideCredentialStoreFactory(contextProvider);
  }

  public static CredentialStore provideCredentialStore(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideCredentialStore(context));
  }
}
