-if class com.mohgwatch.core.api.dto.GraphResponse
-keepnames class com.mohgwatch.core.api.dto.GraphResponse
-if class com.mohgwatch.core.api.dto.GraphResponse
-keep class com.mohgwatch.core.api.dto.GraphResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
