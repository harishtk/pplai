package com.aiavatar.app.commons.util.storage

import android.net.Uri
import java.io.File

data class SavedFileResult(
    val folderName: String,
    val savedFiles: List<File>,
    val originalFiles: List<Uri>
)
