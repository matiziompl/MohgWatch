-if class com.mohgwatch.core.api.dto.ConnectionsResponse
-keepnames class com.mohgwatch.core.api.dto.ConnectionsResponse
-if class com.mohgwatch.core.api.dto.ConnectionsResponse
-keep class com.mohgwatch.core.api.dto.ConnectionsResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
