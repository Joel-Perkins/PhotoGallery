package com.bignerdranch.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joel on 3/24/2016.
 */
public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static final int COLUMN_WIDTH = 150;

    private RecyclerView mPhotoRecyclerView;
    RecyclerView.Adapter rvAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int pageNum = 1;
    private int lastBindPosition = -1;
    private int numColumns = 3;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();
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
                    new FetchItemsTask().execute();
                }
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

        setupAdapter();

        return view;
    }

    private void setupAdapter(){
        if (isAdded()){
            if(rvAdapter == null || mPhotoRecyclerView.getAdapter() == null){
                rvAdapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(rvAdapter);
            } else {
                lastBindPosition = ((PhotoAdapter)rvAdapter).getLastBindPosition() - 1;
                rvAdapter.notifyDataSetChanged();
                mPhotoRecyclerView.scrollToPosition(lastBindPosition);
            }
        }
    }

    private class FetchItemsTask extends AsyncTask<Void,Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems(pageNum);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
//            mItems = items;
            mItems.addAll(items);
            setupAdapter();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;

        public PhotoHolder(View itemView){
            super(itemView);
            mTitleTextView = (TextView) itemView;
        }

        public void bindGalleryItem(GalleryItem item){
            mTitleTextView.setText(item.toString());
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        private int lastBindPosition = -1;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView galleryItem = new TextView(getActivity());
            return new PhotoHolder(galleryItem);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
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
