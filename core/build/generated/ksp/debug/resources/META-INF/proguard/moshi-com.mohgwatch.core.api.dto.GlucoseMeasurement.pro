-if class com.mohgwatch.core.api.dto.GlucoseMeasurement
-keepnames class com.mohgwatch.core.api.dto.GlucoseMeasurement
-if class com.mohgwatch.core.api.dto.GlucoseMeasurement
-keep class com.mohgwatch.core.api.dto.GlucoseMeasurementJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.mohgwatch.core.api.dto.GlucoseMeasurement
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.mohgwatch.core.api.dto.GlucoseMeasurement
-keepclassmembers class com.mohgwatch.core.api.dto.GlucoseMeasurement {
    public synthetic <init>(float,float,java.lang.String,java.lang.String,int,int,java.lang.Integer,boolean,boolean,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
