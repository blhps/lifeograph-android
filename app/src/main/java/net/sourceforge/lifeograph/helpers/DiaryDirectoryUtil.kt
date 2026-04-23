/***************************************************************************************************
Copyright (C) 2026. Ahmet Öztürk (aoz_2@yahoo.com)

This file is part of Lifeograph.

Lifeograph is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Lifeograph is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************************************/

package net.sourceforge.lifeograph.helpers

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile


object DiaryDirectoryUtil {
    /**
     * lists all files in a tree URI directory
     */
    fun listFiles(context: Context, treeUri: Uri): List<Pair<String, Uri>> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
                                                                           )
        val results = mutableListOf<Pair<String, Uri>>()
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID
                   ),
            null, null, null
                                     )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                val docId = cursor.getString(1)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                results.add(name to fileUri)
            }
        }
        return results
    }

    fun getTreeUriFromFileUri(fileUri: Uri): Uri {
        val docId = DocumentsContract.getDocumentId(fileUri)
        val treeId = docId.substringBeforeLast("/") // split at last '/'
        // Reconstruct the tree URI
        return DocumentsContract.buildTreeDocumentUri(fileUri.authority, treeId)
    }

    /**
     * resolves a sibling file URI by name within the same tree
     * returns null if the file doesn't exist
     */
    fun findFileInTree(context: Context, treeUri: Uri, fileName: String): Uri? {
        return listFiles(context, treeUri).firstOrNull { it.first == fileName }?.second
    }

    /**
     * checks whether a lock file exists for the given diary file name
     */
    fun isLocked(context: Context, fileUri: Uri, suffix: String): Boolean {
        val treeUri = getTreeUriFromFileUri(fileUri)
        val diaryFileName = FileUtil.getFileName(fileUri, context)
        val lockName = "$diaryFileName.$suffix"
        return findFileInTree(context, treeUri, lockName) != null
    }

    /**
     * creates and writes a lock file in the same directory
     */
    fun createLockFile(context: Context, fileUri: Uri, suffix: String): DocumentFile? {
        val treeUri = getTreeUriFromFileUri(fileUri)
        val diaryFileName = FileUtil.getFileName(fileUri, context)
        val lockName = "$diaryFileName$suffix"
        val treeDocFile = DocumentFile.fromTreeUri(context, treeUri) ?: return null

        return try {
            // delete existing lock file if present as overwriting is not supported:
            treeDocFile.findFile(lockName)?.delete()

            treeDocFile.createFile("application/octet-stream", lockName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * deletes the lock file when done editing
     */
    fun deleteLockFile(context: Context, fileUri: Uri, suffix: String): Boolean {
        val treeUri = getTreeUriFromFileUri(fileUri)
        val diaryFileName = FileUtil.getFileName(fileUri, context)
        val lockName = "$diaryFileName$suffix"
        val lockUri = findFileInTree(context, treeUri, lockName)
            ?: return true  // already gone, that's fine
        return try {
            DocumentsContract.deleteDocument(context.contentResolver, lockUri)
        } catch (e: Exception) {
            false
        }
    }
}