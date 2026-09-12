-if class com.mohgwatch.core.api.dto.GraphData
-keepnames class com.mohgwatch.core.api.dto.GraphData
-if class com.mohgwatch.core.api.dto.GraphData
-keep class com.mohgwatch.core.api.dto.GraphDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.mohgwatch.core.api.dto.GraphData
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.mohgwatch.core.api.dto.GraphData
-keepclassmembers class com.mohgwatch.core.api.dto.GraphData {
    public synthetic <init>(com.mohgwatch.core.api.dto.Connection,java.util.List,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
