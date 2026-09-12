-if class com.mohgwatch.core.api.dto.LoginData
-keepnames class com.mohgwatch.core.api.dto.LoginData
-if class com.mohgwatch.core.api.dto.LoginData
-keep class com.mohgwatch.core.api.dto.LoginDataJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
-if class com.mohgwatch.core.api.dto.LoginData
-keepnames class kotlin.jvm.internal.DefaultConstructorMarker
-if class com.mohgwatch.core.api.dto.LoginData
-keepclassmembers class com.mohgwatch.core.api.dto.LoginData {
    public synthetic <init>(com.mohgwatch.core.api.dto.UserData,com.mohgwatch.core.api.dto.AuthTicket,boolean,java.lang.String,int,kotlin.jvm.internal.DefaultConstructorMarker);
}
