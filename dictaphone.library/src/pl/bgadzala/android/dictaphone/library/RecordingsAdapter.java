package pl.bgadzala.android.dictaphone.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import pl.bgadzala.android.dictaphone.Logger;
import pl.bgadzala.android.dictaphone.lib.R;

import java.util.Collection;
import java.util.List;

public class RecordingsAdapter extends ArrayAdapter<Recording> {

    private final Context mContext;
    private RecordingsLibrary mLibrary;
    private OnRecordingSelectionChangedListener mOnRecordingSelectionChangedListener;
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Recording recording = (Recording) buttonView.getTag();
            onRecordingCheckedChanged(recording, isChecked);
        }
    };

    public RecordingsAdapter(Context context) {
        super(context, R.layout.filelist_item);
        mContext = context;
        mLibrary = new RecordingsLibrary();
    }

    public void setOnRecordingSelectionChangedListener(OnRecordingSelectionChangedListener onRecordingSelectionChangedListener) {
        mOnRecordingSelectionChangedListener = onRecordingSelectionChangedListener;
    }

    public RecordingsLibrary getLibrary() {
        return mLibrary;
    }

    public void refresh() {
        setNotifyOnChange(false);
        synchronized (mLibrary) {
            if (mLibrary.scan()) {
                Logger.debug("Refreshing recordings list");
                clear();
                List<Recording> elements = mLibrary.elements();
                synchronized (elements) {
                    addAll(elements);
                }
            }
        }
    }

    public void deselectAll() {
        synchronized (mLibrary) {
            mLibrary.delesectAll();
            notifyDataSetChanged();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Recording recording = position < getCount() ? getItem(position) : null;

        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.filelist_item, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.cbSelected = (CheckBox) rowView.findViewById(R.id.cb_selected);
            viewHolder.cbSelected.setTag(recording);
            viewHolder.cbSelected.setOnCheckedChangeListener(mOnCheckedChangeListener);
            viewHolder.tvName = (TextView) rowView.findViewById(R.id.tv_name);
            viewHolder.tvDirectory = (TextView) rowView.findViewById(R.id.tv_directory);
            rowView.setTag(viewHolder);
        } else {
            ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.cbSelected.setTag(recording);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.cbSelected.setChecked(recording != null && recording.isSelected());
        holder.tvName.setText(recording != null ? recording.getFullName() : "???");
        holder.tvDirectory.setText(recording != null ? recording.getDirectory() : "???");

        return rowView;
    }

    public void destroy() {
        clear();
        destroyLibrary();
    }

    @Override
    public void addAll(Collection<? extends Recording> collection) {
        try {
            super.addAll(collection);
        } catch (NoSuchMethodError ex) {
            for (Recording recording : collection) {
                super.add(recording);
            }
        }
    }

    private void destroyLibrary() {
        if (mLibrary != null) {
            synchronized (mLibrary) {
                mLibrary.destroy();
                mLibrary = null;
            }
        }
    }

    private void onRecordingCheckedChanged(Recording recording, boolean isChecked) {
        recording.setSelected(isChecked);
        if (mOnRecordingSelectionChangedListener != null) {
            mOnRecordingSelectionChangedListener.onRecordingSelectionChanged(recording);
        }
    }

    public interface OnRecordingSelectionChangedListener {
        void onRecordingSelectionChanged(Recording recording);
    }

    static class ViewHolder {
        private CheckBox cbSelected;
        private TextView tvName;
        private TextView tvDirectory;
    }

}
