// TAKEN FROM https://www.b4x.com/android/forum/threads/how-to-get-full-path-from-uri.89974/

package net.sourceforge.lifeograph.helpers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

@SuppressLint("NewApi")
public final class FileUtil {

    static String TAG="TAG";
    private static final String PRIMARY_VOLUME_NAME = "primary";



    public static boolean isKitkat() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }
    public static boolean isAndroid5() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    @NonNull
    public static String getSdCardPath() {
        String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        try {
            sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
        }
        catch (IOException ioe) {
            Log.e(TAG, "Could not get SD directory", ioe);
        }
        return sdCardDirectory;
    }


    public static ArrayList<String> getExtSdCardPaths(Context con) {
        ArrayList<String> paths = new ArrayList<>();
        File[] files = ContextCompat.getExternalFilesDirs( con, "external");
        File firstFile = files[0];
        for (File file : files) {
            if (file != null && !file.equals(firstFile)) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w("", "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    }
                    catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri), con);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if ( !documentPath.isEmpty() ) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            }
            else {
                return volumePath + File.separator + documentPath;
            }
        }
        else {
            return volumePath;
        }
    }


    private static String getVolumePath(final String volumeId, Context con) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }

        try {
            StorageManager mStorageManager =
                    (StorageManager) con.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        }
        else {
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        }
        else {
            return File.separator;
        }
    }

    public static String getFileName( Uri uri, Context context ) {
        String result = null;
        if( Objects.equals( uri.getScheme(), "content" ) ) {
            try( Cursor cursor = context.getContentResolver()
                                        .query( uri, null, null, null, null ) ) {
                if( cursor != null && cursor.moveToFirst() ) {
                    result = cursor.getString(
                            cursor.getColumnIndex( OpenableColumns.DISPLAY_NAME ) );
                }
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
        }
        if( result == null ) {
            result = uri.getPath();
            int cut = result.lastIndexOf( File.separator );
            if( cut != -1 ) {
                result = result.substring( cut + 1 );
            }
        }
        return result;
    }

    public static String getRealPathFromURI( Uri contentUri, Context context ) {
        Cursor cursor = null;
        try {
            String[] pathColumn = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query( contentUri, pathColumn, null, null, null );
            int column_index = cursor.getColumnIndexOrThrow( MediaStore.Images.Media.DATA );
            cursor.moveToFirst();
            return cursor.getString( column_index );
        }
        finally {
            if( cursor != null ) {
                cursor.close();
            }
        }

//        File file = new File(contentUri.getPath());//create path from uri
//        final String[] split = file.getPath().split(":");//split the path.
//        return split[1];//assign it to a string(your choice).

    }


//    public static String getFilePathFromURI(Context context, Uri contentUri) {
//        String TEMP_DIR_PATH =
//                Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOCUMENTS ).getPath();
//        //copy file and send new file path
//        String fileName = getFileName(contentUri);
//        if (!TextUtils.isEmpty( fileName)) {
//            File copyFile = new File(TEMP_DIR_PATH + File.separator + fileName);
//            copy(context, contentUri, copyFile);
//            return copyFile.getAbsolutePath();
//        }
//        return null;
//    }
//
//    public static String getFileName(Uri uri) {
//        if (uri == null) return null;
//        String fileName = null;
//        String path = uri.getPath();
//        int cut = path.lastIndexOf('/');
//        if (cut != -1) {
//            fileName = path.substring(cut + 1);
//        }
//        return fileName;
//    }
//
//    public static void copy(Context context, Uri srcUri, File dstFile) {
//        try {
//            InputStream inputStream = context.getContentResolver().openInputStream( srcUri);
//            if (inputStream == null) return;
//            OutputStream outputStream = new FileOutputStream( dstFile);
//            IOUtils.copyStream(inputStream, outputStream);
//            inputStream.close();
//            outputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}


