-if class com.mohgwatch.core.api.dto.Connection
-keepnames class com.mohgwatch.core.api.dto.Connection
-if class com.mohgwatch.core.api.dto.Connection
-keep class com.mohgwatch.core.api.dto.ConnectionJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.mohgwatch.core.api.dto.Connection
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.mohgwatch.core.api.dto.Connection
-keepclassmembers class com.mohgwatch.core.api.dto.Connection {
    public synthetic <init>(java.lang.String,java.lang.String,java.lang.String,java.lang.Integer,java.lang.Integer,java.lang.Integer,com.mohgwatch.core.api.dto.GlucoseMeasurement,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
