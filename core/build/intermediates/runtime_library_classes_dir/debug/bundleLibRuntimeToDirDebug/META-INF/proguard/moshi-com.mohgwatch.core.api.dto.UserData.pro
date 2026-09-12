-if class com.mohgwatch.core.api.dto.UserData
-keepnames class com.mohgwatch.core.api.dto.UserData
-if class com.mohgwatch.core.api.dto.UserData
-keep class com.mohgwatch.core.api.dto.UserDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
