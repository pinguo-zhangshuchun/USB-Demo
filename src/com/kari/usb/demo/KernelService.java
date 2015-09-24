package com.kari.usb.demo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import com.kari.usb.core.ImportTask;
import com.kari.usb.core.IngestObjectInfo;
import com.kari.usb.core.MtpClient;
import com.kari.usb.core.MtpClient.Listener;
import android.app.Service;
import android.content.Intent;
import android.mtp.MtpDevice;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class KernelService extends Service {
	final static String TAG = "KernelService";

	public final static int PERIOD = 5 * 1000;
	private final static int THREAD_TASK_COUNT = 3;

	private MtpClient mMtpClient;
	private MtpDevice mMtpDevice;

	private boolean mStartFlag;
	private boolean mExitFlag;

	private String mOriginPath;
	private String mEffectPath;

	private List<IngestObjectInfo> mCachedList;
	private BlockingQueue<String> mEditQueue;
	private Object mEditLock;
	private ExecutorService mExecutor;

	private ImportListener mClientImportListener;
	private EditListener mClientEditListener;

	public void onCreate() {
		super.onCreate();
		mExitFlag = false;
		mStartFlag = false;
		mMtpClient = new MtpClient(this);
		mMtpClient.addListener(mMtpClientListener);
		mExecutor = Executors.newFixedThreadPool(THREAD_TASK_COUNT);
		mExecutor.execute(mPollTask);
		mExecutor.execute(mEditTask);
	}

	public void startWork(String id) {
		mStartFlag = true;

		// TODO
		mOriginPath = id;
		mEffectPath = id;
	}

	public void stopWork() {
		mStartFlag = false;

		if (null != mCachedList) {
			mCachedList.clear();
		}
	}

	public void setImportListener(ImportListener listener) {
		mClientImportListener = listener;
	}

	public void setEditListener(EditListener listener) {
		mClientEditListener = listener;
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		return 0;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mMtpClient.close();
	}

	public class PollBinder extends Binder {
		public KernelService getService() {
			return KernelService.this;
		}
	}

	private PollBinder mBinder = new PollBinder();

	private MtpClient.Listener mMtpClientListener = new Listener() {
		@Override
		public void deviceAdded(MtpDevice device) {
			if (null == mMtpDevice) {
				Toast.makeText(KernelService.this, "detect 1 device attach",
						Toast.LENGTH_LONG).show();
				mMtpDevice = device;
			} else {
				Toast.makeText(KernelService.this,
						"detect more than 1 device attach", Toast.LENGTH_LONG)
						.show();
			}
		}

		@Override
		public void deviceRemoved(MtpDevice device) {
			if (mMtpDevice == device) {
				Toast.makeText(KernelService.this,
						"current active device removed", Toast.LENGTH_LONG)
						.show();
				mMtpDevice = null;
			} else {
				Toast.makeText(KernelService.this,
						"other connected device removed", Toast.LENGTH_LONG)
						.show();
			}
		}
	}; // mMtpClientListener

	private ImportTask.Listener mImportListener = new ImportTask.Listener() {

		@Override
		public void onImportProgress(int visitedCount, int totalCount,
				String pathIfSuccessful) {
			Toast.makeText(
					KernelService.this,
					"visited:" + visitedCount + " , total:" + totalCount
							+ ",path:" + pathIfSuccessful, Toast.LENGTH_LONG)
					.show();

			if (!mEditQueue.offer(pathIfSuccessful)) {
				Toast.makeText(KernelService.this, "Failed add to EditQueue",
						Toast.LENGTH_LONG).show();
			}

			if (null != mClientImportListener) {
				mClientImportListener.onImportProgress(totalCount,
						visitedCount, pathIfSuccessful);
			}
		}

		@Override
		public void onImportFinish(
				Collection<IngestObjectInfo> objectsNotImported,
				int visitedCount) {
			Toast.makeText(
					KernelService.this,
					"total:" + visitedCount + " , error:"
							+ objectsNotImported.size(), Toast.LENGTH_LONG)
					.show();
			if (null != mClientImportListener) {
				mClientImportListener.onImportFinish(objectsNotImported,
						visitedCount);
			}
		}
	}; // ImportTask.Listener

	private Runnable mPollTask = new Runnable() {
		@Override
		public void run() {
			while (!mExitFlag) {
				if (null != mMtpDevice && mStartFlag) {

					// TODO
					List<IngestObjectInfo> info = null;
					//

					// TODO
					/*
					 * if (null == mCachedList) { if (null != info) {
					 * mCachedList = info; } else { mCachedList = new
					 * ArrayList<IngestObjectInfo>(); } continue; }
					 */
					// TODO

					ImportTask task = new ImportTask(mMtpDevice, info,
							mOriginPath, KernelService.this);
					task.setListener(mImportListener);
					mExecutor.execute(task);
				}

				try {
					Thread.sleep(PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}; // mPollTask

	private Runnable mEditTask = new Runnable() {
		@Override
		public void run() {
			try {
				while (!mExitFlag) {
					String path = mEditQueue.take();
					if (null != path) {
						// TODO
					}
					synchronized (mEditLock) {
						mEditLock.wait();
					}
				} // while
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}; // mEditTask

	public interface ImportListener {
		public void onImportProgress(int total, int progress, String name);

		public void onImportFinish(
				Collection<IngestObjectInfo> objectsNotImported,
				int visitedCount);
	}

	public interface EditListener {
		public void onEditProgess(String path, int left);
	};
}
