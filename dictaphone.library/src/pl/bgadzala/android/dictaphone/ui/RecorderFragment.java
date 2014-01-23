package pl.bgadzala.android.dictaphone.ui;

import pl.bgadzala.android.dictaphone.Logger;
import pl.bgadzala.android.dictaphone.lib.R;
import pl.bgadzala.android.dictaphone.service.RecorderService;
import pl.bgadzala.android.dictaphone.service.RecorderService.RecorderListener;
import pl.bgadzala.android.dictaphone.service.RecorderService.State;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RecorderFragment extends BaseFragment implements RecorderListener {

	private class RecorderServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRecorderService = ((RecorderService.LocalBinder) service).getService();
			mRecorderService.addRecorderListener(RecorderFragment.this);
			if (getActivity() != null) {
				mRecorderService.setActivityClass(getActivity().getClass());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mRecorderService.removeRecorderListener(RecorderFragment.this);
			mRecorderService = null;
		}
	}

	private RecorderService mRecorderService;
	private RecorderServiceConnection mRecorderServiceConnection = new RecorderServiceConnection();

	private ImageButton mBtnRecord;
	private ImageButton mBtnPause;
	private ImageButton mBtnStop;
	private ImageView mIvRecordingIndicator;
	private TextView mTvClock;
	private ProgressBar mPbAmplitude;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_recorder, container);

		mBtnRecord = (ImageButton) root.findViewById(R.id.btn_record);
		mBtnRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onRecordBtnClicked();
			}
		});
		mBtnPause = (ImageButton) root.findViewById(R.id.btn_pause);
		mBtnPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onPauseBtnClicked();
			}
		});
		mBtnStop = (ImageButton) root.findViewById(R.id.btn_stop);
		mBtnStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onStopBtnClicked();
			}
		});

		mTvClock = (TextView) root.findViewById(R.id.tv_clock);
		mIvRecordingIndicator = (ImageView) root.findViewById(R.id.iv_redocording_indicator);
		mPbAmplitude = (ProgressBar) root.findViewById(R.id.pb_amplitude);

		return root;
	}

	@Override
	public void onDestroy() {
		unbindService();
		super.onDestroy();
	}

	@Override
	public void onStateChanged(final State state) {
		runOnUiThread(new AsyncTask() {

			@Override
			protected void runAsync() {
				final boolean paused = state == State.PAUSED;
				final boolean started = state == State.STARTED;
				final boolean recording = started || paused;
				final boolean notStopped = recording || state == State.NEW;

				if (mBtnPause != null) {
					mBtnPause.setEnabled(recording);
					mBtnPause.setImageResource(paused ? R.drawable.ic_pause_active : R.drawable.ic_pause_inactive);
				}

				if (mBtnStop != null) {
					mBtnStop.setEnabled(recording);
				}

				if (mBtnRecord != null) {
					mBtnRecord.setEnabled(notStopped);
					mBtnRecord.setImageResource(recording ? R.drawable.ic_microphone_active : R.drawable.ic_microphone_inactive);
				}

				if (mIvRecordingIndicator != null) {
					mIvRecordingIndicator.setVisibility(recording ? View.VISIBLE : View.INVISIBLE);
				}
			}
		});
	}

	@Override
	public void onClockChanged(final String elapsedTime) {
		runOnUiThread(new AsyncTask() {

			@Override
			protected void runAsync() {
				if (mTvClock != null) {
					mTvClock.setText(elapsedTime);
				}
			}
		});
	}

	@Override
	public void onAmplitudeChanged(final int amplitude) {
		runOnUiThread(new AsyncTask() {

			@Override
			protected void runAsync() {
				if (mPbAmplitude != null) {
					if (mPbAmplitude.getMax() < amplitude) {
						mPbAmplitude.setMax(amplitude);
					}
					mPbAmplitude.setProgress(amplitude);
				}
			}
		});
	}

	private void onRecordBtnClicked() {
		if (mRecorderService != null) {
			State state = mRecorderService.getState();
			if (state == State.NEW) {
				startRecording();
			} else if (state == State.STARTED || state == State.PAUSED) {
				stopRecording();
			}
		}
	}

	private void onPauseBtnClicked() {
		pauseRecording();
	}

	private void onStopBtnClicked() {
		stopRecording();
	}

	private void bindService() {
		Intent serviceIntent = new Intent(getActivity(), RecorderService.class);
		getActivity().bindService(serviceIntent, mRecorderServiceConnection, Context.BIND_AUTO_CREATE);
	}

	private void unbindService() {
		try {
			getActivity().unbindService(mRecorderServiceConnection);
		} catch (IllegalArgumentException ex) {
			Logger.error("Error while unbinding recorder service", ex);
		}
	}

	private void startRecording() {
		Intent serviceIntent = new Intent(getActivity(), RecorderService.class);
		serviceIntent.setAction(RecorderService.ACTION_START);
		getActivity().startService(serviceIntent);
	}

	private void pauseRecording() {
		Intent serviceIntent = new Intent(getActivity(), RecorderService.class);
		serviceIntent.setAction(RecorderService.ACTION_PAUSE);
		getActivity().startService(serviceIntent);
	}

	private void stopRecording() {
		Intent serviceIntent = new Intent(getActivity(), RecorderService.class);
		serviceIntent.setAction(RecorderService.ACTION_STOP);
		getActivity().startService(serviceIntent);
	}
}
