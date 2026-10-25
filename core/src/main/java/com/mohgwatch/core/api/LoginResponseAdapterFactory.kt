package com.mohgwatch.core.api

import com.mohgwatch.core.api.dto.LoginResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

internal object LoginResponseAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != LoginResponse::class.java || annotations.isNotEmpty()) return null
        val delegate = moshi.nextAdapter<LoginResponse>(this, type, annotations)
        return object : JsonAdapter<LoginResponse>() {
            override fun fromJson(reader: JsonReader): LoginResponse? {
                // Non-success responses have different data shapes and may contain no session.
                val status = reader.peekJson().use { preview ->
                    var status: Int? = null
                    preview.beginObject()
                    while (preview.hasNext()) {
                        if (preview.nextName() == "status") status = preview.nextInt()
                        else preview.skipValue()
                    }
                    preview.endObject()
                    status ?: throw JsonDataException("Required value 'status' missing at $")
                }
                if (status != 0) {
                    reader.skipValue()
                    return LoginResponse(status, null)
                }
                return delegate.fromJson(reader)
            }

            override fun toJson(writer: JsonWriter, value: LoginResponse?) = delegate.toJson(writer, value)
        }
    }
}
