-if class com.mohgwatch.core.api.dto.LoginResponse
-keepnames class com.mohgwatch.core.api.dto.LoginResponse
-if class com.mohgwatch.core.api.dto.LoginResponse
-keep class com.mohgwatch.core.api.dto.LoginResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
