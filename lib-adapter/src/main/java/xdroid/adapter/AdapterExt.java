package xdroid.adapter;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.Iterator;
import java.util.List;

import xdroid.collections.Indexed;
import xdroid.collections.IndexedIterator;
import xdroid.core.ParcelUtils;

/**
 * @author Oleksii Kropachov (o.kropachov@shamanland.com)
 */
public class AdapterExt<D, V extends View> extends BaseAdapter implements IAdapter<D, V>, Iterable<D>, Parcelable {
    private Indexed<D> mData;
    private ViewBinder<D, V> mBinder;
    private ViewTypeResolver<D> mViewTypeResolver;
    private SparseIntArray mLayoutId;

    private transient boolean mChangesLocked;

    public AdapterExt() {
        mLayoutId = new SparseIntArray();
        mLayoutId.put(0, android.R.layout.simple_list_item_1);
    }

    @SuppressWarnings("unchecked")
    public List<D> getDataList() {
        return List.class.cast(mData);
    }

    public Indexed<D> getData() {
        return mData;
    }

    public void setData(Indexed<D> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public void setBinder(ViewBinder<D, V> binder) {
        mBinder = binder;
        notifyDataSetChanged();
    }

    public void setViewTypeResolver(ViewTypeResolver<D> viewTypeResolver) {
        mViewTypeResolver = viewTypeResolver;
        notifyDataSetChanged();
    }

    public void setLayoutId(int layoutId) {
        putLayoutId(0, layoutId);
    }

    public void putLayoutId(int viewType, int layoutId) {
        mLayoutId.put(viewType, layoutId);
        notifyDataSetChanged();
    }

    public void lockChanges() {
        mChangesLocked = true;
    }

    public void unlockChanges() {
        mChangesLocked = false;
    }

    @Override
    public void notifyDataSetChanged() {
        if (!mChangesLocked) {
            super.notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return mData != null ? mData.size() : 0;
    }

    @Override
    public D getItem(int position) {
        return mData != null ? mData.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getView(int position, View convertView, ViewGroup parent) {
        final V result;

        if (convertView == null) {
            result = (V) LayoutInflater.from(parent.getContext()).inflate(mLayoutId.get(getItemViewType(position)), parent, false);

            if (mBinder != null) {
                mBinder.onNewView(position, result);
            }
        } else {
            result = (V) convertView;
        }

        if (mBinder != null) {
            mBinder.onNewData(position, result, getItem(position));
        }

        return result;
    }

    @SuppressWarnings("unused")
    public D getFirstItem() {
        return getCount() > 0 ? getItem(0) : null;
    }

    @SuppressWarnings("unused")
    public D getLastItem() {
        int count = getCount();
        return count > 0 ? getItem(count - 1) : null;
    }

    @Override
    public Iterator<D> iterator() {
        return new IndexedIterator<D>(mData);
    }

    @Override
    public int getViewTypeCount() {
        return mLayoutId.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mViewTypeResolver != null) {
            return mViewTypeResolver.getViewType(position, getItem(position));
        }

        return super.getItemViewType(position);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        ParcelUtils.writeParcelableOrSerializable(out, flags, mData);
        ParcelUtils.writeParcelableOrSerializable(out, flags, mBinder);
        ParcelUtils.writeParcelableOrSerializable(out, flags, mViewTypeResolver);
        ParcelUtils.writeSparseIntArray(out, mLayoutId);
    }

    public static final Parcelable.Creator<AdapterExt> CREATOR = new Parcelable.Creator<AdapterExt>() {
        public AdapterExt createFromParcel(Parcel in) {
            return new AdapterExt(in);
        }

        public AdapterExt[] newArray(int size) {
            return new AdapterExt[size];
        }
    };

    protected AdapterExt(Parcel in) {
        ClassLoader cl = ((Object) this).getClass().getClassLoader();
        mData = ParcelUtils.readParcelableOrSerializable(in, cl);
        mBinder = ParcelUtils.<ViewBinder<D, V>>readParcelableOrSerializable(in, cl);
        mViewTypeResolver = ParcelUtils.<ViewTypeResolver<D>>readParcelableOrSerializable(in, cl);
        mLayoutId = ParcelUtils.readSparseIntArray(in);
    }
}
