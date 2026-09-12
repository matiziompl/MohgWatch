package com.mohgwatch.phone.di;

import android.content.Context;
import com.mohgwatch.phone.service.DataLayerSender;
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
public final class AppModule_ProvideDataLayerSenderFactory implements Factory<DataLayerSender> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideDataLayerSenderFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DataLayerSender get() {
    return provideDataLayerSender(contextProvider.get());
  }

  public static AppModule_ProvideDataLayerSenderFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideDataLayerSenderFactory(contextProvider);
  }

  public static DataLayerSender provideDataLayerSender(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDataLayerSender(context));
  }
}
