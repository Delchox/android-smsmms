package com.microspacegames.app.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class UriHelper {

    private static final String TEXT_TREE = "tree";
    private static final Queue<Uri> visitedChilds = new LinkedList<>();

    public static List<Uri> getChildDocuments(ContentResolver contentResolver, Uri parentDirUri, boolean onlyDirectories) {
        final List<Uri> childDocumentIds = new ArrayList<>();

        // Retrieve the document ID of the parent directory
        String documentId = DocumentsContract.getDocumentId(parentDirUri);

        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentDirUri, documentId);

        // Specify the columns you want to retrieve
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE};

        // Execute the query
        Cursor cursor = contentResolver.query(childrenUri, projection, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    // Get the document ID and MIME type of each child
                    String childDocumentId = cursor.getString(0);
                    String mimeType = cursor.getString(1);

                    // Create the child URI using the parent URI and document ID
                    // Uri childUri = ContentUris.withAppendedId(parentUri, Long.parseLong(childDocumentId));
                    Uri childUri = DocumentsContract.buildDocumentUriUsingTree(parentDirUri, childDocumentId);

                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        childDocumentIds.add(childUri);
                    } else if (!onlyDirectories) {
                        childDocumentIds.add(childUri);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return childDocumentIds;
    }

    public static List<Uri> getChildDocumentsOnlyFiles(ContentResolver contentResolver, Uri parentTreeUri) {
        final List<Uri> childDocumentIds = new ArrayList<>();

        // Retrieve the document ID of the parent directory
        String treeDocumentId = DocumentsContract.getDocumentId(parentTreeUri);

        Uri childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentTreeUri, treeDocumentId);

        // Specify the columns you want to retrieve
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE};

        // Execute the query
        Cursor cursor = contentResolver.query(childDocumentsUri, projection, null, null, null);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    // Get the document ID
                    String childDocumentId = cursor.getString(0);
                    // Get the MIME type of each child
                    String mimeType = cursor.getString(1);

                    Uri childDocumentUri = DocumentsContract.buildDocumentUriUsingTree(parentTreeUri, childDocumentId);
                    if (!DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        childDocumentIds.add(childDocumentUri);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return childDocumentIds;
    }

    public static Uri createDirectory(ContentResolver contentResolver, Uri parentUri, String childDirectoryName) throws FileNotFoundException {
        return DocumentsContract.createDocument(
                contentResolver,
                parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                childDirectoryName
        );
    }

    public static Uri createDirectoryPath(ContentResolver contentResolver, Uri parentUri, String directoryPath) throws FileNotFoundException {
        Uri baseDirUri = parentUri;
        String[] directories = directoryPath.split("/");
        for (String childDir : directories) {
            Uri dirUri = findDirectory(contentResolver, baseDirUri, childDir);
            if (dirUri == null)
                baseDirUri = createDirectory(contentResolver, baseDirUri, childDir);
            else
                baseDirUri = dirUri;
        }

        return baseDirUri;
    }

    public static String getDirectoryOfFileUri(String fileStrUri) {
        String filePathUri = null;
        int slashIndex = fileStrUri.lastIndexOf("%2F"); // In octa "%2F" = "/" - in unicode.
        if (slashIndex > 0) {
            filePathUri = fileStrUri.substring(0, slashIndex);
        }
        return filePathUri;
    }

    public static String getDirectoryOfFileUri(Uri fileUri) {
        return getDirectoryOfFileUri(fileUri.toString());
    }

    public static String getParentDirectoryOfDirectoryUri(String directoryUri) {
        String parentDirUri = null;
        int slashIndex = directoryUri.lastIndexOf("%2F"); // In octa "%2F" = "/" - in unicode.
        if (slashIndex > 0) {
            parentDirUri = directoryUri.substring(0, slashIndex);
        }
        return parentDirUri;
    }

    public static Uri findDirectory(ContentResolver contentResolver, Uri parentDirectoryUri, String directoryName) {
        Uri uri = null;

        String[] projection = {DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE};

        List<Uri> childUris = getChildDocuments(contentResolver, parentDirectoryUri, true);

        for (Uri childUri : childUris) {
            Cursor cursor = contentResolver.query(childUri, projection, null, null, null, null);
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        // Get the document ID and MIME type of each child
                        String childDisplayName = cursor.getString(0);
                        String mimeType = cursor.getString(1);

                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                            if (childDisplayName.equals(directoryName)) {
                                uri = childUri;
                                break;
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        return uri;
    }

    public static boolean isExistsDirectoryPath(ContentResolver contentResolver, Uri parentDirUri, String dirPath) {
        boolean exist = true;
        String[] directories = dirPath.split("/");

        Uri subDirUri = parentDirUri;

        for (String nextChildDir : directories) {
            subDirUri = findDirectory(contentResolver, subDirUri, nextChildDir);
            if (subDirUri == null) {
                exist = false;
                break;
            }
        }
        return exist;
    }

    public static boolean isFileExists(ContentResolver contentResolver, Uri dirTreeUri, String fileName) {
        List<Uri> uris = getChildDocumentsOnlyFiles(contentResolver, dirTreeUri);
        for (Uri uri : uris) {
            String name = getFileNameByUri(contentResolver, uri);
            if (name != null) {
                if (fileName.contains("%")) {
                    fileName = Uri.decode(fileName);
                }
                if (name.equals(fileName))
                    return true;
            }
        }

        return false;
    }

    public static boolean isFileExists(ContentResolver contentResolver, String docDirTreeStrUri, String fileName) {
        return isFileExists(contentResolver, Uri.parse(docDirTreeStrUri), fileName);
    }

    public static boolean isTreeUri(Uri docUri) {
        return docUri.getPathSegments().get(0).equals(TEXT_TREE);
    }

    public static boolean isTreeUri(String docStrUri) {
        return isTreeUri(Uri.parse(docStrUri));
    }

    /**
     * Find file by walk all directories beginning from parent directory.
     *
     * @param contentResolver        A content resolver that get access to the content's provider
     * @param parentDirectoryTreeUri A Base directory from that starting the find.
     * @param fileName               A name of finding file.
     * @return A return the file's Uri if the file is finded or null.
     */
    public static Uri findFileTreeUri(ContentResolver contentResolver, Uri parentDirectoryTreeUri, String fileName) {
        Uri fileUri = null;

        visitedChilds.clear();

        visitedChilds.add(parentDirectoryTreeUri);

        while (!visitedChilds.isEmpty()) {
            Uri nextVisitedChild = visitedChilds.remove();

            List<Uri> subChildrens = UriHelper.getChildDocuments(contentResolver, nextVisitedChild, false);

            String[] projection = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
            };

            for (Uri subChild : subChildrens) {
                // Execute the query
                Cursor cursor = contentResolver.query(subChild, projection, null, null, null);

                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            // Get the document ID and MIME type of each child
                            //String childDocumentId = cursor.getString(0);
                            String childName = cursor.getString(1);
                            String mimeType = cursor.getString(2);

                            // Create the child URI using the parent URI and document ID
                            // Uri fileUri = Uri.withAppendedPath(parentDirectory, childDocumentId);

                            if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                                visitedChilds.add(subChild);
                            } else if (childName.equals(fileName)) {
                                fileUri = Uri.parse(subChild.toString()); //DocumentsContract.buildDocumentUriUsingTree(subChild, childDocumentId);
                                break;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }

                if (fileUri != null) break;
            }
            if (fileUri != null) break;
        }

        return fileUri;
    }

    public static Uri findFileTreeUri(ContentResolver contentResolver, String directoryTreeStringUri, String fileName) {
        return findFileTreeUri(contentResolver, Uri.parse(directoryTreeStringUri), fileName);
    }

    public static String getFileNameByUri(ContentResolver contentResolver, Uri fileUri) {
        String name = null;

        try (Cursor cursor = contentResolver.query(fileUri, null, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                // Note it's called "Display Name". This is
                // provider-specific, and might not necessarily be the file name.
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    name = cursor.getString(columnIndex);
                }
            }
        }

        return name;
    }

    public static String getFileNameByUri(ContentResolver contentResolver, String fileStrUri) {
        return getFileNameByUri(contentResolver, Uri.parse(fileStrUri));
    }

    public static boolean copyFile(ContentResolver contentResolver, Uri srcFileUri, Uri destFileUri) {
        boolean success = true;
        try {
            InputStream inputStream = contentResolver.openInputStream(srcFileUri);
            OutputStream outputStream = contentResolver.openOutputStream(destFileUri, "wt");
            if (inputStream != null && outputStream != null) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                inputStream.close();
            }
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        return success;
    }

    public static Uri createEmptyFile(ContentResolver contentResolver, Uri parentDirectoryUri, String fileName) throws FileNotFoundException {
        return DocumentsContract.createDocument(
                contentResolver,
                parentDirectoryUri,
                "vnd.android.document/file",
                fileName
        );
    }

    public static Uri createEmptyFile(ContentResolver contentResolver, String parentDirectoryStrUri, String fileName) throws FileNotFoundException {
        return createEmptyFile(contentResolver, Uri.parse(parentDirectoryStrUri), fileName);
    }

    public static void deleteFileUri(ContentResolver contentResolver, Uri fileUri) throws FileNotFoundException {
        DocumentsContract.deleteDocument(contentResolver, fileUri);
    }

    public static void deleteFileUri(ContentResolver contentResolver, String fileStrUri) throws FileNotFoundException {
        deleteFileUri(contentResolver, Uri.parse(fileStrUri));
    }

    public static String getFileNameByStringUri(String fileStringUri) {
        int slashIndex = fileStringUri.lastIndexOf("%2F"); // %2F = "/"
        if (slashIndex > 0) {
            return fileStringUri.substring(slashIndex + 3);
        }
        return null;
    }

    public static Uri uriToTreeUri(String directoryStrTreeUri, String documentUri) {
        Uri docTreeUri = null;
        Uri dirTreeUri = Uri.parse(directoryStrTreeUri);
        String dirTreeUriId = DocumentsContract.getDocumentId(dirTreeUri);
        Uri docFileUriStr = Uri.parse(documentUri);
        String docFileUriId = DocumentsContract.getDocumentId(docFileUriStr);
        int indexDirTreeUri = docFileUriId.indexOf(dirTreeUriId);
        if (indexDirTreeUri == 0) {
            docTreeUri = DocumentsContract.buildDocumentUriUsingTree(dirTreeUri, docFileUriId);
        }

        return docTreeUri;
    }

    public static String getPath(Uri documentUri) {
        String uriId = DocumentsContract.getDocumentId(documentUri);
        return getRealPath(uriId);
    }

    public static String getPath(String documentUri) {
        String uriId = DocumentsContract.getDocumentId(Uri.parse(documentUri));
        return getRealPath(uriId);
    }

    private static String getRealPath(String documentStrUri) {
        int index = documentStrUri.lastIndexOf(':');
        return documentStrUri.substring(index + 1);
    }

    public static boolean isNestedDir(Uri parentDirUri, Uri docUri) {
        List<String> projectPathSegments = docUri.getPathSegments();
        List<String> projectsPathSegments = parentDirUri.getPathSegments();
        String projectsPathSegment = projectsPathSegments.get(projectsPathSegments.size() - 1);
        String projectPathSegment = projectPathSegments.get(projectPathSegments.size() - 1);

        return projectPathSegment.contains(projectsPathSegment);
    }

    public static boolean isNestedDir(String parentDirStrUri, String docStrUri) {
        return isNestedDir(Uri.parse(parentDirStrUri), Uri.parse(docStrUri));
    }

    public static String getRelativePath(String baseDirTreeStrUri, String docTreeStrUri) {
        String relativePath = null;
        String baseDirPath = getPath(baseDirTreeStrUri);
        String docPath = getPath(docTreeStrUri);

        int indexOfBasePath = docPath.indexOf(baseDirPath);
        if (indexOfBasePath == 0) {
            relativePath = docPath.substring(baseDirPath.length() + 1, docPath.length());
        }

        return relativePath;
    }
}

