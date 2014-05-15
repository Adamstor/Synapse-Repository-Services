package org.sagebionetworks.table.worker;

import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.util.csv.CSVWriterStream;

import com.amazonaws.services.sqs.model.Message;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * This implementation of CSVWriterStream will notify that progress is made for
 * each row written.
 * 
 * @author John
 * 
 */
public class ProgressingCSVWriterStream implements CSVWriterStream {

	private static final String BUILDING_THE_CSV = "Building the CSV...";
	/**
	 * The number of milliseconds between updates.
	 * 
	 */
	public static final long UPDATE_FEQUENCY_MS = 2000;
	CSVWriter writer;
	WorkerProgress progress;
	Message originatingMessage;
	AsynchJobStatusManager asynchJobStatusManager;
	long currentProgress;
	long totalProgress;
	String jobId;
	/**
	 * The time of the last progress update.
	 */
	long lastUpdateTimeMS;

	/**
	 * 
	 * @param writer
	 *            Each row will be passed to this writer.
	 * @param progress
	 *            Progress will be reported to this object.
	 * @param originatingMessage
	 *            The original message that started this job. The visibility
	 *            timeout for this message will get extended as long progress
	 *            continues to be made.
	 */
	public ProgressingCSVWriterStream(CSVWriter writer,
			WorkerProgress progress, Message originatingMessage,
			AsynchJobStatusManager asynchJobStatusManager,
			long currentProgress, long totalProgress, String jobId) {
		super();
		this.writer = writer;
		this.progress = progress;
		this.originatingMessage = originatingMessage;
		this.asynchJobStatusManager = asynchJobStatusManager;
		this.currentProgress = currentProgress;
		this.totalProgress = totalProgress;
		this.jobId = jobId;
		this.lastUpdateTimeMS = System.currentTimeMillis();
	}



	@Override
	public void writeNext(String[] nextLine) {
		// We do not want to spam the listeners, so we only update progress every few seconds.
		if(System.currentTimeMillis() - lastUpdateTimeMS > UPDATE_FEQUENCY_MS){
			// It is time to update the progress
			// notify that progress is still being made for this message
			progress.progressMadeForMessage(originatingMessage);
			// Update the status
			asynchJobStatusManager.updateJobProgress(jobId, currentProgress, totalProgress, BUILDING_THE_CSV);
			// reset the clock
			this.lastUpdateTimeMS = System.currentTimeMillis();
		}

		// Write the line
		writer.writeNext(nextLine);
		// some progress was made
		currentProgress++;
	}

}
