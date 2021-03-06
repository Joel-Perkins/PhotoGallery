package com.bignerdranch.android.photogallery;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joel on 3/24/2016.
 * Commented out code is kept for reference on how to manually fetch images instead of using a
 * library such as Picasso.
 */
public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static final int COLUMN_WIDTH = 150;

    private RecyclerView mPhotoRecyclerView;
    private RecyclerView.Adapter rvAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();
    //    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int pageNum = 1;
    private int lastBindPosition = 0;
    private int numColumns = 3;
    private String prevResultString = "";

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

//        Handler responseHandler = new Handler();
//        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
//        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnaildownloadListener<PhotoHolder>() {
//            @Override
//            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
//                Drawable drawable = new BitmapDrawable(getResources(), bitmap);
//                photoHolder.bindDrawable(drawable);
//            }
//        });
//        mThumbnailDownloader.start();
//        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    pageNum++;
                    String query = QueryPreferences.getStoredQuery(getActivity());
                    new FetchItemsTask(query).execute();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        view.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mPhotoRecyclerView.getWidth() / COLUMN_WIDTH != numColumns) {
                            numColumns = mPhotoRecyclerView.getWidth() / COLUMN_WIDTH;
                            if (mPhotoRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
                                GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
                                layoutManager.setSpanCount(numColumns);
                            } else {
                                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), numColumns));
                            }
                            Log.i(TAG, "Columns: " + numColumns);
                        }
                        Log.i(TAG, "Inside onGlobalLayout()");
                    }
                });

//        setupAdapter();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        mThumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                searchView.clearFocus();
                searchItem.collapseActionView();
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if(PollService.isServiceAlarmOn(getActivity())){
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }

    private void setupAdapter() {
        if (isAdded()) {
            if (rvAdapter == null || mPhotoRecyclerView.getAdapter() == null) {
                rvAdapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(rvAdapter);
            } else {
                lastBindPosition = ((PhotoAdapter) rvAdapter).getLastBindPosition() - 1;
//                rvAdapter.notifyDataSetChanged();
                rvAdapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(rvAdapter);
                mPhotoRecyclerView.scrollToPosition(lastBindPosition);
            }
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        private String mQuery;

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            if (mQuery == null) {
                return new FlickrFetchr().fetchRecentPhotos(pageNum);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery, pageNum);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if(mQuery.equals(prevResultString)){
                mItems.addAll(items);
//                mItems = items;
            } else {
                mItems = items;
            }
            setupAdapter();
            prevResultString = mQuery;
        }
    }

    /**
     * Photo Holder
     */
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            mItemImageView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
            Picasso.with(getActivity())
                    .load(galleryItem.getUrl())
                    .placeholder(R.drawable.loading_image)
                    .into(mItemImageView);
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(),mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    /**
     * Photo Adapter
     */
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        private int lastBindPosition = 1;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
//            Drawable placeHolder = getResources().getDrawable(R.drawable.loading_image);
//            photoHolder.bindDrawable(placeHolder);
//            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            photoHolder.bindGalleryItem(galleryItem);

            if (position + 10 < mGalleryItems.size()) {
                Picasso.with(getActivity())
                        .load(mGalleryItems.get(position + 10).getUrl())
                        .fetch();
            }

            lastBindPosition = position;
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        public int getLastBindPosition() {
            return lastBindPosition;
        }
    }


}
