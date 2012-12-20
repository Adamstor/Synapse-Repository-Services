package org.sagebionetworks.file.worker;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.sagebionetworks.repo.model.file.ExternalFileMetadata;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.PreviewFileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker process file create messages.
 * When a file is created without a preview, this worker will create on for it.
 * 
 * @author John
 *
 */
public class PreviewWorker implements Callable<List<Message>> {
	
	static private Log log = LogFactory.getLog(PreviewWorker.class);
	
	private PreviewManager previewManager;
	private List<Message> messages;
	
	/**
	 * Instances of this class are created on the fly as needed.  Therefore, all of the
	 * dependencies must be provide in the constructor.
	 * @param fileMetadataDao
	 * @param messages
	 */
	public PreviewWorker(PreviewManager previewManager, List<Message> messages) {
		super();
		this.previewManager = previewManager;
		this.messages = messages;
	}

	@Override
	public List<Message> call() throws Exception {
		// Any message we return will treated as processed and deleted from the queue.
		List<Message> processedMessage = new LinkedList<Message>();
		// Process the messages
		for(Message message: messages){
			try{
				ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
				// Ignore all non-file messages.
				if(ObjectType.FILE == changeMessage.getObjectType()){
					// This is a file message so look up the file
					try{
						FileMetadata metadata = previewManager.getFileMetadata(changeMessage.getObjectId());
						if(metadata instanceof PreviewFileMetadata){
							// We do not make previews of previews
							continue;
						}else if(metadata instanceof S3FileMetadata){
							S3FileMetadata s3fileMeta = (S3FileMetadata) metadata;
							// Generate a preview.
							previewManager.generatePreview(s3fileMeta);
						}else if(metadata instanceof ExternalFileMetadata){
							// we need to add support for this
							throw new UnsupportedOperationException("We need to add support for generating files");
						}else{
							throw new IllegalArgumentException("Unknown file type: "+metadata.getClass().getName());
						}
					}catch(NotFoundException e){
						// we can ignore messages for files that no longer exist.
						continue;
					}
				}
				// This message was processed
				processedMessage.add(message);
			}catch (Throwable e){
				// Failing to process a message should not terminate the rest of the message processing.
				log.error("Failed to process message: "+message.toString(), e);
			}
		}
		return processedMessage;
	}

}
