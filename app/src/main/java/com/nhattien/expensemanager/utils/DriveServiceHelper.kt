package com.nhattien.expensemanager.utils

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DriveServiceHelper(private val mDriveService: Drive) {
    private val mExecutor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    fun createFile(filePath: java.io.File, mimeType: String = "application/json"): Task<String> {
        return Tasks.call(mExecutor) {
            val metadata = File()
                .setParents(Collections.singletonList("root"))
                .setMimeType(mimeType)
                .setName("backup_expense_manager.json")

            val fileContent = FileContent(mimeType, filePath)
            val myFile = mDriveService.files().create(metadata, fileContent).execute() ?: throw IOException("Null result when requesting file creation.")
            myFile.id
        }
    }

    /**
     * search for the backup file
     */
    fun searchFile(): Task<List<File>> {
        return Tasks.call(mExecutor) {
            val result = mDriveService.files().list()
                .setQ("name = 'backup_expense_manager.json' and trashed = false")
                .setSpaces("drive")
                .execute()
            result.files
        }
    }

    /**
     * Download file content
     */
    fun downloadFile(fileId: String, targetFile: java.io.File): Task<Void?> {
        return Tasks.call(mExecutor) {
            val outputStream = java.io.FileOutputStream(targetFile)
            mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            outputStream.flush()
            outputStream.close()
            null
        }
    }
    
    /**
     * Updates an existing file in the user's My Drive folder and returns its file ID.
     */
    fun updateFile(fileId: String, filePath: java.io.File, mimeType: String = "application/json"): Task<String> {
        return Tasks.call(mExecutor) {
            val metadata = File()
            val fileContent = FileContent(mimeType, filePath)
             val myFile = mDriveService.files().update(fileId, metadata, fileContent).execute()
            myFile.id
        }
    }
}
