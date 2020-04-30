package com.example.sequencemultiplayer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.util.ArrayList;

import static com.example.sequencemultiplayer.FirebaseAuthUser.getImgProfilePicURL;

public class PlayersInRoomAdapter extends ArrayAdapter {

    private Activity context;
    int resourceID;
    ArrayList<PlayerDetails> playerDetailsList;

    public PlayersInRoomAdapter(Activity context, int resourceID, ArrayList<PlayerDetails> playerDetailsList) {
        super(context, resourceID, playerDetailsList);
        this.context = context;
        this.resourceID = resourceID;
        this.playerDetailsList = playerDetailsList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        ViewHolder holder;

        Log.d("Adapter", "Codebase 5");
        if (convertView == null) {
            Log.d("Adapter", "Codebase 6");
            convertView = LayoutInflater.from(context).inflate(R.layout.player_in_roomlist_row, null);
            holder = new ViewHolder();
            holder.mImageView = (ImageView) convertView.findViewById(R.id.imgProfilePic);
            convertView.setTag(holder);
        }
        else {
            Log.d("Adapter", "Codebase 6");
            holder = (ViewHolder) convertView.getTag();
        }
        Log.d("Adapter", "Codebase 7");

        //fetching your data object with the current position
        PlayerDetails playerDetailsObject = this.playerDetailsList.get(position);

        TextView player_name = (TextView)convertView.findViewById(R.id.playerName);
        player_name.setText(playerDetailsObject.getPlayerName());
        Log.d("Adapter", "Codebase 8");

        String img_url = playerDetailsObject.getImgProfilePicURL();

        if (!img_url.equals("")){
            Picasso.with(context).load(img_url).into(holder.mImageView);
            Log.d("Adapter", "Codebase 9");
        }
        else {
            Picasso.with(context).load(R.drawable.user_default).into(holder.mImageView);
            Log.d("Adapter", "Codebase 10");
        }
        Log.d("Adapter", "Codebase 11");
        return convertView;
    }

    private static class ViewHolder {
        ImageView mImageView;
    }

    public class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... uri) {
            String url = uri[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                Bitmap resized = Bitmap.createScaledBitmap(result,200,200, true);
                bmImage.setImageBitmap(ImageHelper
                        .getRoundedCornerBitmap(context, resized,250,200,200,
                                false, false, false, false));
            }
        }
    }
}
