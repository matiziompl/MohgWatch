-if class com.mohgwatch.core.api.dto.AuthTicket
-keepnames class com.mohgwatch.core.api.dto.AuthTicket
-if class com.mohgwatch.core.api.dto.AuthTicket
-keep class com.mohgwatch.core.api.dto.AuthTicketJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
