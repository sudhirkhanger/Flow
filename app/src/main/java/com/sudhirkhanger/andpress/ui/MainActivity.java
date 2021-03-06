/*
 * Copyright 2017 Sudhir Khanger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sudhirkhanger.andpress.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sudhirkhanger.andpress.R;
import com.sudhirkhanger.andpress.adapter.WordPressPostAdapter;
import com.sudhirkhanger.andpress.model.Post;
import com.sudhirkhanger.andpress.model.PostColumns;
import com.sudhirkhanger.andpress.model.PostProvider;
import com.sudhirkhanger.andpress.rest.WordPressAsyncTask;
import com.sudhirkhanger.andpress.rest.WordPressResponse;
import com.sudhirkhanger.andpress.widget.AndPressWidgetProvider;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ACTION_DATA_UPDATED =
            "com.sudhirkhanger.andpress.ACTION_DATA_UPDATED";

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String WP_SITE_URL = "https://api.myjson.com/bins/gqho3";

    private RecyclerView recyclerView;
    private WordPressPostAdapter wordPressPostAdapter;
    private static final int WORDPRESS_LOADER_ID = 0;

    public static final String[] POST_COLUMNS = {
            PostColumns._ID,
            PostColumns.POST_ID,
            PostColumns.TITLE,
            PostColumns.IMAGE_URL,
            PostColumns.CONTENT
    };

    private static boolean itemsAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("5958710972D2BCB58D67C91E95E24602")
                .build();
        mAdView.loadAd(adRequest);

        recyclerView = (RecyclerView)
                findViewById(R.id.recyclerview);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this));

        wordPressPostAdapter = new WordPressPostAdapter(
                this, null);

        recyclerView.setAdapter(
                new WordPressPostAdapter(this, null));

        if (!itemsAvailable) {
            update();
        }

        getSupportLoaderManager().initLoader(WORDPRESS_LOADER_ID, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void update() {
        new WordPressAsyncTask(new WordPressResponse() {
            @Override
            public void processFinish(ArrayList<Post> postArrayList) {
                insertData(postArrayList);
            }
        }).execute(WP_SITE_URL);
        itemsAvailable = true;
        Toast.makeText(this, "Refreshing", Toast.LENGTH_SHORT).show();
        Log.e(LOG_TAG, "update()");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            update();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void insertData(ArrayList<Post> postArrayList) {
        Log.d(LOG_TAG, "insert");
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>(10);

        for (Post post : postArrayList) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                    PostProvider.Posts.CONTENT_URI);
            builder.withValue(PostColumns.POST_ID, post.getId());
            builder.withValue(PostColumns.TITLE, post.getTitle());
            builder.withValue(PostColumns.IMAGE_URL, post.getImage_url());
            builder.withValue(PostColumns.CONTENT, post.getContent());
            batchOperations.add(builder.build());
        }

        try {
            getContentResolver().applyBatch(PostProvider.AUTHORITY, batchOperations);
        } catch (RemoteException | OperationApplicationException e) {
            Log.e(LOG_TAG, "Error applying batch insert", e);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                PostProvider.Posts.CONTENT_URI,
                POST_COLUMNS,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        recyclerView.setAdapter(new WordPressPostAdapter(this, cursor));
        updateWidgets();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        wordPressPostAdapter.swapCursor(null);
    }

    private void updateWidgets() {
        ComponentName name = new ComponentName(this, AndPressWidgetProvider.class);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(name);

        Intent intent = new Intent(this, AndPressWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
}
