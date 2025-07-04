package com.psia.pkoc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

/**
 * List Model Adapter
 */
public class ListModelAdapter extends BaseAdapter
{
    private final ArrayList<ListModel> data;
    private static LayoutInflater inflater;
    private final Activity activity;
    ListModel tempValues;

    /**
     * View Holder
     */
    public static class ViewHolder
    {
        /**
         * Address Text
         */
        public TextView AddressText;

        /***
         * Name Text
         */
        public TextView NameText;

        /**
         * Rssi Bar
         */
        public ProgressBar RssiBar;

        /**
         * Activity Indicator
         */
        public ProgressBar ActivityIndicator;

        /**
         * Icon Image
         */
        public ImageView IconImage;
    }

    /**
     * Constructor
     * @param a Parent activity
     */
    public ListModelAdapter(Activity a)
    {
        data = new ArrayList<>();
        activity = a;

        inflater = (LayoutInflater) a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Add
     * @param lm List model
     */
    public void add(ListModel lm)
    {
        data.add(lm);
    }

    /**
     * Remove
     * @param lm List model
     */
    public void remove(ListModel lm)
    {
        data.remove(lm);
    }

    /**
     * Clear list
     */
    public void clear()
    {
        data.clear();
    }

    /**
     * Get count of list
     * @return integer
     */
    @Override
    public int getCount()
    {
        return data.size();
    }

    /**
     * Get item at position
     * @param position Position of the item whose data we want within the adapter's
     * data set.
     * @return Object at nth position in list
     */
    @Override
    public Object getItem (int position)
    {
        return data.get(position);
    }

    /**
     * Get item id
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return Id
     */
    @Override
    public long getItemId (int position)
    {
        return position;
    }

    /**
     * Get view
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@link #getViewTypeCount()} and
     *        {@link #getItemViewType(int)}).
     * @param parent The parent that this view will eventually be attached to
     * @return View
     */
    @Override
    public View getView (int position, View convertView, ViewGroup parent)
    {
        View vi = convertView;
        ViewHolder holder;

        if (convertView == null)
        {
            vi = inflater.inflate(R.layout.list_row, parent, false);

            holder = new ViewHolder();
            holder.AddressText = vi.findViewById(R.id.row_address);
            holder.NameText = vi.findViewById(R.id.row_name);
            holder.RssiBar = vi.findViewById(R.id.row_rssi);
            holder.ActivityIndicator = vi.findViewById(R.id.ctrlActivityIndicator);
            holder.IconImage = vi.findViewById(R.id.imgIcon);

            vi.setTag(holder);
        }
        else
            holder = (ViewHolder) vi.getTag();

        if (!data.isEmpty())
        {
            tempValues = data.get(position);

            holder.NameText.setText(tempValues.getName());
            holder.AddressText.setText(tempValues.getAddress());

            SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
            boolean displayMAC = sharedPref.getBoolean(PKOC_Preferences.DisplayMAC, true);
            boolean enableRanging = sharedPref.getBoolean(PKOC_Preferences.EnableRanging, false);

            if(enableRanging)
            {
                int range = sharedPref.getInt(PKOC_Preferences.RangeValue, 0);
                range *= -5;
                range -= 35;

                if (tempValues.getRssi() >= range)
                    holder.RssiBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(activity.getApplicationContext(), R.color.colorAccent)));
                else
                    holder.RssiBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
            }

            if(!displayMAC)
                holder.AddressText.setVisibility(View.GONE);

            holder.RssiBar.setProgress(tempValues.getRssi() + 120, true);

            holder.IconImage.setImageResource(tempValues.getIconID());

            if (tempValues.getIsBusy())
            {
                holder.IconImage.setVisibility(View.GONE);
                holder.ActivityIndicator.setVisibility(View.VISIBLE);
            }
            else
            {
                holder.IconImage.setVisibility(View.VISIBLE);
                holder.ActivityIndicator.setVisibility(View.GONE);
            }
        }

        return vi;
    }
}
