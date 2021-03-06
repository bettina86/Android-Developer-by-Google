/*
* Copyright (C) 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.todolist.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.example.android.todolist.data.TaskContract.TaskEntry;

public class TaskContentProvider extends ContentProvider {

    // Define final integer constants for the directory of tasks and a single item.
    // It's convention to use 100, 200, 300, etc for directories,
    // and related ints (101, 102, ..) for items in that directory.
    public static final int TASKS = 100;
    public static final int TASK_WITH_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    /**
     * Initialize a new matcher object without any matches,
     * then use .addURI(String authority, String path, int match) to add matches
     */
    public static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        /**
         * All paths added to the UriMatcher have a corresponding int.
         * For each kind of uri you may want to access, add the corresponding match with addURI.
         * The two calls below add matches for the task directory and a single item by ID.
         */
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.PATH_TASKS, TASKS);
        uriMatcher.addURI(TaskContract.AUTHORITY, TaskContract.PATH_TASKS + "/#", TASK_WITH_ID);
        return uriMatcher;
    }

    private TaskDbHelper mTaskDbHelper;

    /**
     * onCreate() is where you should initialize anything you’ll need to setup
     * your underlying data source.
     * In this case, you’re working with a SQLite database, so you’ll need to
     * initialize a DbHelper to gain access to it.
     */
    @Override
    public boolean onCreate() {
        this.mTaskDbHelper = new TaskDbHelper(getContext());
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        final SQLiteDatabase database = mTaskDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        switch (match) {
            case TASKS:
                long id = database.insertOrThrow(TaskEntry.TABLE_NAME, null, values);
                notifyChangeForUri(uri);
                return ContentUris.withAppendedId(TaskEntry.CONTENT_URI, id);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase database = mTaskDbHelper.getReadableDatabase();
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                cursor = database.query(TaskEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case TASK_WITH_ID:
                selection = TaskEntry._ID + "=?";
                selectionArgs = new String[]{idStringFrom(uri)};
                cursor = database.query(TaskEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        setNotificationUri(uri, cursor);

        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = mTaskDbHelper.getWritableDatabase();
        int rowsDeleted = 0;

        switch (sUriMatcher.match(uri)) {
            case TASKS:
                rowsDeleted = database.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TASK_WITH_ID:
                selection = TaskEntry._ID + "=?";
                selectionArgs = new String[]{idStringFrom(uri)};
                rowsDeleted = database.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed.
        if (rowsDeleted != 0) {
            notifyChangeForUri(uri);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int rowsUpdated = 0;
        switch (sUriMatcher.match(uri)) {
            case TASK_WITH_ID:
                selection = TaskEntry._ID + "=?";
                selectionArgs = new String[]{idStringFrom(uri)};
                rowsUpdated = mTaskDbHelper.getWritableDatabase().update(TaskEntry.TABLE_NAME,
                        values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            notifyChangeForUri(uri);
        }

        return rowsUpdated;
    }

    /**
     * getType() handles requests for the MIME type of data
     * We are working with two types of data:
     * 1) a directory and 2) a single row of data.
     * This method will not be used in our app, but gives a way to standardize the data formats
     * that your provider accesses, and this can be useful for data organization.
     * For now, this method will not be used but will be provided for completeness.
     */
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TASKS:
                return "vnd.android.cursor.dir" + "/" + TaskContract.AUTHORITY + "/" + TaskContract.PATH_TASKS;
            case TASK_WITH_ID:
                return "vnd.android.cursor.item" + "/" + TaskContract.AUTHORITY + "/" + TaskContract.PATH_TASKS;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    private void notifyChangeForUri(Uri uri) {
        Context context = getContext();
        if (context != null) context.getContentResolver().notifyChange(uri, null);
    }

    private void setNotificationUri(Uri uri, Cursor cursor) {
        Context context = getContext();
        if (context != null) cursor.setNotificationUri(context.getContentResolver(), uri);
    }

    private String idStringFrom(Uri uri) {
        return String.valueOf(ContentUris.parseId(uri));
    }

}
