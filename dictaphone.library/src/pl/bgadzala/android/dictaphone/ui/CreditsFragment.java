package pl.bgadzala.android.dictaphone.ui;

import pl.bgadzala.android.dictaphone.lib.R;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CreditsFragment extends BaseFragment {

	private static class Credit {
		private String mPerson;
		private String mMerits;

		public Credit(String entry) {
			String[] tokens = entry.split(":");
			mPerson = tokens.length >= 1 ? tokens[0].trim() : "";
			mMerits = tokens.length >= 2 ? tokens[1].trim() : "";
		}

		public String getPerson() {
			return mPerson;
		}

		public String getMerits() {
			return mMerits;
		}

	}

	private static class CreditsAdapter extends ArrayAdapter<Credit> {

		static class ViewHolder {
			private TextView tvPerson;
			private TextView tvMerits;
		}

		private final Context mContext;

		public CreditsAdapter(Context context, String[] entries) {
			super(context, R.layout.credits_item);
			mContext = context;
			addCredits(entries);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.credits_item, parent, false);
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.tvPerson = (TextView) rowView.findViewById(R.id.tv_person);
				viewHolder.tvMerits = (TextView) rowView.findViewById(R.id.tv_merits);
				rowView.setTag(viewHolder);
			}

			Credit credit = getItem(position);
			ViewHolder holder = (ViewHolder) rowView.getTag();
			holder.tvPerson.setText(credit.getPerson());
			holder.tvMerits.setText(credit.getMerits());

			return rowView;
		}

		private void addCredits(String[] entries) {
			setNotifyOnChange(false);
			clear();
			for (int i = 0; i < entries.length; i++) {
				add(new Credit(entries[i]));
			}
			setNotifyOnChange(true);
			notifyDataSetChanged();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_credits, container);

		ListView mLvCredits = (ListView) root.findViewById(R.id.lv_credits);
		mLvCredits.setAdapter(new CreditsAdapter(getActivity(), getResources().getStringArray(R.array.credits)));

		return root;
	}

}
