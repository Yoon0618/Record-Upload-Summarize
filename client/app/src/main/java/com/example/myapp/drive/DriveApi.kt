package com.example.myapp.drive
private const val BASE = "https://www.googleapis.com/drive/v3"
private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"


fun ensureFolder(token: String, name: String = "myapp"): String {
// 1) 검색
    val q = "name = '$name' and mimeType = 'application/vnd.google-apps.folder' and 'root' in parents and trashed = false"
    val url = "$BASE/files?q=${URLEncoder.encode(q, "UTF-8")}&fields=files(id,name)"
    val listReq = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $token")
        .get()
        .build()
    client.newCall(listReq).execute().use { resp ->
        if (!resp.isSuccessful) error("List failed: ${resp.code}")
        val arr = JSONObject(resp.body!!.string()).optJSONArray("files")
        if (arr != null && arr.length() > 0) {
            return arr.getJSONObject(0).getString("id")
        }
    }
// 2) 생성
    val meta = JSONObject().apply {
        put("name", name)
        put("mimeType", "application/vnd.google-apps.folder")
    }
    val createReq = Request.Builder()
        .url("$BASE/files")
        .addHeader("Authorization", "Bearer $token")
        .post(meta.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()
    client.newCall(createReq).execute().use { resp ->
        if (!resp.isSuccessful) error("Create folder failed: ${resp.code}")
        val id = JSONObject(resp.body!!.string()).getString("id")
        return id
    }
}


fun uploadMultipart(
    token: String,
    folderId: String,
    displayName: String,
    mimeType: String,
    contentResolver: ContentResolver,
    contentUri: Uri,
): String {
    val metadata = JSONObject().apply {
        put("name", displayName)
        put("parents", listOf(folderId))
    }.toString()


    val metaBody = metadata.toRequestBody("application/json; charset=utf-8".toMediaType())
    val input = contentResolver.openInputStream(contentUri) ?: error("InputStream null")
    val mediaBody: RequestBody = InputStreamRequestBody(mimeType, input)


    val multipart = MultipartBody.Builder()
        .setType("multipart/related".toMediaType())
        .addPart(MultipartBody.Part.create(metaBody))
        .addPart(MultipartBody.Part.create(mediaBody))
        .build()


    val req = Request.Builder()
        .url("$UPLOAD/files?uploadType=multipart&fields=id")
        .addHeader("Authorization", "Bearer $token")
        .post(multipart)
        .build()


    client.newCall(req).execute().use { resp ->
        if (!resp.isSuccessful) error("Upload failed: ${resp.code}")
        return JSONObject(resp.body!!.string()).getString("id")
    }
}
}