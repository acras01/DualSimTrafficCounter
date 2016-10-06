package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.squareup.picasso.Transformation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ua.od.acros.dualsimtrafficcounter.R;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.ViewHolder> {


    private List<ListItem> mList;
    private Picasso mPicasso;
    private Context mContext;

    /** Uri scheme for app icons */
    private static final String SCHEME_APP_ICON = "app_icon";
    private static final String SCHEME_CONTACT_PHOTO = "contact_photo";


    public MyListAdapter(List<ListItem> list) {
        if (list != null)
            this.mList = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AppCompatCheckBox checkBox;
        TextView txtViewName;
        TextView txtViewNumber;
        ImageView imgIcon;


        ViewHolder(View v) {
            super(v);
            checkBox = (AppCompatCheckBox) v.findViewById(R.id.checkBox);
            txtViewName = (TextView) v.findViewById(R.id.name);
            txtViewNumber = (TextView) v.findViewById(R.id.number);
            imgIcon = (ImageView) v.findViewById(R.id.icon);
        }
    }

    @Override
    public MyListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_list_row, parent, false);

        // тут можно программно менять атрибуты лэйаута (size, margins, paddings и др.)
        ViewHolder viewHolder = new ViewHolder(v);
        final CheckBox checkBox = viewHolder.checkBox;
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((ListItem) buttonView.getTag()).setChecked(isChecked);
            }
        });
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((ListItem) v.getTag()).isChecked();
                ((ListItem) v.getTag()).setChecked(!isChecked);
                checkBox.setChecked(!isChecked);
            }
        };
        viewHolder.txtViewName.setOnClickListener(click);
        viewHolder.txtViewNumber.setOnClickListener(click);
        viewHolder.imgIcon.setOnClickListener(click);
        mContext = CustomApplication.getAppContext();
        Picasso.Builder builder = new Picasso.Builder(mContext);
        builder.addRequestHandler(new CustomRequestHandler(mContext));
        mPicasso = builder.build();
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.txtViewName.setText(mList.get(position).getName());
        holder.txtViewName.setTag(mList.get(position));
        holder.txtViewNumber.setText(mList.get(position).getNumber());
        holder.txtViewNumber.setTag(mList.get(position));
        holder.checkBox.setTag(mList.get(position));
        holder.checkBox.setChecked(mList.get(position).isChecked());
        int dim = (int) mContext.getResources().getDimension(R.dimen.logo_size);
        Uri icon = mList.get(position).getIcon();
        if (icon.toString().contains(SCHEME_CONTACT_PHOTO))
            mPicasso.load(icon)
                    .resize(dim, dim)
                    .transform(new CircularTransformation(0))
                    .centerInside()
                    //.error(R.drawable.ic_person_black_24dp)
                    .into(holder.imgIcon);
        else
            mPicasso.load(icon)
                    .resize(dim, dim)
                    .centerInside()
                    //.error(R.drawable.ic_android_black_24dp)
                    .into(holder.imgIcon);
        //holder.imgIcon.setImageDrawable(mList.get(position).getIcon());
    }

    public ArrayList<String> getCheckedItems(){
        ArrayList<String> list = new ArrayList<>();
        for (ListItem item : mList)
            if (item.isChecked())
                list.add(item.getNumber());
        return list;
    }

    // кол-во элементов
    @Override
    public int getItemCount() {
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    // элемент по позиции
    public ListItem getItem(int position) {
        return mList.get(position);
    }

    // id по позиции
    @Override
    public long getItemId(int position) {
        return position;
    }

    public void swapItems(List<ListItem> list) {
        if (list != null)
            this.mList = list;
        notifyDataSetChanged();
    }

    private class CustomRequestHandler extends RequestHandler {

        private PackageManager pm;
        private Context ctx;

        CustomRequestHandler(Context context) {
            ctx = context;
            pm = context.getPackageManager();
        }

        @Override
        public boolean canHandleRequest(Request data) {
            // only handle Uris matching our scheme
            return data.uri.getScheme().contains(SCHEME_APP_ICON) || data.uri.getScheme().contains(SCHEME_CONTACT_PHOTO);
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            Bitmap bmp = null;
            if (request.uri.toString().contains(SCHEME_APP_ICON)) {
                try {
                    bmp = ((BitmapDrawable) pm.getApplicationIcon(request.uri.toString().replace(SCHEME_APP_ICON + "://", ""))).getBitmap();
                } catch (Exception e) {
                }
                if (bmp != null)
                    return new Result(bmp, Picasso.LoadedFrom.DISK);
            } else if (request.uri.toString().contains(SCHEME_CONTACT_PHOTO)) {
                boolean choice = false;
                long contactID = Long.parseLong(request.uri.toString().replace(SCHEME_CONTACT_PHOTO + "://", ""));
                Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID);
                Uri photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                Cursor cursor = ctx.getContentResolver().query(photoUri,
                        new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                if (cursor != null)
                    try {
                        if (cursor.moveToFirst()) {
                            byte[] data = cursor.getBlob(0);
                            if (data != null)
                                return new Result(new ByteArrayInputStream(data), Picasso.LoadedFrom.DISK);
                            else
                                choice = true;
                        } else
                            choice = true;
                    } finally {
                        cursor.close();
                    }
                else
                    choice = true;
                if (choice) {
                    bmp = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_contact_picture);
                    return new Result(bmp, Picasso.LoadedFrom.DISK);
                }
            }
            return null;
        }
    }

    private class CircularTransformation implements Transformation {

        private int mRadius = 10;

        CircularTransformation(final int radius) {
            this.mRadius = radius;
        }

        @Override
        public Bitmap transform(final Bitmap source) {
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
            final Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(output);
            if (mRadius == 0)
                canvas.drawCircle(source.getWidth() / 2, source.getHeight() / 2, source.getWidth() / 2, paint);
            else
                canvas.drawCircle(source.getWidth() / 2, source.getHeight() / 2, mRadius, paint);
            if (source != output)
                source.recycle();
            return output;
        }

        @Override
        public String key() {
            return "circular" + String.valueOf(mRadius);
        }
    }
}
