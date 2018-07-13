package com.tesco.isearch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by durgapadala on 13/07/18.
 */

public class CustomAdapter extends BaseAdapter {
    Context context;
    LayoutInflater inflter;
    List<Product> products;

    public CustomAdapter(Context applicationContext, List<Product> products) {
        this.products = products;
        this.context = applicationContext;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public Object getItem(int i) {
        return products.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflter.inflate(R.layout.search_results, null);
        TextView textView = view.findViewById(R.id.nameView);
        textView.setText(products.get(i).getName());
        TextView priceView = view.findViewById(R.id.priceView);
        priceView.setText(products.get(i).getPrice());
        TextView labelView = view.findViewById(R.id.labelView);
        labelView.setText(products.get(i).getLabels());
        ImageView imageView = view.findViewById(R.id.imageView);
        if (products.get(i).getImageUrl() != null) {
            new ImageLoadTask(products.get(i).getImageUrl(), imageView).execute();
        }
        return view;
    }

    class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private final String url;
        private final ImageView imageView;

        ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }

    }
}
